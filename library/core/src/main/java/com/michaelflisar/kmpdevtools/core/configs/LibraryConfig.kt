package com.michaelflisar.kmpdevtools.core.configs

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import java.io.File

@Serializable
data class LibraryConfig(
    @SerialName("github") val library: GithubLibrary,
    val maven: Maven,
    val modules: List<Module>,
    val xcframeworks: List<XCFramework>,
) {
    companion object {

        const val relativePath = "configs/library-config.yml"

        fun read(project: org.gradle.api.initialization.ProjectDescriptor) =
            readFromProject(project.projectDir)

        fun read(project: Project) = readFromProject(project.rootDir)
        fun readFromProject(root: File) = read(root, relativePath)
        fun readFromFile(file: File) = read(file)

        private fun read(root: File, relativePath: String): LibraryConfig {
            return read(File(root, relativePath))
        }

        private fun read(file: File): LibraryConfig {
            return try {
                tryRead(file)!!
            } catch (e: Exception) {
                e.printStackTrace()
                throw kotlin.RuntimeException(
                    "Failed to read `LibraryConfig` from path '${file.path}'",
                    e
                )
            }
        }

        private fun tryRead(file: File): LibraryConfig? {
            if (!file.exists()) {
                return null
            }
            val content = file.readText(Charsets.UTF_8)
            return Yaml.default.decodeFromString(serializer(), content)
        }
    }

    fun getModuleForProject(rootDir: File, projectDir: File): Module {
        val path = projectDir.relativeTo(rootDir).path
        return getModuleByPath(path)
    }

    private fun getModuleByPath(path: String): Module {
        return modules.find { it.path.replace("\\", "/") == path.replace("\\", "/") }
            ?: throw RuntimeException("module setup definition not found for path: $path => make sure to define it inside library config yml file")
    }

    @Serializable
    class GithubLibrary(
        val name: String,
        val release: Int,
        val namespace: String,
        val license: License,
    ) {
        fun getRepoLink(developer: Config.Developer): String {
            return "https://github.com/{${developer.githubUserName}}/${name}/"
        }

        @Serializable
        class License(
            val name: String,
            val path: String,
        ) {
            fun getLink(developer: Config.Developer, library: GithubLibrary): String {
                val link = library.getRepoLink(developer)
                return "$link/$path".replace("//", "/")
            }
        }
    }

    @Serializable
    class Maven(
        @SerialName("group-id") val groupId: String,
        @SerialName("primary-artifact-id") val primaryArtifactId: String,
    )

    @Serializable
    class Module(
        val name: String,
        @SerialName("artifact-id") val artifactId: String,
        val description: String,
        val path: String,
        @SerialName("exclude-from-docs") val excludeFromDocs: Boolean = false,
    ) {
        fun libraryDescription(setup: LibraryConfig): String {
            val library = setup.library.name
            return "$library - $artifactId module - $description"
        }

        fun androidNamespace(setup: LibraryConfig): String {
            val namespace = setup.library.namespace
            val artifactIdPart = artifactId.replace("-", ".")
            return "$namespace.$artifactIdPart"
        }
    }

    @Serializable
    class XCFramework(
        val name: String,
        val path: String,
        val targets: List<String>,
    )
}

