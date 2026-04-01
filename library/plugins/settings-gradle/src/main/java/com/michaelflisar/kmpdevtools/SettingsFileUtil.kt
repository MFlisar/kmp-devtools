package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.ModuleUtil
import org.gradle.api.initialization.Settings
import java.io.File

object SettingsFileUtil {

    fun includeModules(
        settings: Settings,
        libraryName: String,
        libraryConfig: LibraryConfig,
        libraryFolder: String = "library",
    ) {
        val allPaths = libraryConfig.modules.map { it.path }.distinct()
        val foldersInPaths = allPaths.map { it.substringBeforeLast("/", "") }.distinct()

        // include all folders first
        foldersInPaths.forEach {
            val name = ModuleUtil.folderToModuleName(it, libraryName, libraryFolder)
            settings.include(name)
            settings.project(name).projectDir = File(settings.rootDir, it)
        }

        // then include all modules
        println()
        println("Including modules - libraryName = '$libraryName'")
        libraryConfig.modules.forEach {
            val name = ModuleUtil.folderToModuleName(it.path, libraryName, libraryFolder)
            includeModule(settings, it.path, name)
            println("- $name => ${it.path}")
        }
    }

    fun includeModulesInFolder(
        settings: Settings,
        folder: String,
        exclude: (buildGradleFile: File) -> Boolean = { false },
    ) {
        val folder = File(settings.rootDir, folder)
        val buildGradleFiles = folder.walkTopDown()
            .filter { it.isFile && it.name == "build.gradle.kts" && !exclude(it) }
        println()
        println("Including modules in folder '$folder'")
        buildGradleFiles.forEach {
            val projectFolder = it.parentFile
            val relativePath = projectFolder.relativeTo(settings.rootDir).invariantSeparatorsPath
            val name = ":" + relativePath.replace("/", ":")
            includeModule(settings, relativePath, name)
            println("- $name => $relativePath")
        }
    }

    fun includeDokkaModule(settings: Settings, libraryFolder: String = "library") {
        val relativePath = "$libraryFolder/dokka"
        val name = ":dokka"
        println()
        println("Including dokka module")
        includeModule(settings, relativePath, name)
        println("- $name => $relativePath")
    }

    /**
     * include a module like following:
     *
     * includeModule(":toolbox:core") => "$folder\\library\\core"
     */
    fun includeModule(settings: Settings, fullPath: String, name: String) {
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
    fun includeLibrary(
        settings: Settings,
        libraryFolder: String,
        libraryName: String,
        name: String,
        isInRoot: Boolean = false,
    ) {
        val relativePath = name.replaceFirst(libraryName, if (isInRoot) "" else "library", true)
            .replace("::", ":")
            .replace(":", "\\").removePrefix("\\")
        //println("relativePath: $relativePath")
        includeModule(settings, "$libraryFolder\\$relativePath", name)
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
    fun includeToolbox(
        settings: Settings,
        myLibsFolder: String,
        name: String,
        isInRoot: Boolean = false,
    ) {
        val libraryFolder = "$myLibsFolder\\Toolbox"
        val libraryName = "toolbox"
        includeLibrary(settings, libraryFolder, libraryName, name, isInRoot)
    }

    fun checkGradleProperty(settings: Settings, property: String): Boolean? {
        if (!settings.providers.gradleProperty(property).isPresent) {
            return null
        }
        return settings.providers.gradleProperty(property).get().toBoolean()
    }
}