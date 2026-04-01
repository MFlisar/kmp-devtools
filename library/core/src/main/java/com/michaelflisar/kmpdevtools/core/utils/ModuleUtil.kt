package com.michaelflisar.kmpdevtools.core.utils

object ModuleUtil {

    fun folderToModuleName(path: String, libraryName: String, libraryFolder: String = "library"): String {
        return if (path == libraryFolder) {
            ":$libraryName"
        } else if (path.startsWith(libraryFolder)){
            ":$libraryName:" + path.replace("$libraryFolder/", "").replace("/", ":")
        } else {
            ":" + path.replace("/", ":")
        }
    }
}