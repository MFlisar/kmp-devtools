package com.michaelflisar.kmpdevtools.core

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VersionSynchronizerTest {

    private fun withProject(block: (File) -> Unit) {
        val root = Files.createTempDirectory("version-synchronizer-test").toFile()
        try {
            File(root, "gradle").mkdirs()
            File(root, "gradle/mflisar.versions.toml").writeText(
                """
                [versions]
                kmpdevtools = "9.0.5" # desired version
                other = "1.0"

                [libraries]
                kmpdevtools = { module = "example:tools", version.ref = "kmpdevtools" }
                """.trimIndent()
            )
            File(root, "settings.gradle.kts").writeText(
                """
                plugins {
                    // id("io.github.mflisar.kmpdevtools.plugins-settings-gradle") version "example"
                    id("io.github.mflisar.kmpdevtools.plugins-settings-gradle") version "9.0.4"
                }
                """.trimIndent()
            )
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun readsOnlyVersionSectionAndSupportsLiteralStringsAndCrLf() = withProject { root ->
        val catalog = File(root, "gradle/mflisar.versions.toml")
        catalog.writeText(catalog.readText().replace("\"9.0.5\"", "'9.0.5'").replace("\n", "\r\n"))
        assertEquals("9.0.5", VersionSynchronizer.synchronizeVersions(root).targetVersion)
    }

    @Test
    fun synchronizesFilesAndIsIdempotent() = withProject { root ->
        val workflows = File(root, ".github/workflows").apply { mkdirs() }
        val workflowText = """
            jobs:
              publish:
                uses: "MFlisar/kmp-devtools/.github/workflows/reuseable-publish.yml@9.0.4" # keep comment
              docs:
                uses: MFlisar/kmp-devtools/.github/workflows/reuseable-update-markdown-docs.yml@main
              unrelated:
                uses: example/other/.github/workflows/build.yml@v1
            # uses: MFlisar/kmp-devtools/.github/workflows/example.yml@keep
        """.trimIndent()
        File(workflows, "publish.yml").writeText(workflowText)
        File(workflows, "publish.yaml").writeText(workflowText)

        val catalog = File(root, "gradle/mflisar.versions.toml")
        val originalCatalog = catalog.readText()
        val (targetVersion, changed) = VersionSynchronizer.synchronizeVersions(root)
        assertEquals("9.0.5", targetVersion)
        assertEquals(originalCatalog, catalog.readText())
        assertEquals(
            setOf("settings.gradle.kts", ".github/workflows/publish.yml", ".github/workflows/publish.yaml"),
            changed.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        )
        assertTrue(File(root, "settings.gradle.kts").readText().contains("version \"9.0.5\""))
        val expected = workflowText.replace("@9.0.4", "@9.0.5").replace("@main", "@9.0.5")
        assertEquals(expected, File(workflows, "publish.yml").readText())
        assertEquals(expected, File(workflows, "publish.yaml").readText())
        assertTrue(VersionSynchronizer.synchronizeVersions(root).updatedFiles.isEmpty())
    }

    @Test
    fun manualUpdateAlsoChangesCatalogAndPreservesFormatting() = withProject { root ->
        val catalog = File(root, "gradle/mflisar.versions.toml")
        val original = catalog.readText()
        val changed = VersionSynchronizer.synchronizeVersions(root, "10.0.0")
        assertEquals(setOf(catalog, File(root, "settings.gradle.kts")), changed.toSet())
        assertEquals(original.replace("\"9.0.5\"", "\"10.0.0\""), catalog.readText())
    }

    @Test
    fun invalidSettingsDoNotPartiallyUpdateCatalog() = withProject { root ->
        val catalog = File(root, "gradle/mflisar.versions.toml")
        val original = catalog.readText()
        File(root, "settings.gradle.kts").writeText("plugins {}")
        assertFailsWith<IllegalArgumentException> {
            VersionSynchronizer.synchronizeVersions(root, "10.0.0")
        }
        assertEquals(original, catalog.readText())
    }

    @Test
    fun rejectsMissingOrDuplicateCatalogVersion() = withProject { root ->
        val catalog = File(root, "gradle/mflisar.versions.toml")
        catalog.writeText("[versions]\nother = \"1\"\n[libraries]\nkmpdevtools = \"not-a-version\"")
        assertFailsWith<IllegalArgumentException> { VersionSynchronizer.synchronizeVersions(root) }
        catalog.writeText("[versions]\nkmpdevtools = \"1\"\nkmpdevtools = \"2\"\n")
        assertFailsWith<IllegalArgumentException> { VersionSynchronizer.synchronizeVersions(root) }
    }

    @Test
    fun rejectsInvalidTargetBeforeWritingFiles() = withProject { root ->
        val settings = File(root, "settings.gradle.kts")
        val original = settings.readText()
        assertFailsWith<IllegalArgumentException> {
            VersionSynchronizer.synchronizeVersions(root, "9.0.5\"")
        }
        assertEquals(original, settings.readText())
    }
}
