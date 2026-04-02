package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.configs.AppConfig
import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project

sealed class LibraryModuleConfig {

    abstract val project: Project
    abstract val config: Config

    val projectNamespace: String
        get() = config.project.namespace

    class Library internal constructor(
        override val project: Project,
        override val config: Config,
        val libraryConfig: LibraryConfig,
    ) : LibraryModuleConfig() {
        fun namespace(): String {
            val module = libraryConfig.getModuleForProject(
                project.rootDir,
                project.projectDir
            )
            return module.androidNamespace(config)
        }
    }

    class Manual internal constructor(
        override val project: Project,
        override val config: Config,
        val appConfig: AppConfig,
    ) : LibraryModuleConfig()

    companion object {
        fun read(project: Project): Library {
            val config = Config.read(project.rootProject)
            val libraryConfig = LibraryConfig.read(project.rootProject)
            val libraryModuleConfig = Library(project, config, libraryConfig)
            return libraryModuleConfig
        }

        fun readManual(project: Project): Manual {
            val config = Config.read(project.rootProject)
            val appConfig = AppConfig.read(project.rootProject)
            val libraryModuleConfig = Manual(project, config, appConfig)
            return libraryModuleConfig
        }
    }
}