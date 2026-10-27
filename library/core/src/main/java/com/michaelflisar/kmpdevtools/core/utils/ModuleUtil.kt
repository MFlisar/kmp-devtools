package com.michaelflisar.kmpdevtools.core.utils

object ModuleUtil {

    fun folderToModuleName(invariantPath: String, libraryName: String, libraryFolder: String = "library"): String {
        return if (invariantPath == libraryFolder) {
            ":$libraryName"
        } else if (invariantPath.startsWith(libraryFolder)){
            ":$libraryName:" + invariantPath.replace("$libraryFolder/", "").replace("/", ":")
        } else {
            ":" + invariantPath.replace("/", ":")
        }
    }
}