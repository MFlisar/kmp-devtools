package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.configs.AppConfig
import com.michaelflisar.kmpdevtools.core.configs.Config
import org.gradle.api.Project

class AppModuleConfig internal constructor(
    val project: Project,
    val config: Config,
    val appConfig: AppConfig,
) {
    val projectNamespace: String
        get() = config.project.namespace

    companion object {
        fun readManual(project: Project): AppModuleConfig {
            val config = Config.readFromProject(project.rootDir)
            val appConfig = AppConfig.readFromProject(project.rootDir)
            return AppModuleConfig(
                project = project,
                config = config,
                appConfig = appConfig
            )
        }
    }
}