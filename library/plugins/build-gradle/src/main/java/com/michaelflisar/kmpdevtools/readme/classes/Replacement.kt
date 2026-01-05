package com.michaelflisar.kmpdevtools.readme.classes

import java.io.File

sealed class Replacement {
    abstract fun replace(content: String): String

    companion object {
        fun replaceContent(
            content: String,
            placeholder: String,
            replacement: String,
            trimEmptyLines: Boolean = true,
        ): String {
            val replacementContent =
                if (trimEmptyLines) replacement.replace(Regex("^\\s+|\\s+$"), "") else replacement
            return content.replace(placeholder, replacementContent)
        }
    }
}

class Placeholder(
    private val placeholder: String,
    private val replacement: String,
) : Replacement() {
    override fun replace(content: String): String {
        return replaceContent(content, placeholder, replacement)
    }
}

class Partial(
    private val placeholder: String,
    private val partialFile: File,
) : Replacement() {
    override fun replace(content: String): String {
        val replacement = partialFile.readText()
        return replaceContent(content, placeholder, replacement)
    }
}