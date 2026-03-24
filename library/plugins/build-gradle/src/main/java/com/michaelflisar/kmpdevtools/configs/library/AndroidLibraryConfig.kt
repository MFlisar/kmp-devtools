package com.michaelflisar.kmpdevtools.configs.library

import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class AndroidLibraryConfig private constructor(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean,
    private val namespaceAddon: String?
) {
    companion object {
        fun create(
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ) : AndroidLibraryConfig{
             return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespaceAddon = null
            )
        }

        fun createManualNamespace(
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
            namespaceAddon: String
        ) : AndroidLibraryConfig {
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespaceAddon = namespaceAddon
            )
        }
    }

    fun getNamespace(
        project: Project,
        libraryConfig: LibraryConfig
    ): String {
        return if (namespaceAddon != null) {
            "${libraryConfig.library.namespace}.$namespaceAddon"
        } else {
            val module = libraryConfig.getModuleForProject(project.rootDir, project.projectDir)
            module.androidNamespace(libraryConfig)
        }
    }
}