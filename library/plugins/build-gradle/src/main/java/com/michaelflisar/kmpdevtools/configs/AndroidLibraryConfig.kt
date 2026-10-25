package com.michaelflisar.kmpdevtools.configs

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
            libraryModuleConfig: LibraryModuleConfig.Library,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
        ): AndroidLibraryConfig {
            val namespace = libraryModuleConfig.config
                .getModuleForProject(libraryModuleConfig.project.rootDir, libraryModuleConfig.project.projectDir)
                .androidNamespace(libraryModuleConfig.config)
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
            customNamespaceAddon: String? = null
        ): AndroidLibraryConfig {
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = createNamespaceFromPath(
                    libraryModuleConfig.project,
                    libraryModuleConfig.projectNamespace,
                    customNamespaceAddon
                )
            )
        }

        fun createFromPath(
            appModuleConfig: AppModuleConfig,
            compileSdk: Provider<String>,
            minSdk: Provider<String>,
            enableAndroidResources: Boolean = true,
            customNamespaceAddon: String? = null
        ): AndroidLibraryConfig {
            return AndroidLibraryConfig(
                compileSdk = compileSdk,
                minSdk = minSdk,
                enableAndroidResources = enableAndroidResources,
                namespace = createNamespaceFromPath(
                    appModuleConfig.project,
                    appModuleConfig.projectNamespace,
                    customNamespaceAddon
                )
            )
        }

        private fun createNamespaceFromPath(
            project: Project,
            projectNamespace: String,
            customNamespaceAddon: String?
        ): String {

            if (!customNamespaceAddon.isNullOrBlank()) {
                return "$projectNamespace.$customNamespaceAddon"
            }

            val namespace = project.rootDir
                .toPath()
                .toAbsolutePath()
                .normalize()
                .relativize(
                    project.projectDir
                        .toPath()
                        .toAbsolutePath()
                        .normalize()
                )
                .filter { it.toString() != ".." }
                .joinToString(".") {
                    it.toString().toPackageSegment()
                }

            return if (namespace.isBlank()) {
                projectNamespace
            } else {
                "$projectNamespace.$namespace"
            }
        }

        private fun String.toPackageSegment(): String {
            var value = replace(Regex("[^A-Za-z0-9_]"), "_")

            if (value.firstOrNull()?.isDigit() == true) {
                value = "_$value"
            }

            return value
        }
    }
}