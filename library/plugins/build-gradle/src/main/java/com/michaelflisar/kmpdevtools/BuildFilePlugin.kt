package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.Config
import com.michaelflisar.kmpdevtools.readme.ReadmeDefaults
import com.michaelflisar.kmpdevtools.readme.UpdateReadmeUtil
import com.michaelflisar.kmpdevtools.tooling.MacActions
import com.michaelflisar.kmpdevtools.tooling.MacDefaults
import com.michaelflisar.kmpdevtools.tooling.ProjectActions
import com.michaelflisar.kmpdevtools.tooling.ToolingSetup
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

//abstract class BuildFilePluginExtension @Inject constructor(private val objects: ObjectFactory) {
//
//}

class BuildFilePlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {

        this.project = project

        //val ext = project.extensions.create("buildFilePlugin", BuildFilePluginExtension::class.java)

        // 1) register root tasks
        if (project == project.rootProject) {
            project.tasks.register("updateMarkdownFiles", UpdateMarkdownFilesTask::class.java)
            project.tasks.register("macActions", MacActionsTask::class.java)
            project.tasks.register("updateDevToolsVersion", UpdateDevToolsVersionTask::class.java)
            project.tasks.register("devTools", DevToolsTask::class.java)
        }
    }
}

// ----------------------------
// Tasks
// ----------------------------

abstract class BaseTask : DefaultTask() {

    @get:InputDirectory
    abstract val rootDirectory: DirectoryProperty

    @get:InputFiles
    abstract val configFiles: ConfigurableFileCollection

    init {
        rootDirectory.convention(project.rootProject.layout.projectDirectory)
        configFiles.from(project.rootProject.layout.projectDirectory.file(Config.FILE_NAME))
    }

    fun readRoot() = rootDirectory.get().asFile

    fun readConfig(): Config {
        val root = readRoot()
        return Config.read(root)
    }

}

abstract class UpdateMarkdownFilesTask : BaseTask() {

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
        val config = readConfig()
        UpdateReadmeUtil.update(
            rootDir = rootDirectory.get().asFile,
            config = config,
            readmeTemplate = template.get(),
            folderModules = folderModules.get(),
            folderScreenshots = folderScreenshots.get(),
            hasApiDocs = hasApiDocs.get().toBoolean()
        )
    }
}

abstract class MacActionsTask : BaseTask() {

    @TaskAction
    fun run() {
        val config = readConfig()
        val sshSetup = MacDefaults.getMacSSHSetup()
        val relativePathRoot =
            MacDefaults.getRelativePathRoot(rootDirectory.get().asFile, config)
        val toolingSetup = ToolingSetup(relativePathRoot)
        MacActions.run(
            projectRootDirectory = rootDirectory.get().asFile,
            sshSetup = sshSetup,
            toolingSetup = toolingSetup,
            config = config,
        )
    }
}

abstract class UpdateDevToolsVersionTask : BaseTask() {

    @TaskAction
    fun run() {
        ProjectActions.updateDevToolsVersion(rootDirectory.get().asFile)
    }
}

abstract class DevToolsTask : BaseTask() {

    @TaskAction
    fun run() {

        val root = readRoot()

        println()
        println("-------------------")
        println("Available Tasks:")
        println("-------------------")
        println("- (1) [u] updateDevToolsVersion")
        println("- (2) [r] project renamer")

        val input = ProjectActions.readUserInput("Enter task number/key: ").lowercase().trim()

        when (input) {
            "1", "u" -> ProjectActions.updateDevToolsVersion(root)
            "2", "r" -> ProjectActions.runProjectRenamer()
            else -> println("Invalid input")
        }

    }
}