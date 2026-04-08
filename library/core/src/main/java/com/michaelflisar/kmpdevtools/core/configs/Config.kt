package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.BaseConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("java-version") val javaVersion: String,
    val developer: Developer,
    val project: Project,
    val readme: Readme,
) : BaseConfig() {

    companion object : BaseConfigCompanion<Config>("config.yml", Config.serializer())

    @Serializable
    class Developer(
        val name: String,
        val mail: String,
        @SerialName("maven-id") val mavenId: String,
        @SerialName("github-user-name") val githubUserName: String,
    )

    @Serializable
    class Project(
        val namespace: String,
    ) {

    }

    /*
     * readme:
        screenshots:
          excludeRoot: true
           excludeFolders: [previews]
           excludeImages: []
     */
    @Serializable
    class Readme(
        val screenshots: Screenshots,
    ) {
        @Serializable
        class Screenshots(
            @SerialName("exclude-root") val excludeRoot: Boolean,
            @SerialName("group-by-folder") val groupByFolders: Boolean,
            @SerialName("excluded-folders") val excludedFolders: List<String>,
            @SerialName("excluded-images") val excludedImages: List<String>,
        )
    }
}