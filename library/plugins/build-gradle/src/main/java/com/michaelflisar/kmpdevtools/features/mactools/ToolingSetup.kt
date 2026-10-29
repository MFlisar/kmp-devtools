package com.michaelflisar.kmpdevtools.features.mactools

import com.michaelflisar.kmpdevtools.core.classes.RelativePath

class ToolingSetup(
    val root: RelativePath.Root,
    val gradlewFile: RelativePath = RelativePath("gradlew"),
    val xcodeProjectFolder: RelativePath? = RelativePath("demo/iosApp/iosApp.xcodeproj").takeIf {
        it.getLocalFile(root).exists()
    },
) {
    fun getExecutables(): List<RelativePath> {
        return listOf(gradlewFile)
    }
}