package com.michaelflisar.kmpdevtools.core.utils

import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project
import java.io.File
import java.util.Properties

class ProjectData(
    val packageNameTo: String,
    val libraryNameTo: String,
    val pathStateFile: String = "configs/state.properties",
    val pathRunConfigFolders: String = ".run",
    val root: File = File(System.getProperty("user.dir")),
) {
    constructor(
        projectRootDirectory: File,
        libraryConfig: LibraryConfig,
        pathStateFile: String = "configs/state.properties",
        pathRunConfigFolders: String = ".run",
    ) : this(
        packageNameTo = libraryConfig.library.namespace,
        libraryNameTo = libraryConfig.library.name,
        pathStateFile = pathStateFile,
        pathRunConfigFolders = pathRunConfigFolders,
        root = projectRootDirectory,
    )

    constructor(
        project: Project,
        libraryConfig: LibraryConfig = LibraryConfig.read(project),
        pathStateFile: String = "configs/state.properties",
        pathRunConfigFolders: String = ".run",
        root: File = project.rootDir
    ) : this(
        packageNameTo = libraryConfig.library.namespace,
        libraryNameTo = libraryConfig.library.name,
        pathStateFile = pathStateFile,
        pathRunConfigFolders = pathRunConfigFolders,
        root = root,
    )

    val fileStateProperties = File(root, pathStateFile)
    val state = Properties().apply {
        fileStateProperties.inputStream().use { load(it) }
    }

    val packageNameFrom = state.getProperty("packageNameFrom")!!
    val libraryNameFrom = state.getProperty("libraryNameFrom")!!

    val fromPath = packageNameFrom.replace('.', File.separatorChar)
    val toPath = packageNameTo.replace('.', File.separatorChar)

    fun updateStateFile() {
        state.setProperty("packageNameFrom", packageNameTo)
        state.setProperty("libraryNameFrom", libraryNameTo)
        fileStateProperties.outputStream().use { state.store(it, null) }
    }

    /**
     * Updates the content of the given file by replacing occurrences of the old package name and library name
     * with the new ones, based on the provided flags.
     *
     * @param file the file to be updated.
     * @param replacePackageName whether to replace the package name.
     * @param replaceLibraryName whether to replace the library name.
     * @param log whether to log the update process.
     */
    fun updateFile(
        file: File,
        replacePackageName: Boolean = false,
        replaceLibraryName: Boolean = false,
        log: Boolean = true,
    ) {
        val content = file.readText()
        var newContent = content
        if (replacePackageName) {
            newContent = newContent.replace(packageNameFrom, packageNameTo)
        }
        if (replaceLibraryName) {
            newContent = newContent.replace(libraryNameFrom, libraryNameTo)
        }
        if (content != newContent) {
            file.writeText(newContent)
            if (log)
                println("Updated file: ${file.absolutePath}")
        } else {
            if (log)
                println("No changes needed for file: ${file.absolutePath}")
        }
    }

    fun asStringMap(): Map<String, String> {
        return mapOf(
            "Project Root" to root.absolutePath,
            "Package Name From" to packageNameFrom,
            "Package Name To" to packageNameTo,
            "Library Name From" to libraryNameFrom,
            "Library Name To" to libraryNameTo,
            "Path State File" to pathStateFile,
            "Path Run Config Folders" to pathRunConfigFolders,
        )
    }
}

object ProjectRenamer {

    /**
     * Renames all package folders recursively, regardless of their depth in the directory tree.
     * If the target directory already exists, moves all content from the source to the target (overwriting files if needed).
     * deletes empty fromPaths afterwards
     *
     * @param data the ProjectData containing the necessary information for renaming.
     * @param log whether to log the renaming process (default is true).
     */
    fun renameFolder(
        data: ProjectData,
        log: Boolean = true,
    ) {
        val dirsToRename = FolderUtil.findFoldersWithPath(data.root, data.fromPath)
        if (log)
            println("renameFolders => dirsToRename: " + dirsToRename.size)
        for (dir in dirsToRename) {
            val relative = dir.relativeTo(data.root).path
            val newRelative = relative.replace(data.fromPath, data.toPath)
            val newDir = File(data.root, newRelative)
            if (log)
                print("renaming folder: $relative => $newRelative => result = ")
            if (dir != newDir) {
                if (!newDir.exists()) {
                    // Create parent directories if needed
                    newDir.parentFile?.mkdirs()
                    dir.renameTo(newDir)
                    if (log)
                        print("RENAMED")
                } else {
                    // Move all content from dir to newDir, overwriting existing files
                    FolderUtil.moveDirectoryContent(dir, newDir)
                    // Optionally delete dir if empty
                    if (dir.listFiles()?.isEmpty() == true) {
                        dir.delete()
                    }
                    if (log)
                        print("MERGED")
                }
            } else {
                if (log)
                    print("SKIPPED (same dir)")
            }
            if (log)
                print("\n")
        }
    }

    /**
     * Renames the project by updating imports, package names, and module references based on the provided ProjectData.
     *
     * Imports:
     * Renames all import statements from packageNameFrom to packageNameTo in Kotlin files. E.g. following:
     * - "import com.old.package" -> "import com.new.package"
     *
     * Package Names:
     * Renames all package declarations from packageNameFrom to packageNameTo in Kotlin files. E.g. following:
     * - "package com.old.package" -> "package com.new.package"
     *
     * Module References:
     * Renames all module references in gradle.kts files from libraryNameFrom to libraryNameTo. E.g. following:
     * - project(":libraryoldname:...") -> project(":librarynewname:...")
     *
     * @param data the ProjectData containing the necessary information for renaming.
     * @param renameImports whether to rename import statements.
     * @param renamePackageNames whether to rename package declarations.
     * @param renameModuleReferences whether to rename module references.
     * @param log whether to log the renaming process (default is true).
     */
    fun renameProject(
        data: ProjectData,
        renameImports: Boolean,
        renamePackageNames: Boolean,
        renameModuleReferences: Boolean,
        log: Boolean = true,
    ) {
        if (renameImports)
            renameImports(data.root, data.packageNameFrom, data.packageNameTo, log)
        if (renamePackageNames)
            renamePackageNames(data.root, data.packageNameFrom, data.packageNameTo, log)
        if (renameModuleReferences)
            renameModuleReferences(data.root, data.libraryNameFrom, data.libraryNameTo, log)
    }

    /**
     * Updates run configuration files in the specified run config folder by replacing occurrences
     * of the old library name with the new library name.
     *
     * @param data the ProjectData containing the necessary information for updating run configurations.
     * @param log whether to log the update process (default is true).
     */
    fun updateRunConfigurations(
        data: ProjectData,
        log: Boolean = true,
    ) {
        val runConfigDir = File(data.root, data.pathRunConfigFolders)
        if (runConfigDir.exists()) {
            var changedFiles = 0
            runConfigDir.walkTopDown()
                .filter { it.isFile && it.extension == "xml" }
                .forEach { file ->
                    val originalText = file.readText()
                    val newText = originalText.replace(
                        data.libraryNameFrom.lowercase(),
                        data.libraryNameTo.lowercase()
                    )
                    if (originalText != newText) {
                        file.writeText(newText)
                        changedFiles++
                    }
                }
            if (log)
                println("updateRunConfigurations => changedFiles: $changedFiles")
        } else {
            if (log)
                println("updateRunConfigurations => run config folder does not exist: ${runConfigDir.absolutePath}")
        }
    }


    /**
     * Renames a package from packageNameFrom to packageNameTo within the given root directory.
     * This includes renaming folder structures as well as updating import statements and package declarations in Kotlin files.
     */
    fun rename(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean = true,
    ): Boolean {

        // Convert package names to path format
        val fromPath = packageNameFrom.replace('.', File.separatorChar)
        val toPath = packageNameTo.replace('.', File.separatorChar)

        // 1) rename all folders
        renameFolders(root, fromPath, toPath, log)

        // 2) rename imports/packagenames/references in all files
        renameLibraryIdInRootSettingsGradleKts(root, libraryNameFrom, libraryNameTo, log)
        renameImports(root, packageNameFrom, packageNameTo, log)
        renamePackageNames(root, packageNameFrom, packageNameTo, log)
        renameModuleReferences(root, libraryNameFrom, libraryNameTo, log)

        return true
    }


    private fun renameFolders(
        root: File,
        fromPath: String,
        toPath: String,
        log: Boolean,
    ) {

    }

    /**
     * Replaces all import statements in Kotlin files from the old package name to the new package name.
     * Only .kt files are processed, recursively from the given root directory.
     */
    private fun renameImports(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        log: Boolean,
    ) {
        // Prepare the import statement prefix
        val importFrom = "import $packageNameFrom"
        val importTo = "import $packageNameTo"

        // Recursively process all .kt files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace(importFrom, importTo)
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }
        if (log)
            println("renameImports => changedFiles: $changedFiles")
    }

    /**
     * Replaces all package statements in Kotlin files from the old package name to the new package name.
     * Only .kt(s) files are processed, recursively from the given root directory.
     */
    private fun renamePackageNames(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        log: Boolean,
    ) {
        // Prepare the import statement prefix
        val packageFrom = "package $packageNameFrom"
        val packageTo = "package $packageNameTo"

        // Recursively process all .kt files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.extension in listOf("kt", "kts") }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace(packageFrom, packageTo)
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }

        if (log)
            println("renameImports => changedFiles: $changedFiles")
    }

    /**
     * Replaces all references to the old package name with the new package name in Kotlin and Gradle files.
     * Processes gradle.kts files only
     *
     * following is replaced e.g.:
     *
     * project(":librarytemplate:...)
     *
     */
    private fun renameModuleReferences(
        root: File,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean,
    ) {
        // Recursively process all .kt(s) and .gradle.kts files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".gradle.kts") }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace(
                    "project(\":${libraryNameFrom.lowercase()}:",
                    "project(\":${libraryNameTo.lowercase()}:"
                )
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }

        if (log)
            println("renameModuleReferences => changedFiles: $changedFiles")
    }

    /**
     * Replaces libraryId in root settings.gradle.kts
     *
     * val libraryId = "librarytemplate"
     */
    private fun renameLibraryIdInRootSettingsGradleKts(
        root: File,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean,
    ) {
        val buildGradleKtsFile = File(root, "settings.gradle.kts")
        if (buildGradleKtsFile.exists()) {
            val originalText = buildGradleKtsFile.readText()
            val newText =
                originalText.replace(libraryNameFrom.lowercase(), libraryNameTo.lowercase())
            if (originalText != newText) {
                buildGradleKtsFile.writeText(newText)
                if (log)
                    println("renameLibraryIdInRootSettingsGradleKts => updated build.gradle.kts")
            } else {
                if (log)
                    println("renameLibraryIdInRootSettingsGradleKts => no changes needed")
            }
        }
    }
}