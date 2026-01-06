package com.michaelflisar.kmpdevtools.core.utils

object ModuleUtil {

    fun folderToModuleName(path: String, libraryId: String, libraryFolder: String = "library"): String {
        return if (path == libraryFolder) {
            ":$libraryId"
        } else if (path.startsWith(libraryFolder)){
            ":$libraryId:" + path.replace("$libraryFolder/", "").replace("/", ":")
        } else {
            ":" + path.replace("/", ":")
        }
    }
}