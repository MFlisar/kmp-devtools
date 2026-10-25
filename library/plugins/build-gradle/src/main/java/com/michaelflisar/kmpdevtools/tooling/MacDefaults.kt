package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.classes.RelativePath
import com.michaelflisar.kmpdevtools.core.Config
import com.michaelflisar.kmpdevtools.core.utils.SSHSetup
import org.gradle.api.Project
import java.io.File

object MacDefaults {

    fun getMacSSHSetup(): SSHSetup {
        return SSHSetup(
            host = "macmini.local",
            user = "mflisar",
            port = 22,
            keyPath = System.getProperty("user.home") + "/.ssh/id_ed25519",
        )
    }

    fun getRelativePathRoot(
        project: Project,
        config: Config = Config.read(project.projectDir),
        macTargetRootDir: String = "/Users/mflisar/dev",
    ) = RelativePath.Root(
        local = project.rootDir.absolutePath,
        remote = "$macTargetRootDir/${config.project.repoName}"
    )

    fun getRelativePathRoot(
        rootDirectory: File,
        config: Config,
        macTargetRootDir: String = "/Users/mflisar/dev",
    ) = RelativePath.Root(
        local = rootDirectory.absolutePath,
        remote = "$macTargetRootDir/${config.project.repoName}"
    )
}