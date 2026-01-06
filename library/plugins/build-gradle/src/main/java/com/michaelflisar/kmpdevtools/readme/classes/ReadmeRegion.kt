package com.michaelflisar.kmpdevtools.readme.classes

class ReadmeRegion(
    val image: String?,
    val text: String,
) {
    fun header() = if (image != null) ":$image: $text" else text
    fun markdownHeader() = "# ${header()}"
    fun markdownLink() = header()
        .lowercase()
        .replace(":", "")
        .replace(" ", "-")
        .let { "[${text}](#${it})" }
}