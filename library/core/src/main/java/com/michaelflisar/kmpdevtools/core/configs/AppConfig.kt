package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.ConfigReader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import java.io.File

@Serializable
data class AppConfig(
    val name: String,
    @SerialName("namespace") val namespace: String,
    @SerialName("version-code") val versionCode: Int,
    @SerialName("version-name") val versionName: String,
) {
    companion object {

        const val relativePath = "configs/app-config.yml"

        fun read(project: org.gradle.api.initialization.ProjectDescriptor) =
            readFromProject(project.projectDir)

        fun read(project: Project) = readFromProject(project.rootDir)
        fun readFromProject(root: File) =
            ConfigReader.readFromProject(root, relativePath, serializer())
    }
}