package com.michaelflisar.kmpdevtools

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.crash.afterEvaluate
import com.michaelflisar.composechangelog.format.ChangelogVersionFormatter
import com.michaelflisar.kmpdevtools.configs.AndroidAppConfig
import com.michaelflisar.kmpdevtools.configs.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.configs.AppModuleConfig
import com.michaelflisar.kmpdevtools.configs.DesktopAppConfig
import com.michaelflisar.kmpdevtools.configs.LibraryModuleConfig
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import com.vanniktech.maven.publish.SourcesJar
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
     * @param libraryModuleConfig The configuration for the library module being published.
     * @param platform The platform configuration for the publication.
     * @param autoReleaseOnMavenCentral A function that determines if releases should be automatically published.
     * @param sign Whether to sign the publications.
     * @param version The version of the library, defaults to the value of the "TAG" environment variable (TAG is set by github action workflow) or "LOCAL-SNAPSHOT".
     */
    fun setupMavenPublish(
        libraryModuleConfig: LibraryModuleConfig.Library,
        platform: Platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = SourcesJar.Sources()
        ),
        autoReleaseOnMavenCentral: (version: String) -> Boolean = { true },
        sign: Boolean = System.getenv("CI")?.toBoolean() == true,
        version: String = System.getenv("TAG") ?: "LOCAL-SNAPSHOT",
    ) {
        val module = libraryModuleConfig.libraryConfig.getModuleForProject(
            libraryModuleConfig.project.rootDir,
            libraryModuleConfig.project.projectDir
        )

        libraryModuleConfig.project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(platform)
            coordinates(
                groupId = libraryModuleConfig.libraryConfig.maven.groupId,
                artifactId = module.artifactId,
                version = version
            )

            pom {
                name.set(libraryModuleConfig.libraryConfig.library.name)
                description.set(module.libraryDescription(libraryModuleConfig.libraryConfig))
                inceptionYear.set(libraryModuleConfig.libraryConfig.library.release.toString())
                url.set(libraryModuleConfig.libraryConfig.library.getRepoLink(libraryModuleConfig.config.developer))

                licenses {
                    license {
                        name.set(libraryModuleConfig.libraryConfig.library.license.name)
                        url.set(
                            libraryModuleConfig.libraryConfig.library.license.getLink(
                                libraryModuleConfig.config.developer,
                                libraryModuleConfig.libraryConfig.library
                            )
                        )
                    }
                }

                developers {
                    developer {
                        id.set(libraryModuleConfig.config.developer.mavenId)
                        name.set(libraryModuleConfig.config.developer.name)
                        email.set(libraryModuleConfig.config.developer.mail)
                    }
                }

                scm {
                    url.set(
                        libraryModuleConfig.libraryConfig.library.getRepoLink(
                            libraryModuleConfig.config.developer
                        )
                    )
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
        libraryModuleConfig: LibraryModuleConfig.Library,
        androidConfig: AndroidLibraryConfig,
        buildConfig: Boolean,
    ) {
        libraryModuleConfig.project.extensions.configure(LibraryExtension::class.java) {

            namespace = libraryModuleConfig.namespace()

            this.compileSdk = androidConfig.compileSdk.get().toInt()

            buildFeatures {
                this.buildConfig = buildConfig
            }

            defaultConfig {
                this.minSdk = androidConfig.minSdk.get().toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(libraryModuleConfig.config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(libraryModuleConfig.config.javaVersion)
            }
        }
    }

    fun setupAndroidApp(
        appModuleConfig: AppModuleConfig,
        androidAppConfig: AndroidAppConfig,
        generateResAppName: Boolean,
        buildConfig: Boolean,
        checkDebugKeyStoreProperty: Boolean,
        setupBuildTypesDebugAndRelease: Boolean,
        versionFormatter: ChangelogVersionFormatter,
        buildTypeDebugSuffix: String = ".debug",
    ) {
        appModuleConfig.project.extensions.configure(ApplicationExtension::class.java) {

            namespace = appModuleConfig.appConfig.namespace

            this.compileSdk = androidAppConfig.compileSdk.get().toInt()

            buildFeatures {
                resValues = true // needed for app name as string resource
                this.buildConfig = buildConfig
            }

            defaultConfig {

                this.minSdk = androidAppConfig.minSdk.get().toInt()
                this.targetSdk = androidAppConfig.targetSdk.get().toInt()
                this.versionCode = versionFormatter.parseVersion(appModuleConfig.appConfig.versionName)
                this.versionName = appModuleConfig.appConfig.versionName

                if (generateResAppName) {
                    resValue(
                        type = "string",
                        name = androidAppConfig.stringResourceIdForAppName,
                        value = appModuleConfig.appConfig.name
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(appModuleConfig.config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(appModuleConfig.config.javaVersion)
            }

            // eventually use local custom signing
            if (checkDebugKeyStoreProperty) {
                val debugKeyStore =
                    appModuleConfig.project.providers.gradleProperty("debugKeyStore").orNull
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
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
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
        appModuleConfig: AppModuleConfig,
        application: JvmApplication,
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

                packageName = appModuleConfig.appConfig.name // entspricht dem exe Name
                packageVersion = appModuleConfig.appConfig.versionName
                description =
                    "${appModuleConfig.appConfig.name} - Build at ${now.format(formatter)}"
                copyright =
                    "©${now.year} ${appModuleConfig.config.developer.name}. All rights reserved."
                vendor = appModuleConfig.config.developer.name

                // https://github.com/JetBrains/compose-multiplatform/issues/1154
                // => suggestRuntimeModules task ausführen um zu prüfen, was man hier hinzufügen sollte
                // modules("java.instrument", "java.security.jgss", "java.sql", "java.xml.crypto", "jdk.unsupported")

                windows {
                    iconFile.set(appModuleConfig.project.file(desktopAppConfig.ico))
                    //includeAllModules = true
                }
            }
        }
    }

    fun registerLaunch4JTask(
        appModuleConfig: AppModuleConfig,
        desktopAppConfig: DesktopAppConfig,
        jarTask: String = "flattenReleaseJars",
        outputFile: (exe: File) -> File = { it },
    ) {
        appModuleConfig.project.tasks.register("launch4j", Launch4jLibraryTask::class.java) {
            setupLaunch4J(
                appModuleConfig = appModuleConfig,
                task = this,
                desktopAppConfig = desktopAppConfig,
                jarTask = jarTask,
                outputFile = outputFile
            )
        }
    }

    private fun setupLaunch4J(
        appModuleConfig: AppModuleConfig,
        task: Launch4jLibraryTask,
        desktopAppConfig: DesktopAppConfig,
        jarTask: String = "flattenReleaseJars",
        outputFile: (exe: File) -> File = { it },
    ) {
        with(task) {

            mainClassName.set(desktopAppConfig.mainClass)
            icon.set(project.file(desktopAppConfig.ico).absolutePath)
            setJarTask(project.tasks.getByName(jarTask))
            outfile.set("${appModuleConfig.appConfig.name}.exe")

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            productName.set(appModuleConfig.appConfig.name)
            version.set(appModuleConfig.appConfig.versionName)
            textVersion.set(appModuleConfig.appConfig.versionName)
            description = "${appModuleConfig.appConfig.name} - Build at ${now.format(formatter)}"
            copyright.set("©${now.year} ${appModuleConfig.config.developer.name}. All rights reserved.")
            companyName.set(appModuleConfig.config.developer.name)

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

    fun registerExtractProguardMapFromAABTask(
        appModuleConfig: AppModuleConfig,
        outputFolder: String = "${if (appModuleConfig.project.providers.gradleProperty("work").isPresent) "D:/dev" else "M:/dev"}/06 - retrace",
    ) {
        val project = appModuleConfig.project
        val appName = appModuleConfig.appConfig.name
        val appVersionName = appModuleConfig.appConfig.versionName
        afterEvaluate {
            project.tasks.named("bundleRelease").configure {
                finalizedBy("extractProguardMap")
            }
        }

        project.tasks.register("extractProguardMap") {
            doLast {
                extractProguardMapFromAAB(project, appName, appVersionName, outputFolder)
            }
        }
    }

    private fun extractProguardMapFromAAB(
        project: Project,
        appName: String,
        versionName: String,
        outputFolder: String,
    ) {
        with(project) {

            val outputDir = outputFolder

            //val dev = if (providers.gradleProperty("work").isPresent) "D:/dev" else "M:/dev"
            val projectName = project.name
            val aabFile = file("release/$projectName-release.aab")

            // Files / Paths
            val proguardMapRootZipPath = "BUNDLE-METADATA"
            val proguardMapZipPath =
                "$proguardMapRootZipPath/com.android.tools.build.obfuscation/proguard.map"
            val proguardMapOutput = File("$outputDir/$appName - Proguard $versionName.map")
            val proguardTmpFolder = File("$outputDir/$proguardMapRootZipPath")
            val proguardFile = File(proguardMapOutput.parentFile, proguardMapZipPath)

            // 1) das ProGuard-Map-File aus der aab extrahieren
            copy {
                copySpec {
                    from(zipTree(aabFile)) {
                        include(proguardMapZipPath)
                    }
                    into(proguardMapOutput.parentFile)
                }
            }

            // 2) das ProGuard-Map-File umbenennen
            if (proguardMapOutput.exists())
                proguardMapOutput.delete()
            val success = proguardFile.renameTo(proguardMapOutput)

            // 3) die alten Dateien löschen
            proguardTmpFolder.deleteRecursively()

            if (success) {
                println("ProGuard-Map-Datei wurde in ${proguardMapOutput.absolutePath} umbenannt.")
            } else {
                throw kotlin.Exception("ERROR - ProGuard-Map-Datei wurde NICHT in ${proguardMapOutput.absolutePath} umbenannt!")
            }
        }
    }
}