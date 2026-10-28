package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.VersionSynchronizer
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging

class SettingsFilePlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val root = settings.settingsDir
        val (targetVersion, updatedFiles) = try {
            VersionSynchronizer.synchronizeVersions(root)
        } catch (e: Exception) {
            throw GradleException(
                "Could not synchronize kmp-devtools versions. Check gradle/mflisar.versions.toml, " +
                    "settings.gradle.kts and GitHub workflows. Some files may already have been updated.",
                e
            )
        }

        if (updatedFiles.isEmpty())
            return

        val feedback = buildString {
            appendLine("kmp-devtools: Version synchronization completed.")
            appendLine()
            appendLine("The version entries were inconsistent and have been updated to $targetVersion.")
            appendLine("Source: gradle/mflisar.versions.toml [versions].kmpdevtools")
            appendLine()
            appendLine("Updated files:")
            updatedFiles.forEach { appendLine("  - ${it.relativeTo(root).path}") }
            appendLine()
            appendLine("Please start a new Gradle sync or rerun the build.")
        }
        Logging.getLogger(SettingsFilePlugin::class.java).lifecycle(feedback)
        throw GradleException(
            "The kmp-devtools version was updated to $targetVersion, so the settings plugin stopped this run. " +
                "Please start a new Gradle sync or rerun the build."
        )
    }
}