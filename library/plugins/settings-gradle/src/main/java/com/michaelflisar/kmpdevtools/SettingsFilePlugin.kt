package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.ModuleUtil
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

class SettingsFilePlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings
    }

    fun checkGradleProperty(property: String): Boolean? {
        if (!settings.providers.gradleProperty(property).isPresent) {
            return null
        }
        return settings.providers.gradleProperty(property).get().toBoolean()
    }

    fun includeModules(libraryId: String, libraryConfig: LibraryConfig, includeDokka: Boolean, libraryFolder: String = "library") {
        val allPaths = libraryConfig.modules.map { it.path }.distinct()
        val foldersInPaths = allPaths.map { it.substringBeforeLast("/", "") }.distinct()

        // include all folders first
        foldersInPaths.forEach {
            val name = ModuleUtil.folderToModuleName(it, libraryId, libraryFolder)
            settings.include(name)
            settings.project(name).projectDir = File(settings.rootDir, it)
        }

        // then include all modules
        println()
        println("Including modules - libraryId = '$libraryId'")
        libraryConfig.modules.forEach {
            val name = ModuleUtil.folderToModuleName(it.path, libraryId, libraryFolder)
            includeModule(it.path, name)
            println("- $name => ${it.path}")
        }
        if (includeDokka) {
            includeModule("library/dokka", ":dokka")
        }
        println()
    }

    /**
     * include a module like following:
     *
     * includeModule(":toolbox:core") => "$folder\\library\\core"
     */
    fun includeModule(fullPath: String, name: String) {
        settings.include(name)
        settings.project(name).projectDir = File(fullPath)
    }

    /**
     * include a module like following:
     *
     * val libsMine = "..\\..\\11 - libs (mine)"
     * val toolbox  = "$libsMine\\Toolbox"
     * val toolboxName  = "toolbox"
     *
     * includeLibrary(toolbox, toolboxName, ":toolbox:core") => "$folder\\library\\core"
     * includeLibrary(toolbox, toolboxName, ":toolbox:modules:ui") => "$folder\\library\\modules\\ui"
     * ...
     *
     * or for root based modules:
     * includeLibrary(toolbox, toolboxName, "toolbox:demo", true) => "$folder\\demo"
     * ...
     *
     * @param libraryFolder the root folder of the library
     * @param name the module name
     * @param isInRoot if true, the module is in the root of the toolbox folder, otherwise in the library folder
     */
    fun includeLibrary(libraryFolder: String, libraryName: String, name: String, isInRoot: Boolean = false) {
        val relativePath =  name.replaceFirst(libraryName, if (isInRoot) "" else "library", true)
            .replace("::", ":")
            .replace(":", "\\").removePrefix("\\")
        //println("relativePath: $relativePath")
        includeModule("$libraryFolder\\$relativePath", name)
    }

    /**
     * include a module like following:
     *
     * val libsMine = "..\\..\\11 - libs (mine)"
     *
     * includeToolbox(libsMine, ":toolbox:core") => "$folder\\library\\core"
     * includeToolbox(libsMine, ":toolbox:modules:ui") => "$folder\\library\\modules\\ui"
     * ...
     *
     * or for root based modules:
     * includeToolbox(libsMine, "toolbox:demo", true) => "$folder\\demo"
     * ...
     *
     * @param myLibsFolder the "11 - libs (mine)" path
     * @param name the module name
     * @param isInRoot if true, the module is in the root of the toolbox folder, otherwise in the library folder
     */
    fun includeToolbox(myLibsFolder: String, name: String, isInRoot: Boolean = false) {
        val libraryFolder = "$myLibsFolder\\Toolbox"
        val libraryName = "toolbox"
        includeLibrary(libraryFolder, libraryName, name, isInRoot)
    }

}