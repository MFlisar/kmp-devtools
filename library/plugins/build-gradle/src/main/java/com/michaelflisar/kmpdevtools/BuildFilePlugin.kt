package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.ProjectData
import com.michaelflisar.kmpdevtools.readme.ReadmeDefaults
import com.michaelflisar.kmpdevtools.readme.UpdateReadmeUtil
import com.michaelflisar.kmpdevtools.tooling.MacActions
import com.michaelflisar.kmpdevtools.tooling.MacDefaults
import com.michaelflisar.kmpdevtools.tooling.ProjectActions
import com.michaelflisar.kmpdevtools.tooling.ToolingSetup
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class BuildFilePluginExtension @Inject constructor(objects: ObjectFactory) {

    abstract val excludeDemoFromCI: Property<Boolean>

    init {
        excludeDemoFromCI.convention(true)
    }
}

class BuildFilePlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {

        this.project = project

        val ext = project.extensions.create("buildFilePlugin", BuildFilePluginExtension::class.java)

        // 1) exclude all demo projects from CI builds (if configured)
        if (ext.excludeDemoFromCI.get() && project.path.contains(
                ":demo:",
                ignoreCase = true
            ) && System.getenv("CI") == "true"
        ) {
            project.tasks.configureEach { enabled = false }
        }

        // 3) register tasks
        project.tasks.register("updateMarkdownFiles", UpdateMarkdownFilesTask::class.java)
        project.tasks.register("macActions", MacActionsTask::class.java)
        project.tasks.register("renameProject", RenameProjectTask::class.java)
    }
}

// ----------------------------
// Tasks
// ----------------------------

/**
 * Base task that provides access to the config files
 *
 * needed to get gradle caching working properly
 */
abstract class ConfigDependentTask : DefaultTask() {

    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:InputFile
    abstract val libraryConfigFile: RegularFileProperty

    init {
        configFile.convention(project.rootProject.layout.projectDirectory.file(Config.relativePath))
        libraryConfigFile.convention(project.rootProject.layout.projectDirectory.file(LibraryConfig.relativePath))
    }

}

abstract class UpdateMarkdownFilesTask : ConfigDependentTask() {

    @get:Input
    abstract val template: Property<String>

    @get:Input
    abstract val folderModules: Property<String>

    @get:Input
    abstract val folderScreenshots: Property<String>

    @get:Input
    abstract val hasApiDocs: Property<String>

    init {
        template.convention(ReadmeDefaults.DefaultReadmeTemplate)
        folderModules.convention(ReadmeDefaults.FOLDER_MODULES)
        folderScreenshots.convention(ReadmeDefaults.FOLDER_SCREENSHOTS)
        hasApiDocs.convention(ReadmeDefaults.HAS_API_DOCS.toString())
    }

    @TaskAction
    fun run() {
        val config = Config.read(project.rootProject)
        val libraryConfig = LibraryConfig.read(project.rootProject)
        UpdateReadmeUtil.update(
            rootDir = project.rootDir,
            config = config,
            libraryConfig = libraryConfig,
            readmeTemplate = template.get(),
            folderModules = folderModules.get(),
            folderScreenshots = folderScreenshots.get(),
            hasApiDocs = hasApiDocs.get().toBoolean()
        )
    }
}

abstract class MacActionsTask : ConfigDependentTask() {

    @TaskAction
    fun run() {
        val sshSetup = MacDefaults.getMacSSHSetup()
        val relativePathRoot = MacDefaults.getRelativePathRoot(project.rootProject)
        val toolingSetup = ToolingSetup(
            root = relativePathRoot
        )
        MacActions.run(
            project = project.rootProject,
            sshSetup = sshSetup,
            toolingSetup = toolingSetup
        )
    }
}

abstract class RenameProjectTask : ConfigDependentTask() {

    @TaskAction
    fun run() {
        val data = ProjectData(project = project.rootProject)
        ProjectActions.runProjectRenamer(data)
    }
}