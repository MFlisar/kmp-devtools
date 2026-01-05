package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.classes.RelativePath
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.CMDUtil
import com.michaelflisar.kmpdevtools.core.utils.SSHSetup
import com.michaelflisar.kmpdevtools.core.utils.SSHUtil
import com.michaelflisar.kmpdevtools.core.utils.ScriptStep
import com.michaelflisar.kmpdevtools.core.utils.ScriptUtil
import java.io.File

object MacActions {

    fun run(
        projectRootDirectory: File,
        sshSetup: SSHSetup,
        toolingSetup: ToolingSetup,
        libraryConfig: LibraryConfig,
        defaultInput: String = "1,2",
    ) {

        // -------------------
        // Script Steps + Run
        // -------------------

        val xcFrameworks = libraryConfig.xcframeworks.map {
            val name = it.name
            val path = it.path
            XCFrameworkSetup(
                name = name,
                scheme = name,
                targets = it.targets,
                xcodeproj = RelativePath("$path/$name.xcodeproj"),
                xcframework = RelativePath("$path/$name.xcframework")
            )
        }

        val steps = listOf(
            ScriptStep("Sync to Mac") {
                syncToMac(
                    projectRootDirectory = projectRootDirectory,
                    libraryConfig = libraryConfig,
                    sshSetup = sshSetup,
                    toolingSetup = toolingSetup
                )
            },
            ScriptStep("Build XCFramework on Mac (and copy it back)") {
                buildXCFramework(
                    sshSetup = sshSetup,
                    toolingSetup = toolingSetup,
                    xcFrameworks = xcFrameworks
                )
            },
            ScriptStep("Open XCode: App") {
                openXCode(
                    remoteXCodeProject = toolingSetup.xcodeProjectFolder?.getRemotePath(toolingSetup.root),
                    sshSetup = sshSetup
                )
            },
            ScriptStep("Open XCode: XCFramework") {
                val index = ScriptUtil.printOptions(
                    prompt = "Select XCFramework to open:",
                    options = xcFrameworks.map { it.name }
                )
                if (index != null) {
                    openXCode(
                        remoteXCodeProject = xcFrameworks[index].xcodeproj.getRemotePath(
                            toolingSetup.root
                        ),
                        sshSetup = sshSetup
                    )
                }
            }
        )

        ScriptUtil.runScriptSteps(
            name = "Mac Actions",
            steps = steps,
            defaultInput = defaultInput
        )
    }
}

private fun syncToMac(
    projectRootDirectory: File,
    libraryConfig: LibraryConfig,
    sshSetup: SSHSetup,
    toolingSetup: ToolingSetup,
) {

    println("Syncing to Mac...")

    val exclude = listOf(
        "build",
        ".gradle",
        ".idea",
        "*.iml",
        ".kotlin",
        ".run"
    )

    val executables = toolingSetup.getExecutables()

    val projectName = libraryConfig.library.name
    val projectRootDirectory = projectRootDirectory.absolutePath
    val projectRemoteRootDirectory = toolingSetup.root.remote

    // ----------------------
    // Script
    // ----------------------

    // Build tar exclude args
    val tarExArgs = exclude.flatMap { listOf("--exclude=$it") }

    println("\n--------------------------------")
    println("- Project:  $projectName")
    println("- Mac User: ${sshSetup.user}")
    println("- Mac Host: ${sshSetup.host}")
    println("- Source:   $projectRootDirectory")
    println("- Target:   $projectRemoteRootDirectory")
    println("--------------------------------\n")

    // 1) Prepare remote dir (delete if exists, then create)
    println("1) Prepare Mac directory")
    val checkDirCmd =
        "[ -d '$projectRemoteRootDirectory' ] && rm -rf '$projectRemoteRootDirectory'"
    SSHUtil.ssh(
        command = "$checkDirCmd && mkdir -p '$projectRemoteRootDirectory' || mkdir -p '$projectRemoteRootDirectory'",
        sshSetup = sshSetup
    )
    SSHUtil.ssh(
        command = "mkdir -p '$projectRemoteRootDirectory'",
        sshSetup = sshSetup
    )

    // 2) Create tar locally
    println("2) Create tar archive")
    val tmpTar = File.createTempFile("sync_", ".tar")
    val tarCmd = listOf(
        "tar",
        "-cf",
        tmpTar.absolutePath,
        "-C",
        projectRootDirectory
    ) + tarExArgs + "."
    CMDUtil.runOrThrow(tarCmd, "tar create failed.")

    // 3) Copy tar to Mac
    println("3) Copy tar to Mac")
    if (!tmpTar.exists())
        error("TAR archive was not created: ${tmpTar.absolutePath}")
    val remoteTar = "$projectRemoteRootDirectory/__sync.tar"
    val scpCmd = mutableListOf("scp", "-P", sshSetup.port.toString())
    if (File(sshSetup.keyPath).exists())
        scpCmd += listOf("-i", sshSetup.keyPath)
    scpCmd += listOf(tmpTar.absolutePath, "${sshSetup.user}@${sshSetup.host}:$remoteTar")
    CMDUtil.runOrThrow(scpCmd, "scp upload failed.")

    // 4) Extract tar on Mac and remove tar
    println("4) Extract tar on Mac")
    SSHUtil.ssh(
        command = "tar -xpf '$remoteTar' -C '$projectRemoteRootDirectory' && rm -f '$remoteTar'",
        sshSetup = sshSetup
    )

    // 5) Clean up local tar
    println("5) Delete local tar")
    tmpTar.delete()

    // 6) Close XCode on Mac
    println("6) Close XCode on Mac")
    SSHUtil.ssh(
        command = "killall Xcode >/dev/null 2>&1",
        sshSetup = sshSetup,
        ignoreExitCode = { exitCode, output ->
            exitCode == 1 && (output.isBlank() || output.contains("no matching processes", true))
        }
    )

    // 7) Set executable files
    println("7) Set executable files on Mac")
    for (file in executables) {
        val remotePath = file.getRemotePath(toolingSetup.root)
        SSHUtil.ssh("chmod +x '$remotePath'", sshSetup)
    }

    println("8) Done.")

}

private fun buildXCFramework(
    sshSetup: SSHSetup,
    toolingSetup: ToolingSetup,
    xcFrameworks: List<XCFrameworkSetup>,
) {

    println("Building XCFramework on Mac...")

    val projectRemoteRootDirectory = toolingSetup.root.remote

    if (xcFrameworks.isEmpty()) {
        println("This library does not have any XCFrameworks! Skipping task.")
        return
    }

    for (xcFramework in xcFrameworks) {

        println("Building XCFramework: ${xcFramework.name}")

        val localXCFrameworkFile = xcFramework.xcframework.getLocalFile(toolingSetup.root)
        val remoteXCFrameworkPath = xcFramework.xcframework.getRemotePath(toolingSetup.root)
        val relativeXCodeProjPath = xcFramework.xcodeproj.path

        val projectName = xcFramework.name
        val scheme = xcFramework.scheme
        val includeMacFlag = if (xcFramework.targets.contains("macos")) 1 else 0

        // im root ausführen, dann passt auch project path relative zum root
        val env = "PROJECT_NAME=\"$projectName\" " +
                "SCHEME=\"$scheme\" " +
                "PROJECT_PATH=\"${relativeXCodeProjPath}\" " +
                "CONFIGURATION=\"Release\" " +
                "INCLUDE_MAC=$includeMacFlag " +
                "INCLUDE_CATALYST=0"

        // 1) get script file content from resources/build_xcframework.sh as stream
        val scriptFileName = "build_xcframework.sh"
        val resourceStream = MacActions::class.java.classLoader.getResourceAsStream(scriptFileName)
            ?: error("Resource not found: $scriptFileName")

        // 2) save script file stream to a local temp file
        val tmpScriptFile = File.createTempFile("build_xcframework", ".sh")
        tmpScriptFile.writeText(resourceStream.bufferedReader().readText())

        // 3) copy script file to remote machine (Mac)
        val remoteScriptFile = "$projectRemoteRootDirectory/$scriptFileName"
        val scpScriptCmd = mutableListOf("scp", "-P", sshSetup.port.toString())
        if (File(sshSetup.keyPath).exists())
            scpScriptCmd += listOf("-i", sshSetup.keyPath)
        scpScriptCmd += listOf(
            tmpScriptFile.absolutePath,
            "${sshSetup.user}@${sshSetup.host}:$remoteScriptFile"
        )
        CMDUtil.runOrThrow(scpScriptCmd, "scp upload of script failed.")
        tmpScriptFile.delete()

        // 4) run script
        val command1 =
            "cd '$projectRemoteRootDirectory' && chmod +x '$scriptFileName' && $env './$scriptFileName'"
        SSHUtil.ssh(command1, sshSetup)

        // 5) xcframework zurück kopieren
        println("Copying XCFramework back to local machine...")
        if (localXCFrameworkFile.exists()) {
            localXCFrameworkFile.deleteRecursively()
        }
        val scpCommand = listOf(
            "scp",
            "-r",
            "${sshSetup.user}@${sshSetup.host}:$remoteXCFrameworkPath",
            localXCFrameworkFile.parentFile.absolutePath
        )
        val process = ProcessBuilder(scpCommand)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("scp failed with exit code $exitCode. Output:\n$output")
        }
        if (!localXCFrameworkFile.exists()) {
            throw RuntimeException("Kopieren fehlgeschlagen: ${localXCFrameworkFile.absolutePath} existiert nicht!")
        }
    }
}

private fun openXCode(
    remoteXCodeProject: String?,
    sshSetup: SSHSetup,
) {
    println("Opening XCode...")

    if (remoteXCodeProject == null) {
        println("This library does not have a XCode App! Skipping task.")
        return
    }

    SSHUtil.ssh(
        command = "open -a Xcode '$remoteXCodeProject' >/dev/null 2>&1 &",
        sshSetup = sshSetup
    )
}

private class XCFrameworkSetup(
    val name: String,
    val scheme: String,
    val targets: List<String>,
    val xcodeproj: RelativePath,
    val xcframework: RelativePath,
)