package com.michaelflisar.kmpdevtools

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.configs.app.AndroidAppConfig
import com.michaelflisar.kmpdevtools.configs.app.DesktopAppConfig
import com.michaelflisar.kmpdevtools.configs.library.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.core.configs.AppConfig
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import edu.sc.seis.launch4j.tasks.Launch4jLibraryTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BuildFileUtil {

    fun checkGradleProperty(project: Project, property: String): Boolean? {
        if (!project.providers.gradleProperty(property).isPresent) {
            return null
        }
        return project.providers.gradleProperty(property).get().toBoolean()
    }

    /**
     * Configures the project for publishing to Maven Central.
     * This includes setting up the Maven coordinates, POM metadata, and signing.
     *
     * Usage:
     *
     * to only auto-publish releases without suffixes like "-debug", "-alpha", "-test" use following:
     * <pre><code>
     * autoReleaseOnMavenCentral = { version -> !version.contains("-") }
     * </code></pre>
     *
     * @param project The Gradle project to configure.
     * @param config The general configuration for the project.
     * @param libraryConfig The library configuration for the publication.
     * @param platform The platform configuration for the publication.
     * @param autoReleaseOnMavenCentral A function that determines if releases should be automatically published.
     * @param sign Whether to sign the publications.
     * @param version The version of the library, defaults to the value of the "TAG" environment variable (TAG is set by github action workflow) or "LOCAL-SNAPSHOT".
     */
    fun setupMavenPublish(
        project: Project,
        config: Config,
        libraryConfig: LibraryConfig,
        platform: Platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = true
        ),
        autoReleaseOnMavenCentral: (version: String) -> Boolean = { true },
        sign: Boolean = System.getenv("CI")?.toBoolean() == true,
        version: String = System.getenv("TAG") ?: "LOCAL-SNAPSHOT",
    ) {
        val module = libraryConfig.getModuleForProject(project.rootDir, project.projectDir)

        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(platform)
            coordinates(
                groupId = libraryConfig.maven.groupId,
                artifactId = module.artifactId,
                version = version
            )

            pom {
                name.set(libraryConfig.library.name)
                description.set(module.libraryDescription(libraryConfig))
                inceptionYear.set(libraryConfig.library.release.toString())
                url.set(libraryConfig.library.getRepoLink(config.developer))

                licenses {
                    license {
                        name.set(libraryConfig.library.license.name)
                        url.set(
                            libraryConfig.library.license.getLink(
                                config.developer,
                                libraryConfig.library
                            )
                        )
                    }
                }

                developers {
                    developer {
                        id.set(config.developer.mavenId)
                        name.set(config.developer.name)
                        email.set(config.developer.mail)
                    }
                }

                scm {
                    url.set(libraryConfig.library.getRepoLink(config.developer))
                }
            }

            // Configure publishing to Maven Central
            publishToMavenCentral(autoReleaseOnMavenCentral(version))

            // Enable GPG signing for all publications
            if (sign) {
                signAllPublications()
            }
        }
    }

    fun setupAndroidLibrary(
        project: Project,
        config: Config,
        libraryConfig: LibraryConfig,
        androidConfig: AndroidLibraryConfig,
        buildConfig: Boolean,
    ) {
        val module = libraryConfig.getModuleForProject(project.rootDir, project.projectDir)
        project.extensions.configure(LibraryExtension::class.java) {

            namespace = module.androidNamespace(libraryConfig)

            this.compileSdk = androidConfig.compileSdk.get().toInt()

            buildFeatures {
                this.buildConfig = buildConfig
            }

            defaultConfig {
                this.minSdk = androidConfig.minSdk.get().toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(config.javaVersion)
            }
        }
    }

    fun setupAndroidApp(
        project: Project,
        config: Config,
        appConfig: AppConfig,
        androidAppConfig: AndroidAppConfig,
        generateResAppName: Boolean,
        buildConfig: Boolean,
        checkDebugKeyStoreProperty: Boolean,
        setupBuildTypesDebugAndRelease: Boolean,
        buildTypeDebugSuffix : String = ".debug",
    ) {
        project.extensions.configure(ApplicationExtension::class.java) {

            namespace = appConfig.androidNamespace

            this.compileSdk = androidAppConfig.compileSdk.get().toInt()

            buildFeatures {
                resValues = true // needed for app name as string resource
                this.buildConfig = buildConfig
            }

            defaultConfig {

                this.minSdk = androidAppConfig.minSdk.get().toInt()
                this.targetSdk = androidAppConfig.targetSdk.get().toInt()
                this.versionCode = appConfig.versionCode
                this.versionName = appConfig.versionName

                if (generateResAppName) {
                    resValue(
                        type = "string",
                        name = androidAppConfig.stringResourceIdForAppName,
                        value = appConfig.name
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(config.javaVersion)
            }

            // eventually use local custom signing
            if (checkDebugKeyStoreProperty) {
                val debugKeyStore = project.providers.gradleProperty("debugKeyStore").orNull
                if (debugKeyStore != null) {
                    signingConfigs {
                        getByName("debug") {
                            keyAlias = "androiddebugkey"
                            keyPassword = "android"
                            storeFile = File(debugKeyStore)
                            storePassword = "android"
                        }
                    }
                }
            }

            if (setupBuildTypesDebugAndRelease) {
                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                    debug {
                        isMinifyEnabled = false
                        isShrinkResources = false
                        applicationIdSuffix = buildTypeDebugSuffix
                    }
                }
            }
        }
    }

    fun setupWindowsApp(
        project: Project,
        config: Config,
        application: JvmApplication,
        appConfig: AppConfig,
        desktopAppConfig: DesktopAppConfig,
        configureNativeDistribution: JvmApplicationDistributions.() -> Unit = {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        },
    ) {
        with(application) {

            this.mainClass = desktopAppConfig.mainClass

            nativeDistributions {

                configureNativeDistribution()

                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                packageName = appConfig.name // entspricht dem exe Name
                packageVersion = appConfig.versionName
                description = "${appConfig.name} - Build at ${now.format(formatter)}"
                copyright = "©${now.year} ${config.developer.name}. All rights reserved."
                vendor = config.developer.name

                // https://github.com/JetBrains/compose-multiplatform/issues/1154
                // => suggestRuntimeModules task ausführen um zu prüfen, was man hier hinzufügen sollte
                // modules("java.instrument", "java.security.jgss", "java.sql", "java.xml.crypto", "jdk.unsupported")

                windows {
                    iconFile.set(project.file(desktopAppConfig.ico))
                    //includeAllModules = true
                }
            }
        }
    }

    fun setupLaunch4J(
        project: Project,
        config: Config,
        task: Launch4jLibraryTask,
        appConfig: AppConfig,
        desktopAppConfig: DesktopAppConfig,
        jarTask: String = "flattenReleaseJars",
        outputFile: (exe: File) -> File = { it },
    ) {
        with(task) {

            mainClassName.set(desktopAppConfig.mainClass)
            icon.set(project.file(desktopAppConfig.ico).absolutePath)
            setJarTask(project.tasks.getByName(jarTask))
            outfile.set("${appConfig.name}.exe")

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            productName.set(appConfig.name)
            version.set(appConfig.versionName)
            textVersion.set(appConfig.versionName)
            description = "${appConfig.name} - Build at ${now.format(formatter)}"
            copyright.set("©${now.year} ${config.developer.name}. All rights reserved.")
            companyName.set(config.developer.name)

            doLast {

                val exe = dest.get().asFile

                val finalExe = outputFile(exe)
                if (finalExe != exe) {
                    if (finalExe.exists())
                        finalExe.delete()
                    val moved = exe.renameTo(finalExe)
                    if (!moved)
                        throw Exception("Konnte exe nicht verschieben!")
                }

                println("")
                println("##############################")
                println("#          LAUNCH4J          #")
                println("##############################")
                println("")
                println("Executable wurde in folgendem Ordner erstellt:")
                println(
                    "file:///" + finalExe.parentFile.absolutePath.replace(" ", "%20")
                        .replace("\\", "/")
                )
                println("")
            }
        }
    }
}