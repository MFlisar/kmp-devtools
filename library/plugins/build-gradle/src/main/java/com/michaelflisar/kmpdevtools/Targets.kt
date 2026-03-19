package com.michaelflisar.kmpdevtools

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.michaelflisar.kmpdevtools.config.AppModuleData
import com.michaelflisar.kmpdevtools.config.LibraryModuleData
import com.michaelflisar.kmpdevtools.config.sub.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.config.sub.WasmAppConfig
import com.michaelflisar.kmpdevtools.core.Platform
import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

class Targets(
    val android: Boolean = false,
    val iOS: Boolean = false,
    val windows: Boolean = false,
    val linux: Boolean = false,
    val macOS: Boolean = false,
    val wasm: Boolean = false,
    val js: Boolean = false,
) {
    private val enabledPlatforms = Platform.entries
        .filter {
            when (it) {
                Platform.ANDROID -> android
                Platform.IOS -> iOS
                Platform.WINDOWS -> windows
                Platform.MACOS -> macOS
                Platform.LINUX -> linux
                Platform.WASM -> wasm
                Platform.JS -> js
            }
        }

    fun isEnabled(target: Platform) = enabledPlatforms.contains(target)

    fun getPlatforms(exclusions: List<Platform>): List<Platform> {
        return enabledPlatforms.filter { !exclusions.contains(it) }
    }

    fun setupDependencies(
        sourceSet: KotlinSourceSet,
        sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
        buildTargets: Targets,
        platforms: List<Platform>,
        platformsNotSupported: Boolean = false,
    ) {
        val targets = if (platformsNotSupported) {
            buildTargets.getPlatforms(exclusions = platforms)
        } else {
            platforms
        }
        targets.filter { buildTargets.isEnabled(it) }.forEach { target ->
            target.targets.forEach {
                sourceSets.getByName("${it}Main").dependsOn(sourceSet)
            }
        }
    }

    fun setupTargetsLibrary(
        libraryModuleData: LibraryModuleData,
        configureAndroid: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        if (libraryModuleData.androidConfig == null && android) {
            throw IllegalArgumentException("androidConfig must be provided when Android target is enabled")
        }
        if (libraryModuleData.androidConfig != null)
            setupAndroidLibraryTarget(
                libraryModuleData.project,
                libraryModuleData.config,
                libraryModuleData.libraryConfig,
                libraryModuleData.androidConfig,
                configureAndroid
            )
        setupIOSTarget(libraryModuleData.project, configureIOS, configureIOSTests)
        setupWindowsTarget(libraryModuleData.project, configureWindows)
        setupMacOSTarget(libraryModuleData.project, configureMacOS)
        setupLinuxTarget(libraryModuleData.project, configureLinux)
        setupWasmLibraryTarget(libraryModuleData.project, configureWASM)
        setupJSTarget(libraryModuleData.project, configureJS)
    }

    fun setupTargetsApp(
        appModuleData: AppModuleData,
        configureAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        if (appModuleData.wasmConfig == null && wasm) {
            throw IllegalArgumentException("wasmConfig must be provided when Wasm target is enabled")
        }
        setupAndroidAppTarget(appModuleData.project, appModuleData.config, configureAndroid)
        setupIOSTarget(appModuleData.project, configureIOS, configureIOSTests)
        setupWindowsTarget(appModuleData.project, configureWindows)
        setupMacOSTarget(appModuleData.project, configureMacOS)
        setupLinuxTarget(appModuleData.project, configureLinux)
        if (appModuleData.wasmConfig != null)
            setupWasmAppTarget(appModuleData.project, appModuleData.wasmConfig, configureWASM)
        setupJSTarget(appModuleData.project, configureJS)
    }

    /**
     * Config Android library target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param libraryConfig The library configuration to use for the Android target.
     * @param androidConfig The Android-specific configuration to use for the Android target.
     * @param configure A lambda to configure the Android target.
     */
    fun setupAndroidLibraryTarget(
        project: Project,
        config: Config,
        libraryConfig: LibraryConfig,
        androidConfig: AndroidLibraryConfig,
        configure: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {
                // TODO: geht dzt. nicht!
                //android {
                //    namespace = libraryConfig.library.namespace
                //    compileSdk = androidConfig.compileSdk.get().toInt()
                //    minSdk = androidConfig.minSdk.get().toInt()
//
                //    compilerOptions {
                //        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                //    }
//
                //    androidResources { enable = androidConfig.enableAndroidResources }
//
                //    configure()
                //}
            }
        }
    }

    /**
     * Config Android app target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param configure A lambda to configure the Android target.
     */
    fun setupAndroidAppTarget(
        project: Project,
        config: Config,
        configure: (KotlinAndroidTarget.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                    }
                    configure()
                }
            }
        }
    }

    /**
     * Config iOS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each iOS target.
     * @param configureTests A lambda to configure simulator tests for applicable iOS targets.
     */
    fun setupIOSTarget(
        project: Project,
        configure: (KotlinNativeTarget.() -> Unit),
        configureTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (iOS) {
                iosX64 {
                    configure()
                    configureTests()
                }
                iosArm64 {
                    configure()
                }
                iosSimulatorArm64 {
                    configure()
                    configureTests()
                }
            }
        }
    }

    fun setupXCFramework(
        project: Project,
        frameworkName: String,
        folderCInterop: File = project.file("iosXCFramework/cinterop"),
        folderXCFramework: File = project.file("iosXCFramework/${frameworkName}.xcframework"),
        relativeHeadersFolderInXCFramework: String = "Headers",
    ) {
        // helper function
        fun sliceDirFor(target: KonanTarget): String = when (target) {
            KonanTarget.IOS_ARM64 -> "ios-arm64"
            KonanTarget.IOS_X64 -> "ios-x86_64-simulator"
            KonanTarget.IOS_SIMULATOR_ARM64 -> "ios-arm64_x86_64-simulator"
            else -> error("Unsupported target: $target")
        }

        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (iOS) {


                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach { iosTarget ->

                    iosTarget.compilations.getByName("main") {

                        cinterops.create(frameworkName) {

                            // 1) add all .def files in the cinterop folder
                            val defFiles =
                                folderCInterop.listFiles { file: File -> file.extension == "def" }
                            defFiles.forEach { defFile(it) }

                            // 2) include header dirs (cinterop + provided Headers folder)
                            val sliceDir =
                                folderXCFramework.resolve(sliceDirFor(iosTarget.konanTarget))
                            includeDirs.allHeaders(
                                sliceDir.resolve("$frameworkName.framework/$relativeHeadersFolderInXCFramework"),
                                folderCInterop
                            )

                            // 3) compiler options
                            compilerOpts(
                                "-F", sliceDir.absolutePath,
                                "-framework", frameworkName
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Config Windows (JVM) target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure the Windows (JVM) target.
     */
    fun setupWindowsTarget(
        project: Project,
        configure: (KotlinJvmTarget.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (windows) {
                jvm {
                    configure()
                }
            }
        }
    }

    /**
     * Config macOS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each macOS target.
     */
    fun setupMacOSTarget(
        project: Project,
        configure: (KotlinNativeTargetWithHostTests.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (macOS) {
                macosX64 {
                    configure()
                }
                macosArm64 {
                    configure()
                }
            }
        }
    }

    /**
     * Config Linux targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each Linux target.
     */
    fun setupLinuxTarget(
        project: Project,
        configure: (KotlinNativeTarget.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (linux) {
                linuxX64 {
                    configure()
                }
                linuxArm64 {
                    configure()
                }
            }
        }
    }

    /**
     * Config Wasm app target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param wasmConfig The configuration to use for the Wasm target.
     * @param configure A lambda to configure the Wasm target.
     */
    fun setupWasmAppTarget(
        project: Project,
        wasmConfig: WasmAppConfig,
        configure: (KotlinWasmJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    outputModuleName.set(wasmConfig.moduleName)
                    val rootDirPath = project.rootDir.path
                    val projectDirPath = project.projectDir.path
                    browser {
                        commonWebpackConfig {
                            outputFileName = wasmConfig.outputFileName
                            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                                static = (static ?: mutableListOf()).apply {
                                    // Serve sources to debug inside browser
                                    add(rootDirPath)
                                    add(projectDirPath)
                                }
                            }
                        }
                    }
                    binaries.executable()

                    configure()
                }
            }
        }
    }

    fun setupWasmLibraryTarget(
        project: Project,
        configure: (KotlinWasmJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    nodejs()
                    configure()
                }
            }
        }
    }

    /**
     * Config JS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each JS target.
     */
    fun setupJSTarget(
        project: Project,
        configure: (KotlinJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (js) {
                js {
                    configure()
                }
                js(IR) {
                    configure()
                }
            }
        }
    }


}