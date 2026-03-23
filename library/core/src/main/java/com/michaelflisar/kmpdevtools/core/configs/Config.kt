package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.ConfigReader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import java.io.File

@Serializable
data class Config(
    @SerialName("java-version") val javaVersion: String,
    val developer: Developer,
) {
    companion object {

        const val relativePath = "configs/config.yml"

        fun read(project: org.gradle.api.initialization.ProjectDescriptor) =
            readFromProject(project.projectDir)

        fun read(project: Project) = readFromProject(project.rootDir)
        fun readFromProject(root: File) =
            ConfigReader.readFromProject(root, relativePath, serializer())
    }


    @Serializable
    class Developer(
        val name: String,
        val mail: String,
        @SerialName("maven-id") val mavenId: String,
        @SerialName("github-user-name") val githubUserName: String,
    )

}

