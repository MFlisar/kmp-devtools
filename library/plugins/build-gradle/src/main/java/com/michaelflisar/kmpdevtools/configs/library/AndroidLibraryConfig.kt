package com.michaelflisar.kmpdevtools.configs.library

import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class AndroidLibraryConfig private constructor(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean,
    val namespaceAddon: String
) {
    companion object {
        fun create(
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
            project: Project,
            libraryConfig: LibraryConfig
        ) = AndroidLibraryConfig(
            compileSdk = compileSdk,
            minSdk = minSdk,
            enableAndroidResources = enableAndroidResources,
            namespaceAddon = libraryConfig.getModuleForProject(project.rootDir, project.projectDir).artifactId
        )

        fun createManualNamespace(
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
            namespaceAddon: String
        ) = AndroidLibraryConfig(
            compileSdk = compileSdk,
            minSdk = minSdk,
            enableAndroidResources = enableAndroidResources,
            namespaceAddon = namespaceAddon
        )
    }
}