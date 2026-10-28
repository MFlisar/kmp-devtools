package com.michaelflisar.kmpdevtools.core

import java.io.File

object VersionSynchronizer {

    data class Result(val targetVersion: String, val updatedFiles: List<File>)

    fun synchronizeVersions(root: File, targetVersion: String): List<File> {
        val catalogFile = File(root, "gradle/mflisar.versions.toml")
        val catalogText = catalogFile.readText()
        return synchronizeVersions(root, targetVersion, catalogFile, catalogText, findCatalogVersion(catalogText))
    }

    fun synchronizeVersions(root: File): Result {
        val catalogFile = File(root, "gradle/mflisar.versions.toml")
        val catalogText = catalogFile.readText()
        val version = findCatalogVersion(catalogText)
        val updatedFiles = synchronizeVersions(root, version.value, catalogFile, catalogText, version)
        return Result(version.value, updatedFiles)
    }

    private val versionsSection = Regex(
        """^\[versions\][ \t]*(?:#[^\r\n]*)?\r?\n(.*?)(?=^\[|\z)""",
        setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
    )
    private val catalogVersion = Regex(
        """^([ \t]*kmpdevtools[ \t]*=[ \t]*)(["'])([^"'\r\n]+)\2[ \t]*(?:#[^\r\n]*)?\r?$""",
        RegexOption.MULTILINE
    )
    private val settingsVersion = Regex(
        """^([ \t]*id\([ \t]*"io\.github\.mflisar\.kmpdevtools\.plugins-settings-gradle"[ \t]*\)[ \t]*version[ \t]*")([^"\r\n]+)(")""",
        RegexOption.MULTILINE
    )
    private val workflowVersion = Regex(
        """^([ \t]*(?:-[ \t]+)?uses:[ \t]*["']?MFlisar/kmp-devtools/\.github/workflows/[^@\s"']+@)([^\s"'#]+)""",
        RegexOption.MULTILINE
    )

    private fun findCatalogVersion(text: String): MatchGroup {
        val section = versionsSection.find(text)
            ?: error("Missing [versions] section in gradle/mflisar.versions.toml")
        val entries = catalogVersion.findAll(section.groupValues[1]).toList()
        require(entries.size == 1) {
            "Expected exactly one literal kmpdevtools version in [versions] of gradle/mflisar.versions.toml"
        }
        val value = entries.single().groups[3]!!
        require(value.value.isNotBlank()) { "The kmpdevtools version must not be blank" }
        val offset = section.groups[1]!!.range.first
        return MatchGroup(value.value, (value.range.first + offset)..(value.range.last + offset))
    }

    private fun synchronizeVersions(
        root: File,
        targetVersion: String,
        catalogFile: File,
        catalogText: String,
        catalogVersion: MatchGroup
    ): List<File> {
        require(targetVersion.isNotBlank() && targetVersion.none { it.isWhitespace() || it in "\"'@#\\" }) {
            "Invalid kmp-devtools version: $targetVersion"
        }

        val updates = linkedMapOf<File, String>()
        fun prepare(file: File, transform: (String) -> String) {
            val text = file.readText()
            val newText = transform(text)
            if (text != newText) updates[file] = newText
        }

        if (catalogVersion.value != targetVersion) {
            updates[catalogFile] = catalogText.replaceRange(catalogVersion.range, targetVersion)
        }
        val settingsFile = File(root, "settings.gradle.kts")
        prepare(settingsFile) { text ->
            require(settingsVersion.findAll(text).count() == 1) {
                "Expected exactly one literal kmp-devtools settings plugin version in ${settingsFile.path}"
            }
            settingsVersion.replace(text) { "${it.groupValues[1]}$targetVersion${it.groupValues[3]}" }
        }

        val workflowsDir = File(root, ".github/workflows")
        if (workflowsDir.exists()) {
            val workflows = checkNotNull(workflowsDir.listFiles()) {
                "Could not read workflows directory: ${workflowsDir.path}"
            }
            workflows.filter { it.isFile && it.extension in setOf("yml", "yaml") }.forEach { file ->
                prepare(file) { text ->
                    workflowVersion.replace(text) { "${it.groupValues[1]}$targetVersion" }
                }
            }
        }

        updates.forEach { (file, text) ->
            file.writeText(text)
        }
        return updates.keys.toList()
    }
}