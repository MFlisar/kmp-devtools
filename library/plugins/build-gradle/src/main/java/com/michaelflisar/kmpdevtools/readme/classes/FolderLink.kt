package com.michaelflisar.kmpdevtools.readme.classes

data class FolderLink(
    val name: String,
    val link: String? = null,
    val children: List<FolderLink> = emptyList()
)