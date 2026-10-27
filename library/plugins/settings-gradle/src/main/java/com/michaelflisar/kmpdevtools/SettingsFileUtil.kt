package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.Config
import com.michaelflisar.kmpdevtools.core.utils.ModuleUtil
import org.gradle.api.initialization.Settings
import java.io.File

object SettingsFileUtil {

    fun includeModules(
        settings: Settings,
        libraryName: String,
        config: Config,
        libraryFolder: String = "library",
    ) {
        val modules = config.modules.modules
        val allPaths = modules.map { it.invariantPath }.distinct()
        val foldersInPaths = allPaths.map { it.substringBeforeLast("/", "") }.distinct()

        // include all folders first
        foldersInPaths.forEach {
            val name = ModuleUtil.folderToModuleName(it, libraryName, libraryFolder)
            val projectDir = File(settings.rootDir, it)
            settings.include(name)
            settings.project(name).projectDir = projectDir
        }

        // then include all modules
        println()
        println("Including modules - libraryName = '$libraryName'")
        modules.forEach {
            val name = ModuleUtil.folderToModuleName(it.invariantPath, libraryName, libraryFolder)

            val projectDir = File(settings.rootDir, it.invariantPath)
            if (!projectDir.isDirectory) {
                error("The module from the config file does not exist:\n$name => ${it.invariantPath}")
            }

            includeModule(settings, it.invariantPath, name)
        }
    }

    fun includeAllModules(
        settings: Settings,
        exclude: (buildGradleFile: File) -> Boolean = { false },
    ) {
        includeModulesInFolder(settings, settings.rootDir.path, exclude)
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
        }
    }

    fun includeDokkaModule(settings: Settings, libraryFolder: String = "library") {
        val relativePath = "$libraryFolder/dokka"
        val name = ":dokka"
        println()
        println("Including dokka module")
        includeModule(settings, relativePath, name)
    }

    /**
     * include a module like following:
     *
     * includeModule(":toolbox:core") => "$folder\\library\\core"
     */
    fun includeModule(settings: Settings, fullPath: String, name: String) {
        val projectDir = File(settings.rootDir, fullPath)
        if (!projectDir.isDirectory) {
            error("The directory for the $name module does not exit! ($fullPath)")
        }
        settings.include(name)
        settings.project(name).projectDir = projectDir
        println("- $name => $fullPath")
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

    /**
     * checks system flags:
     *
     */
    fun includeAppsAndDemos() : Boolean {
        return System.getenv("CI") != "true" || System.getenv("INCLUDE_DEMO_MODULES") == "true"
    }
}