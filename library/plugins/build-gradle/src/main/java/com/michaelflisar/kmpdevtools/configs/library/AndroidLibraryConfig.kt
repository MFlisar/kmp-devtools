package com.michaelflisar.kmpdevtools.configs.library

import com.michaelflisar.kmpdevtools.configs.module.AppModuleConfig
import com.michaelflisar.kmpdevtools.configs.module.LibraryModuleConfig
import org.gradle.api.provider.Provider

class AndroidLibraryConfig private constructor(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean,
    val namespace: String,
) {
    companion object {

        fun create(
            libraryModuleConfig: LibraryModuleConfig.Library,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            val namespace = libraryModuleConfig.libraryConfig.getModuleNamespace(
                libraryModuleConfig.project,
                libraryModuleConfig.config
            )
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = namespace
            )
        }

        fun createFromPath(
            libraryModuleConfig: LibraryModuleConfig.Manual,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            val relativePath =
                libraryModuleConfig.project.projectDir.relativeTo(libraryModuleConfig.project.rootDir).invariantSeparatorsPath
            val namespace = relativePath.split("/").joinToString(".")
            val androidNamespace = "${libraryModuleConfig.projectNamespace}.$namespace"
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = androidNamespace
            )
        }

        fun createFromPath(
            appModuleConfig: AppModuleConfig,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            val relativePath =
                appModuleConfig.project.projectDir.relativeTo(appModuleConfig.project.rootDir).invariantSeparatorsPath
            val namespace = relativePath.split("/").joinToString(".")
            val androidNamespace = "${appModuleConfig.projectNamespace}.$namespace"
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = androidNamespace
            )
        }
    }
}