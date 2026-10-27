package com.michaelflisar.kmpdevtools.configs

import com.michaelflisar.kmpdevtools.core.relativeInvariantProjectPath
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

            val libraryInvariantPath = libraryModuleConfig.project.relativeInvariantProjectPath()
            val app = libraryModuleConfig.config
                .apps
                .find { it.invariantModulePaths.contains(libraryInvariantPath) }

            // 1) prüfen ob project in config apps target paths liegt
            val namespace = if (app != null) {
                val ns = app.namespace
                val addons = app.commonInvariantModulePath()
                    .replace(libraryInvariantPath, "")
                    .split("/")
                    .filter { it.isNotBlank() }
                ns + (if (addons.isNotEmpty()) "." else "") + addons.joinToString(".")
            }
            // 2) aus modulen holen
            else {

                libraryModuleConfig.config
                    .getModuleForProject(
                        libraryModuleConfig.project.rootDir,
                        libraryModuleConfig.project.projectDir
                    )
                    .androidNamespace(libraryModuleConfig.config)
            }

            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = namespace
            )
        }
    }
}