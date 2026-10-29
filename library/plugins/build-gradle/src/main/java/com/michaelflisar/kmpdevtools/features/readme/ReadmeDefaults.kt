package com.michaelflisar.kmpdevtools.features.readme

import com.michaelflisar.kmpdevtools.core.Target
import com.michaelflisar.kmpdevtools.core.Config
import com.michaelflisar.kmpdevtools.features.readme.classes.ReadmeRegion

object ReadmeDefaults {

    const val HAS_API_DOCS = true

    val headerTableOfContent = ReadmeRegion(null, "Table of Contents")
    val headerScreenshot = ReadmeRegion("camera", "Screenshot")
    val headerSupportedPlatform = ReadmeRegion("computer", "Supported Platforms")
    val headerVersions = ReadmeRegion("arrow_right", "Versions")
    val headerSetup = ReadmeRegion("wrench", "Setup")
    val headerUsage = ReadmeRegion("rocket", "Usage")
    val headerDemo = ReadmeRegion("sparkles", "Demo")
    val headerOtherLibraries = ReadmeRegion("bulb", "Other Libraries")
    val headerCompatibility = ReadmeRegion("twisted_rightwards_arrows", "Compatibility")
    val headerApiDocs = ReadmeRegion("books", "API")

    // gleich sortieren wie im template!
    val allHeaders = listOf(
        headerScreenshot,
        headerTableOfContent,
        headerSupportedPlatform,
        headerVersions,
        headerSetup,
        headerUsage,
        headerDemo,
        headerApiDocs,
        headerOtherLibraries
    )

    val GithubMyLibrariesLink = "https://mflisar.github.io/Libraries/"// "https://github.com/MFlisar/Libraries"
    val GithubLibrariesCompatibilityLink = "https://mflisar.github.io/Libraries/compatibilities/"

    val DefaultReadmeTemplate = """
        {{ header }}

        {{ partials.info }}
        
        {{ interactiveWasmDemoButton }}
        
        ${headerScreenshot.markdownHeader()}
        
        {{ screenshot }}
        
        ${headerTableOfContent.markdownHeader()}
        
        {{ tableOfContent }}

        ${headerSupportedPlatform.markdownHeader()}

        {{ supported-platforms }}
        
        ${headerVersions.markdownHeader()}

        {{ versions }}
        
        {{ experimental }}

        ${headerSetup.markdownHeader()}
        
        {{ setup-add-repository }}
        
        <details open>

        <summary><b>Using Version Catalogs</b></summary>

        <br>

        Define the dependencies inside your **libs.versions.toml** file.

        ```toml
        {{ setup-via-version-catalogue1 }}
        ```

        And then use the definitions in your projects **build.gradle.kts** file like following:

        ```java
        {{ setup-via-version-catalogue2 }}
        ```

        </details>

        <details>

        <summary><b>Direct Dependency Notation</b></summary>

        <br>

        Simply add the dependencies inside your **build.gradle.kts** file.

        ```kotlin
        {{ setup-via-dependencies }}
        ```

        </details>

        ${headerUsage.markdownHeader()}

        {{ partials.usage }}
        
        {{ usage-browse-infos }}

        ${headerDemo.markdownHeader()}

        {{ demo }}
        
        ${headerApiDocs.markdownHeader()}
        
        {{ api-docs }}
        
        ${headerOtherLibraries.markdownHeader()}
        
        You can find more of my multiplatform libraries that work well together [here]({{ other-libraries }}).
        
        ${headerCompatibility.markdownHeader()}

        When combining my libraries, you can find compatibility information [here]({{ compatibility-link }}).

    """.trimIndent()

    val ImageSupportedPlatforms = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/PLATFORMS-black?style=for-the-badge",
        altText = "Platforms"
    )

    val ImageAndroid = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/android-${Target.ANDROID.color}?style=for-the-badge",
        altText = "Android"
    )
    val ImageIOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/ios-${Target.IOS.color}?style=for-the-badge",
        altText = "iOS"
    )
    val ImageJVM = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/jvm-${Target.JVM.color}?style=for-the-badge",
        altText = "JVM"
    )
    val ImageMacOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/macos-${Target.MACOS.color}?style=for-the-badge",
        altText = "macOS"
    )
    val ImageLinux = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/linux-${Target.LINUX.color}?style=for-the-badge",
        altText = "Linux"
    )
    val ImageWASM = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/wasm-${Target.WASM.color}?style=for-the-badge",
        altText = "WebAssembly"
    )
    val ImageJS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/javascript-${Target.JS.color}?style=for-the-badge",
        altText = "JavaScript"
    )

    fun imageInteractiveDemo(
        label: String,
        url: String
    ) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/${label.replace(" ", "_").replace("-", "--")}-2962FF?style=for-the-badge&logo=googlechrome&logoColor=white",
        altText = "Open Interactive Demo",
        linkUrl = url
    )

    val imageInteractiveDemo = "https://raw.githubusercontent.com/MFlisar/kmp-devtools/refs/heads/main/resources/open-interactive-demo.svg"

    fun imageMavenCentral(config: Config) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/maven-central/v/${config.publish.groupId}/${config.modules.primaryArtifactId}?color=blue",
        altText = "Maven Central",
        linkUrl = "https://central.sonatype.com/artifact/${config.publish.groupId}/${config.modules.primaryArtifactId}"
    )

    fun imageGithubMaven(github: Config.Publish.Github)= UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/${github.user}-${github.repo.replace("-", "--")}-brightgreen?color=blue",
        altText = "${github.user}/${github.repo}",
        linkUrl = github.fullGithubPageLink
    )

    fun imageAPI(minSdk: Int) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/api-${minSdk}%2B-brightgreen.svg",
        altText = "API"
    )

    fun imageKotlin(config: Config) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/languages/top/${config.developer.githubUserName}/${config.project.repoName}.svg?color=blueviolet",
        altText = "Kotlin"
    )

    fun imageKMP() = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/Kotlin_Multiplatform-blue?label=Kotlin",
        altText = "Kotlin Multiplatform"
    )

    fun imageLicence(config: Config) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/license/${config.developer.githubUserName}/${config.project.repoName}",
        altText = "License",
        linkUrl = "https://github.com/${config.developer.githubUserName}/${config.project.repoName}/${config.project.license.invariantPath}"
    )
}