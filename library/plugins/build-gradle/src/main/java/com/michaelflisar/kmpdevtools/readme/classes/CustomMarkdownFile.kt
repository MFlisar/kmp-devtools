package com.michaelflisar.kmpdevtools.readme.classes

import java.io.File

class CustomMarkdownFile(
    val file: File,
    val name: String,
) {
    fun startsWithIgnoreCase(file: File): Boolean {
        return this.file.invariantSeparatorsPath.lowercase()
            .startsWith(file.invariantSeparatorsPath.lowercase())
    }

    fun relativePathTo(root: File) = file.relativeTo(root).invariantSeparatorsPath

    val extractedTitle by lazy {
        val text = file.readText()
        val regex = Regex("""^#\s*(.+)$""", RegexOption.MULTILINE)
        val matchResult = regex.find(text)
        matchResult?.groupValues?.get(1)?.trim() ?: name
    }
}