package com.michaelflisar.kmpdevtools.tasks

import com.michaelflisar.kmpdevtools.TaskUtil
import com.michaelflisar.kmpdevtools.core.VersionSynchronizer
import com.michaelflisar.kmpdevtools.core.utils.GithubUtil
import java.io.File

class UpdateDevToolsTask(
    override val shortcut: String,
) : ITask {

    override val name = "updateDevTools"
    override val description = "Updates the devTools Version"

    override fun run(root: File) {

        // user input holen
        val lastRelease =
            GithubUtil.getLastRelease("MFlisar/kmp-devtools", GithubUtil.AccessMode.Auto)
        val newVersion = TaskUtil.readUserInput(
            "Neue kmp-devtools version (default: $lastRelease):",
            forceNotEmpty = false
        )
            .trim()
            .takeIf { it.isNotEmpty() } ?: lastRelease?.trim()

        if (newVersion.isNullOrEmpty()) {
            println("Aborted: Could not determine latest version and no input provided!")
            return
        }

        val updatedFiles = VersionSynchronizer.synchronizeVersions(root, newVersion)
        updatedFiles.forEach { println("Updated: ${it.relativeTo(root).path}") }

        println()
        println("ATTENTION:")
        println("You may want to update downloaded files as well!")

    }
}