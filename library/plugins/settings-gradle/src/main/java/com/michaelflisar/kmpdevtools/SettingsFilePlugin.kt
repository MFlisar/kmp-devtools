package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.VersionSynchronizer
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsFilePlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val root = settings.settingsDir
        val feedback = try {
            val (targetVersion, updatedFiles) = VersionSynchronizer.synchronizeVersions(root)
            if (updatedFiles.isEmpty())
                return

            buildString {
                appendLine("kmp-devtools: Version synchronization completed.")
                appendLine()
                appendLine("The version entries were inconsistent and have been updated to $targetVersion.")
                appendLine("Source: gradle/mflisar.versions.toml [versions].kmpdevtools")
                appendLine()
                appendLine("Updated files:")
                updatedFiles.forEach { appendLine("  - ${it.relativeTo(root).path}") }
                appendLine()
                appendLine("Please start a new Gradle sync or rerun the build.")
                append("This run was stopped because the settings plugin was already loaded before the update.")
            }
        } catch (e: Exception) {
            throw GradleException(
                "Could not synchronize kmp-devtools versions. Check gradle/mflisar.versions.toml, " +
                    "settings.gradle.kts and GitHub workflows. Some files may already have been updated.",
                e
            )
        }
        throw GradleException(feedback)
    }
}