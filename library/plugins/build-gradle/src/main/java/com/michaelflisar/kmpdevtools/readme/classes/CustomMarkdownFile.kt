package com.michaelflisar.kmpdevtools.readme.classes

import java.io.File
import kotlin.io.invariantSeparatorsPath

class CustomMarkdownFile(
    val file: File,
    val name: String
) {
    fun startsWithIgnoreCase(file: File) : Boolean {
        return this.file.invariantSeparatorsPath.lowercase().startsWith(file.invariantSeparatorsPath.lowercase())
    }

    fun relativePathTo(root: File) = file.relativeTo(root).invariantSeparatorsPath
}