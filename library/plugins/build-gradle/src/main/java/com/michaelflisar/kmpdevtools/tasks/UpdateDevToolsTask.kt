package com.michaelflisar.kmpdevtools.tasks

import com.michaelflisar.kmpdevtools.TaskUtil
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

        fun replaceInFile(file: File, regex: Regex, replacement: String) {
            val text = file.readText()
            val newText = text.replace(regex, replacement)
            if (text != newText) {
                file.writeText(newText)
                println("Updated: ${file.path}")
            }
        }

        // 1) gradle/mflisar.versions.toml => kmpdevtools = "6.4.1" ersetzen
        val depsFile = File(root, "gradle/mflisar.versions.toml")
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
            val regex = Regex("""uses:\s*MFlisar/kmp-devtools/\.github/workflows/([^@]+)@(\S+)""")
            val newText = regex.replace(text) { matchResult ->
                val fileName = matchResult.groupValues[1]
                "uses: MFlisar/kmp-devtools/.github/workflows/$fileName@$newVersion"
            }
            if (text != newText) {
                ymlFile.writeText(newText)
                println("Updated: ${ymlFile.path}")
            }
        }

        println()
        println("ATTENTION:")
        println("You may want to update downloaded files as well!")

    }
}