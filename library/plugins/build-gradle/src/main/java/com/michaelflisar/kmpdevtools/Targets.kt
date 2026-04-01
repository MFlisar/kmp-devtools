package com.michaelflisar.kmpdevtools

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.michaelflisar.kmpdevtools.configs.app.AndroidAppConfig
import com.michaelflisar.kmpdevtools.configs.app.WasmAppConfig
import com.michaelflisar.kmpdevtools.configs.library.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.core.Platform
import com.michaelflisar.kmpdevtools.core.configs.AppConfig
import com.michaelflisar.kmpdevtools.core.configs.Config
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
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
    val platforms = Platform.entries
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

    fun isEnabled(target: Platform) = platforms.contains(target)

    fun setupTargetsLibrary(
        project: Project,
        //configureAndroid: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        //if (libraryModuleData.androidConfig == null && android) {
        //    throw IllegalArgumentException("androidConfig must be provided when Android target is enabled")
        //}
        //if (libraryModuleData.androidConfig != null)
        //    setupAndroidLibraryTarget(
        //        libraryModuleData.project,
        //        libraryModuleData.config,
        //        libraryModuleData.libraryConfig,
        //        libraryModuleData.androidConfig,
        //        configureAndroid
        //    )
        setupIOSTarget(project, configureIOS, configureIOSTests)
        setupWindowsTarget(project, configureWindows)
        setupMacOSTarget(project, configureMacOS)
        setupLinuxTarget(project, configureLinux)
        setupWasmLibraryTarget(project, configureWASM)
        setupJSTarget(project, configureJS)
    }

    fun setupTargetsAndroidLibrary(
        project: Project,
        config: Config,
        androidConfig: AndroidLibraryConfig,
        androidTarget: KotlinMultiplatformAndroidLibraryTarget,
    ) {
        setupAndroidLibraryTarget(
            androidTarget,
            project,
            config,
            androidConfig,
            {}//configureAndroid
        )
    }

    fun setupTargetsApp(
        project: Project,
        //configureAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        wasmAppConfig: WasmAppConfig? = null,
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        if (wasmAppConfig == null && wasm) {
            throw IllegalArgumentException("wasmConfig must be provided when Wasm target is enabled")
        }
        //setupAndroidAppTarget(appModuleData.project, appModuleData.config, configureAndroid)
        setupIOSTarget(project, configureIOS, configureIOSTests)
        setupWindowsTarget(project, configureWindows)
        setupMacOSTarget(project, configureMacOS)
        setupLinuxTarget(project, configureLinux)
        if (wasmAppConfig != null)
            setupWasmAppTarget(project, wasmAppConfig, configureWASM)
        setupJSTarget(project, configureJS)
    }

    /**
     * Config Android library target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param moduleConfig The module configuration to use for the Android target.
     * @param androidConfig The Android-specific configuration to use for the Android target.
     * @param configure A lambda to configure the Android target.
     */
    private fun setupAndroidLibraryTarget(
        target: KotlinMultiplatformAndroidLibraryTarget,
        project: Project,
        config: Config,
        androidConfig: AndroidLibraryConfig,
        configure: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
    ) {
        if (android) {

            project.extensions.configure(KotlinMultiplatformExtension::class.java) {

                with(target) {
                    namespace = androidConfig.namespace
                    compileSdk = androidConfig.compileSdk.get().toInt()
                    minSdk = androidConfig.minSdk.get().toInt()

                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                    }

                    androidResources { enable = androidConfig.enableAndroidResources }

                    configure()
                }

                // possible extension classes:
                // com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
                // com.android.build.api.dsl.LibraryExtension
                // org.jetbrains.compose.android.AndroidExtension

                //project.extensions.configure(KotlinMultiplatformExtension::class.java) {
                //    val android = project.extensions.getByType(com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension::class.java)
                //    with(android)
                //    {
                //        namespace = libraryConfig.library.namespace + "." + androidConfig.namespaceAddon
                //        compileSdk = androidConfig.compileSdk.get().toInt()
                //        minSdk = androidConfig.minSdk.get().toInt()
//
                //        compilerOptions {
                //            jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                //        }
//
                //        androidResources { enable = androidConfig.enableAndroidResources }
//
                //        configure()
                //    }
                //}

                //if (isKotlinAndroidPluginApplied(project)) {
                //    project.extensions.configure<CommonExtension>("android") {
//
                //    }
                //}
//
                //android {
                //}
            }
        }
    }

    /**
     * Config Android app target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param appConfig The app configuration to use for the Android target.
     * @param configure A lambda to configure the Android target.
     */
    private fun setupAndroidAppTarget(
        extension: ApplicationExtension,
        project: Project,
        config: Config,
        androidAppConfig: AndroidAppConfig,
        appConfig: AppConfig,
        compose: Boolean = true,
        configureDefault: (DefaultConfig.() -> Unit) = {},
        configureBuildFeature: (BuildFeatures.() -> Unit) = {},
        configure: (ApplicationExtension.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {
                with(extension)
                {
                    namespace = appConfig.packageName
                    compileSdk = androidAppConfig.compileSdk.get().toInt()

                    defaultConfig {
                        applicationId = appConfig.androidAppId
                        minSdk = androidAppConfig.minSdk.get().toInt()
                        targetSdk = androidAppConfig.targetSdk.get().toInt()
                        versionCode = appConfig.versionCode
                        versionName = appConfig.versionName
                        configureDefault()
                    }

                    compileOptions {
                        sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
                        targetCompatibility = JavaVersion.toVersion(config.javaVersion)
                    }

                    buildFeatures {
                        this.compose = compose
                        configureBuildFeature()
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
    private fun setupIOSTarget(
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
    private fun setupWindowsTarget(
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
    private fun setupMacOSTarget(
        project: Project,
        configure: (KotlinNativeTargetWithHostTests.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (macOS) {
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
    private fun setupLinuxTarget(
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

    private fun setupWasmLibraryTarget(
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
    private fun setupJSTarget(
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