package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.Config
import com.michaelflisar.kmpdevtools.core.relativeInvariantProjectPath
import org.gradle.api.Project

class AppModuleConfig internal constructor(
    val project: Project,
    val config: Config,
    val app: Config.App,
) {
    val projectNamespace: String
        get() = config.project.namespace

    val relativeInvariantProjectPath
        get() = project.relativeInvariantProjectPath()

    companion object {

        fun read(project: Project): AppModuleConfig {

            val config = Config.read(project.rootDir)

            val appInvariantPath = project.relativeInvariantProjectPath()
            val app = config.apps.find { it.invariantModulePaths.contains(appInvariantPath) } ?:
                error("[AppModuleConfig::read] Missing required 'app' section in ${Config.FILE_NAME} - path: $appInvariantPath")

            return AppModuleConfig(
                project = project,
                config = config,
                app = app
            )
        }
    }
}