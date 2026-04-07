package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.ProjectData
import com.michaelflisar.kmpdevtools.core.utils.ProjectRenamer
import com.michaelflisar.kmpdevtools.core.utils.ScriptStep
import com.michaelflisar.kmpdevtools.core.utils.ScriptUtil
import java.io.File
import kotlin.String

object ProjectActions {

    fun runProjectRenamer() {

        val rootDir = File(System.getProperty("user.dir"))
        val config = Config.readFromProject(rootDir)
        val libraryConfig = LibraryConfig.readFromProject(rootDir)

        val currentPackageName = config.project.namespace
        val currentLibraryName = libraryConfig.library.name

        val packageNameTo = readUserInput("New package name / namespace (current: $currentPackageName): ")
        val libraryNameTo = readUserInput("New library name (current: $currentLibraryName): ")

        val data = ProjectData(
            packageNameFrom = currentPackageName,
            libraryNameFrom = currentLibraryName,
            packageNameTo = packageNameTo,
            libraryNameTo = libraryNameTo
        )

        val steps = listOf(
            ScriptStep("Rename Package Names") {

                // 1) rename folders
                ProjectRenamer.renameFolder(data = data)

                // 2) rename project content
                ProjectRenamer.renameProject(
                    data = data,
                    renameImports = true,
                    renamePackageNames = true,
                    renameModuleReferences = false,
                )

                // 3) update run configurations
                ProjectRenamer.updateRunConfigurations(data = data)

            },
            ScriptStep("Update iOS App") {

                val folderIOSApp = File(rootDir, "demo/iosApp")
                if (folderIOSApp.exists()) {

                    val fileConfig = File(folderIOSApp, "Configuration/Config.xcconfig")
                    val fileProject = File(folderIOSApp, "iosApp.xcodeproj/project.pbxproj")

                    data.updateFile(fileConfig, replacePackageName = true)
                    data.updateFile(fileProject, replacePackageName = true)

                }
            }
        )

        ScriptUtil.runScript(
            name = "Rename Package Name",
            steps = steps,
            scriptInfos = {
                ScriptUtil.printDetails(data.asStringMap())
            }
        )
    }

    fun updateDevToolsVersion(
        root: File,
    ) {
        // user input holen
        val newVersion = readUserInput("Neue kmp-devtools version: ")

        fun replaceInFile(file: File, regex: Regex, replacement: String) {
            val text = file.readText()
            val newText = text.replace(regex, replacement)
            if (text != newText) {
                file.writeText(newText)
                println("Updated: ${file.path}")
            }
        }

        // 1) gradle/deps.versions.toml => kmpdevtools = "6.4.1" ersetzen
        val depsFile = File(root, "gradle/deps.versions.toml")
        val depsText = depsFile.readText()
        val depsRegex = Regex("""(kmpdevtools\s*=\s*)".*?"""")
        val depsNewText = depsRegex.replace(depsText) { matchResult ->
            val prefix = matchResult.groupValues[1]
            "${prefix}\"$newVersion\""
        }
        if (depsText != depsNewText) {
            depsFile.writeText(depsNewText)
            println("Updated: ${depsFile.path}")
        }

        // 2) settings.gradle.kts => id("io.github.mflisar.kmpdevtools.plugins-settings-gradle") version "6.4.1" ersetzen
        val settingsFile = File(root, "settings.gradle.kts")
        replaceInFile(
            file = settingsFile,
            regex = Regex("""id\("io\.github\.mflisar\.kmpdevtools\.plugins-settings-gradle"\)\s*version\s*".*?""""),
            replacement = """id("io.github.mflisar.kmpdevtools.plugins-settings-gradle") version "$newVersion""""
        )

        // 3) in .github/workflows/*.yml uses: MFlisar/kmp-devtools/.github/workflows/kmp-devtools-*.yml@6.4.1 ersetzen
        val workflowsDir = File(root, ".github/workflows")
        workflowsDir.listFiles { f -> f.extension == "yml" }?.forEach { ymlFile ->
            val text = ymlFile.readText()
            // regex: nach dem @ kann auch ein text stehen
            val regex = Regex("""uses:\s*MFlisar/kmp-devtools/.github/workflows/(kmp-devtools-[\w\-]+\.yml)@([^\s]+)""")
            val newText = regex.replace(text) { matchResult ->
                val fileName = matchResult.groupValues[1]
                "uses: MFlisar/kmp-devtools/.github/workflows/$fileName@$newVersion"
            }
            if (text != newText) {
                ymlFile.writeText(newText)
                println("Updated: ${ymlFile.path}")
            }
        }
    }

    fun readUserInput(prompt: String, forceNotEmpty: Boolean = true): String {
        println(prompt)
        val input = readlnOrNull()?.trim().orEmpty()
        if (input.isEmpty() && forceNotEmpty) {
            println("Aborted: Input empty!")
            throw RuntimeException("Input empty")
        }
        return input
    }
}