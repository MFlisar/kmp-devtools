package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.Config
import org.gradle.api.Project

sealed class LibraryModuleConfig {

    abstract val project: Project
    abstract val config: Config

    val projectNamespace: String
        get() = config.project.namespace

    class Library internal constructor(
        override val project: Project,
        override val config: Config,
    ) : LibraryModuleConfig() {
        fun namespace(): String {
            val module = config.getModuleForProject(
                project.rootDir,
                project.projectDir
            )
            return module.androidNamespace(config)
        }
    }

    class Manual internal constructor(
        override val project: Project,
        override val config: Config,
    ) : LibraryModuleConfig()

    companion object {
        fun read(project: Project): Library {
            val config = Config.read(project.rootDir)
            val libraryModuleConfig = Library(project, config)
            return libraryModuleConfig
        }

        fun readManual(project: Project): Manual {
            val config = Config.read(project.rootDir)
            val libraryModuleConfig = Manual(project, config)
            return libraryModuleConfig
        }
    }
}