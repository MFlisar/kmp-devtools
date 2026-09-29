package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.configs.AppModuleConfig
import com.michaelflisar.kmpdevtools.configs.DesktopAppConfig
import edu.sc.seis.launch4j.tasks.Launch4jLibraryTask
import org.gradle.api.tasks.Sync
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Launch4J {

    sealed interface SingleConfig {
        val outputDirName: String
        val jarTask: String
    }

    sealed class Config {

        data class Thin(
            override val outputDirName: String = "launch4j-thin",
            override val jarTask: String = "proguardReleaseJars",
            val jarFolder: String = "build/compose/tmp/main-release/proguard",
            val mainJarFileName: String = "app-jvm.jar",
        ) : Config(), SingleConfig

        data class Fat(
            override val outputDirName: String = "launch4j-fat",
            override val jarTask: String = "flattenReleaseJars",
        ) : Config(), SingleConfig

        data class All(
            val thin: Thin = Thin(),
            val fat: Fat = Fat(),
        ) : Config()
    }

    data class OutputConfig(
        val directory: File? = null,
        val fileName: String? = null,
    )

    internal fun registerTask(
        appModuleConfig: AppModuleConfig,
        desktopAppConfig: DesktopAppConfig,
        config: SingleConfig,
        baseTaskName: String = "launch4j",
        outputConfig: OutputConfig = OutputConfig(),
        configure: Launch4jLibraryTask.() -> Unit = { }
    ) {

        val project = appModuleConfig.project

        val taskName =
            "$baseTaskName${config::class.simpleName}"

        val outputFileName =
            outputConfig.fileName
                ?: "${appModuleConfig.appConfig.name}.exe"

        val launch4jFolder =
            project.layout.buildDirectory.dir(config.outputDirName)

        val launch4jDirectory =
            launch4jFolder.get().asFile

        val outputDirectory =
            outputConfig.directory
                ?: launch4jDirectory

        val needsRelocation =
            outputDirectory.canonicalFile != launch4jDirectory.canonicalFile

        val launch4jTask = project.tasks.register(
            "${taskName}Generate",
            Launch4jLibraryTask::class.java
        ) {

            outputDir.set(config.outputDirName)

            dependsOn(config.jarTask)

            when (config) {

                is Config.Thin -> {

                    jarFiles.set(
                        project.files(
                            "${config.jarFolder}/${config.mainJarFileName}"
                        )
                    )

                    classpath.set(
                        project.provider {
                            project.fileTree(config.jarFolder) {
                                include("*.jar")
                                exclude(config.mainJarFileName)
                            }.files.map {
                                "lib/${it.name}"
                            }.toSet()
                        }
                    )
                }

                is Config.Fat -> {

                    setJarTask(
                        project.tasks.named(config.jarTask).get()
                    )
                }
            }

            // setupLaunch4J
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            mainClassName.set(desktopAppConfig.mainClass)
            icon.set(project.file(desktopAppConfig.ico).absolutePath)
            outfile.set(outputFileName)
            productName.set(appModuleConfig.appConfig.name)
            version.set(appModuleConfig.appConfig.versionName)
            textVersion.set(appModuleConfig.appConfig.versionName)
            companyName.set(appModuleConfig.config.developer.name)
            description = "${appModuleConfig.appConfig.name} - Build at ${now.format(formatter)}"
            copyright.set("©${now.year} ${appModuleConfig.config.developer.name}. All rights reserved.")
            jreMinVersion.set(appModuleConfig.config.javaVersion)

            configure()
        }

        val previousTask = when (config) {

            is Config.Thin -> {

                project.tasks.register(
                    "${taskName}SyncJars",
                    Sync::class.java
                ) {
                    dependsOn(launch4jTask, config.jarTask)

                    from(project.file(config.jarFolder)) {
                        include("*.jar")
                    }

                    into(
                        project.layout.buildDirectory.dir(
                            "${config.outputDirName}/lib"
                        )
                    )
                }
            }

            is Config.Fat -> {
                launch4jTask
            }
        }

        val finalTask = if (needsRelocation) {
            project.tasks.register(
                "${taskName}Relocate",
                Sync::class.java
            ) {
                dependsOn(previousTask)

                from(launch4jDirectory)
                into(outputDirectory)

                doLast {
                    report(
                        logger,
                        outputDirectory,
                        outputFileName
                    )
                }
            }
        } else {
            previousTask.configure {
                doLast {
                    report(
                        logger,
                        outputDirectory,
                        outputFileName
                    )
                }
            }
            previousTask
        }

        project.tasks.register(taskName) {
            group = "distribution"
            description = "Creates ${config::class.simpleName} Launch4J executable"
            dependsOn(finalTask)
        }
    }
}

private fun report(
    logger: org.gradle.api.logging.Logger,
    outputDirectory: File,
    outputFileName: String,
) {
    val outputFile = File(outputDirectory, outputFileName)

    logger.lifecycle(
        "Executable wurde in folgendem Ordner erstellt:"
    )

    logger.lifecycle(
        "file:///{}",
        outputFile.parentFile.absolutePath
            .replace(" ", "%20")
            .replace("\\", "/")
    )
}