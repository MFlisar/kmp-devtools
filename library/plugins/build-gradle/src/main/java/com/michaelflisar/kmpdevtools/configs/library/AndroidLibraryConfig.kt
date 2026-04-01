package com.michaelflisar.kmpdevtools.configs.library

import com.michaelflisar.kmpdevtools.core.configs.AppConfig
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class AndroidLibraryConfig private constructor(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean,
    val namespace: String,
) {
    companion object {

        fun create(
            project: Project,
            libraryConfig: LibraryConfig,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = libraryConfig.getModuleNamespace(project)
            )
        }

        fun createFromPath(
            project: Project,
            appConfig: AppConfig,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            val relativePath =
                project.projectDir.relativeTo(project.rootDir).invariantSeparatorsPath
            val namespace = relativePath.split("/").joinToString(".")
            val androidNamespace = "${appConfig.androidNamespace}.$namespace"
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = androidNamespace
            )
        }
    }
}