package com.michaelflisar.kmpdevtools.readme

import com.michaelflisar.kmpdevtools.core.Platform
import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig

object ReadmeDefaults {

    const val FOLDER_MODULES = "modules"
    const val FOLDER_SCREENSHOTS = "screenshots"
    const val HAS_API_DOCS = true

    val headerTableOfContent = ReadmeRegion("information_source", "Table of Contents")
    val headerScreenshots = ReadmeRegion("camera", "Screenshots")
    val headerSupportedPlatform = ReadmeRegion("computer", "Supported Platforms")
    val headerSetup = ReadmeRegion("wrench", "Setup")
    val headerUsage = ReadmeRegion("rocket", "Usage")
    val headerModules = ReadmeRegion("file_folder", "Modules")
    val headerDemo = ReadmeRegion("sparkles", "Demo")
    val headerMore = ReadmeRegion("information_source", "More")
    val headerOtherLibraries = ReadmeRegion("bulb", "Other Libraries")
    val headerApiDocs = ReadmeRegion("books", "API Documentation")

    val allHeaders = listOf(
        headerTableOfContent,
        headerScreenshots,
        headerSupportedPlatform,
        headerSetup,
        headerUsage,
        headerModules,
        headerDemo,
        headerMore,
        headerOtherLibraries,
        headerApiDocs
    )

    val GithubMyLibrariesLink = "https://mflisar.github.io/Libraries/"// "https://github.com/MFlisar/Libraries"

    val DefaultReadmeTemplate = """
        {{ header }}

        {{ partials.introduction }}

        {{ partials.features }}

        ${headerTableOfContent.markdownHeader()}
        
        {{ tableOfContent }}

        ${headerScreenshots.markdownHeader()}

        {{ screenshots }}

        ${headerSupportedPlatform.markdownHeader()}

        {{ supported_platforms }}

        ${headerSetup.markdownHeader()}

        <details>

        <summary>Dependencies</summary>

        <br>

        Simply add the dependencies inside your **build.gradle.kts** file.

        ```kotlin
        {{ setup-via-dependencies }}
        ```

        </details>

        <details>

        <summary>Version Catalogue</summary>

        <br>

        Define the dependencies inside your **libs.versions.toml** file.

        ```toml
        {{ setup-via-version-catalogue1 }}
        ```

        And then use the definitions in your projects **build.gradle.kts** file like following:

        ```shell
        {{ setup-via-version-catalogue2 }}
        ```

        </details>

        ${headerUsage.markdownHeader()}

        {{ partials.usage }}

        ${headerModules.markdownHeader()}

        {{ modules }}

        ${headerDemo.markdownHeader()}

        {{ demo }}

        ${headerMore.markdownHeader()}

        {{ links }}
        
        ${headerApiDocs.markdownHeader()}
        
        {{ api-docs }}
        
        ${headerOtherLibraries.markdownHeader()}
        
        You can find more libraries (all multiplatform) of mine that all do work together nicely [here]({{ other-libraries }}).

    """.trimIndent()

    val ImageAndroid = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/android-${Platform.ANDROID.color}?style=for-the-badge",
        altText = "Android"
    )
    val ImageIOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/ios-${Platform.IOS.color}?style=for-the-badge",
        altText = "iOS"
    )
    val ImageWindows = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/windows-${Platform.WINDOWS.color}?style=for-the-badge",
        altText = "Windows"
    )
    val ImageMacOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/macos-${Platform.MACOS.color}?style=for-the-badge",
        altText = "macOS"
    )
    val ImageLinux = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/linux-${Platform.LINUX.color}?style=for-the-badge",
        altText = "Linux"
    )
    val ImageWASM = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/wasm-${Platform.WASM.color}?style=for-the-badge",
        altText = "WebAssembly"
    )
    val ImageJS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/javascript-${Platform.JS.color}?style=for-the-badge",
        altText = "JavaScript"
    )

    fun imageMavenCentral(libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/maven-central/v/${libraryConfig.maven.groupId}/${libraryConfig.maven.primaryArtifactId}?style=for-the-badge&color=blue",
        altText = "Maven Central",
        linkUrl = "https://central.sonatype.com/artifact/${libraryConfig.maven.groupId}/${libraryConfig.maven.primaryArtifactId}"
    )

    fun imageAPI(minSdk: Int) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/api-${minSdk}%2B-brightgreen.svg?style=for-the-badge",
        altText = "API"
    )

    fun imageKotlin(config: Config, libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/languages/top/${config.developer.githubUserName}/${libraryConfig.library.name}.svg?style=for-the-badge&amp;color=blueviolet",
        altText = "Kotlin"
    )

    fun imageKMP() = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/Kotlin_Multiplatform-blue?style=for-the-badge&amp;label=Kotlin",
        altText = "Kotlin Multiplatform"
    )

    fun imageLicence(config: Config, libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/license/${config.developer.githubUserName}/${libraryConfig.library.name}?style=for-the-badge",
        altText = "License",
        linkUrl = "https://github.com/${config.developer.githubUserName}/${libraryConfig.library.name}/${libraryConfig.library.license.path}"
    )
}