package com.michaelflisar.kmpdevtools.core.configs

import com.charleskorn.kaml.Yaml
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

        fun read(project: org.gradle.api.initialization.ProjectDescriptor) = readFromProject(project.projectDir)
        fun read(project: Project) = readFromProject(project.rootDir)
        fun readFromProject(root: File) = read(root, relativePath)

        private fun read(root: File, relativePath: String): Config {
            return read(File(root, relativePath))
        }

        private fun read(file: File): Config {
            return try {
                tryRead(file)!!
            } catch (e: Exception) {
                e.printStackTrace()
                throw kotlin.RuntimeException("Failed to read `Config` from path '${file.path}'", e)
            }
        }

        private fun tryRead(file: File): Config? {
            if (!file.exists()) {
                return null
            }
            val content = file.readText(Charsets.UTF_8)
            return Yaml.default.decodeFromString(serializer(), content)
        }
    }


    @Serializable
    class Developer(
        val name: String,
        val mail: String,
        @SerialName("maven-id") val mavenId: String,
        @SerialName("github-user-name") val githubUserName: String,
    )

}

