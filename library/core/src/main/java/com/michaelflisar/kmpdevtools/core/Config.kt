package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Config(
    val publish: Publish,
    @SerialName("config") val general: General,
    val apps: List<App>,
    val project: Project,
    val modules: Modules,
    val documentation: Documentation,
    val settings: Settings = Settings(),
) {
    companion object {

        const val FILE_NAME = ".config.yml"

        fun read(directory: File, filename: String = FILE_NAME): Config {
            val file = File(directory, filename)
            val content = file.readText(Charsets.UTF_8)
            return Yaml.default.decodeFromString(serializer(), content)
        }
    }

    val javaVersion: String
        get() = general.javaVersion

    val developer: Developer
        get() = general.developer

    fun requireApp(project: org.gradle.api.Project): App {
        val relativePath = project.relativeInvariantProjectPath()
        return requireApp(relativePath)
    }

    fun requireApp(relativeInvariantProjectPath: String): App {
        val app = apps.find { it.invariantPath == relativeInvariantProjectPath }
        return app ?: error("Missing required 'app' section in $FILE_NAME - path: $relativeInvariantProjectPath")
    }


    fun getModuleForProject(rootDir: File, projectDir: File): Modules.Module {
        val path = projectDir.relativeTo(rootDir).invariantSeparatorsPath
        return modules.modules.find { it.invariantPath == path }
            ?: error("Module setup not found for path '$path'; define it in config.yml")
    }

    fun libraryName(): String = project.repoName.lowercase()

    @Serializable
    data class Publish(
        @SerialName("group-id") val groupId: String,
        val target: Target,
        val github: Github? = null,
    ) {
        fun requireMavenCentral() {
            check(target == Target.MAVEN_CENTRAL) {
                "'publish.target' must be 'maven-central' to require Maven Central publishing"
            }
        }

        fun requireGithub(): Github {
            check(target == Target.GITHUB) {
                "'publish.target' must be 'github' to require GitHub publishing"
            }
            return github ?: error("'publish.github' is required when target is 'github'")
        }

        @Serializable
        enum class Target {
            @SerialName("none")
            NONE,

            @SerialName("maven-central")
            MAVEN_CENTRAL,

            @SerialName("github")
            GITHUB,
        }

        @Serializable
        data class Github(
            val user: String,
            val repo: String,
            val branch: String,
            val path: String,
        ) {
            val fullRepoLink: String
                get() = "https://github.com/$user/$repo"

            val fullGithubPageLink: String
                get() = "https://$user.github.io/$repo"

            val fullGithubPageLinkWithPath: String
                get() = "https://$user.github.io/${repo.trimEnd('/')}/${path.trimStart('/')}"
        }
    }

    @Serializable
    data class General(
        @SerialName("java-version") val javaVersion: String,
        val developer: Developer,
    )

    @Serializable
    data class Developer(
        val name: String = "<UNKNOWN>",
        val mail: String = "<NONE>",
        @SerialName("maven-id") val mavenId: String = "<NONE>",
        @SerialName("github-user-name") val githubUserName: String = "<NONE>",
    )

    @Serializable
    data class App(
        val name: String,
        val namespace: String,
        @SerialName("version-name") val versionName: String,
        private val path: String,
        @SerialName("publish-app") val publish: Boolean,
        @SerialName("publish-path") val publishPath: String,
        @SerialName("publish-description") val publishDescription: String,
    ) {
        val invariantPath: String
            get() = path.invariantPath()
    }

    @Serializable
    data class Project(
        val namespace: String,
        @SerialName("repo-name") val repoName: String,
        val release: Int,
        val license: License,
    ) {
        init {
            require(repoName.isNotBlank()) { "'repo-name' must not be blank" }
            // vorerst; könnten auch entfernt werden bei bedarf...
            require(!repoName.contains(" ")) { "'repo-name' must not contain spaces" }
        }

        fun getRepoLink(developer: Developer): String =
            "https://github.com/${developer.githubUserName}/$repoName/"

        @Serializable
        data class License(
            val name: String,
            private val path: String,
        ) {
            fun getLink(developer: Developer, project: Project): String =
                "${project.getRepoLink(developer)}${invariantPath}"

            val invariantPath: String
                get() = path.invariantPath()
        }
    }

    @Serializable
    data class Modules(
        @SerialName("primary-artifact-id") val primaryArtifactId: String,
        val modules: List<Module>,
        val xcframeworks: List<XCFramework>,
    ) {
        @Serializable
        data class Module(
            @SerialName("artifact-id") val artifactId: String,
            val description: String,
            private val path: String,
            val plugin: Boolean = false,
            val internal: Boolean = false,
        ) {
            fun libraryDescription(config: Config): String =
                "${config.project.repoName} - $artifactId module - $description"

            fun androidNamespace(config: Config): String {
                val artifactIdPart = artifactId.replace("-", ".")
                return "${config.project.namespace}.$artifactIdPart"
            }

            val invariantPath: String
                get() = path.invariantPath()
        }

        @Serializable
        data class XCFramework(
            val name: String,
            val path: String,
            val targets: List<String>,
        )
    }

    @Serializable
    data class Documentation(
        @SerialName("root-path") private val rootPath: String,
        @SerialName("folder-partials") val folderPartials: String,
        @SerialName("folder-screenshots") val folderScreenshots: String,
        @SerialName("main-screenshot-path") private val mainScreenshotPath: String? = null
    ) {
        val invariantRoot: String
            get() = rootPath.invariantPath()

        val invariantMainScreenShotPath: String?
            get() = mainScreenshotPath?.invariantPath()
    }

    @Serializable
    data class Settings(
        @SerialName("log-dependencies") val logDependencies: Boolean = false,
    )
}