package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.Config
import org.gradle.api.Project

class AppModuleConfig internal constructor(
    val project: Project,
    val config: Config,
    val app: Config.App,
) {
    val projectNamespace: String
        get() = config.project.namespace

    companion object {
        fun readManual(project: Project): AppModuleConfig {
            val config = Config.read(project.rootDir)
            return AppModuleConfig(
                project = project,
                config = config,
                app = config.requireApp(),
            )
        }
    }
}