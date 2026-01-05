package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.classes.RelativePath
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
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
        libraryConfig: LibraryConfig = LibraryConfig.read(project),
        macTargetRootDir: String = "/Users/mflisar/dev",
    ) = RelativePath.Root(
        local = project.rootDir.absolutePath,
        remote = "$macTargetRootDir/${libraryConfig.library.name}"
    )

    fun getRelativePathRoot(
        rootDirectory: File,
        libraryConfig: LibraryConfig,
        macTargetRootDir: String = "/Users/mflisar/dev",
    ) = RelativePath.Root(
        local = rootDirectory.absolutePath,
        remote = "$macTargetRootDir/${libraryConfig.library.name}"
    )
}