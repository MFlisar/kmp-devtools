package com.michaelflisar.kmpdevtools

import com.android.build.api.dsl.LibraryExtension
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.codingfeline.buildkonfig.gradle.TargetConfigDsl
import com.michaelflisar.composechangelog.format.ChangelogVersionFormatter
import com.michaelflisar.composechangelog.format.DefaultVersionFormatter
import com.michaelflisar.kmpdevtools.configs.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.configs.AppModuleConfig
import com.michaelflisar.kmpdevtools.configs.LibraryModuleConfig
import com.michaelflisar.kmpdevtools.core.Target
import com.michaelflisar.kmpdevtools.core.Config
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.text.toInt

class SourceSetPlatformDsl internal constructor(
    private val buildTargets: Targets,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    private val dependencies = mutableMapOf<Target, MutableSet<KotlinSourceSet>>()
    private val additionalSourceSets = mutableMapOf<Target, MutableSet<KotlinSourceSet>>()

    infix fun Target.addSourceSet(sourceSet: KotlinSourceSet) {
        if (buildTargets.isEnabled(this)) {
            additionalSourceSets.getOrPut(this) { mutableSetOf() }.add(sourceSet)
        }
    }

    infix fun List<Target>.addSourceSet(sourceSet: KotlinSourceSet) {
        forEach {
            it.addSourceSet(sourceSet)
        }
    }

    infix fun KotlinSourceSet.supportedBy(targets: List<Target>) {
        targets.forEach { platform ->
            if (buildTargets.isEnabled(platform))
                dependencies.getOrPut(platform) { mutableSetOf() }.add(this)
        }
    }

    infix fun KotlinSourceSet.supportedBy(target: Target) = supportedBy(listOf(target))

    operator fun List<Target>.not(): List<Target> {
        return buildTargets.targetsList.filter { !this.contains(it) }
    }

    operator fun Target.not() = !listOf(this)

    internal fun setupDependencies(log: Boolean) {

        buildTargets.targetsList.forEach { platform ->

            if (log)
                println("Dependencies for platform ${platform.name}:")

            // alle source sets der platform suchen
            val defaultPlatformSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customPlatformSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            val allPlatformSourceSets = defaultPlatformSourceSets + customPlatformSourceSets

            // alle dependencies der platform suchen
            val platformDependencies = dependencies[platform].orEmpty()

            // 1) alle default source sets der platform von den custom source sets der platform abhängig machen
            // bspw. [iosX64Main, iosArm64Main, iosSimulatorArm64Main] sollten von [iosMain] abhängig sein
            if (log)
                println("- custom source sets: ${customPlatformSourceSets.joinToStringOrEmpty { it.name }}")
            defaultPlatformSourceSets.forEach { defaultPlatformSourceSet ->
                customPlatformSourceSets.forEach { customPlatformSourceSet ->
                    defaultPlatformSourceSet.dependsOn(customPlatformSourceSet)
                }
            }

            // 2) alle source sets der platform mit allen dependencies verbinden
            if (log)
                println("- dependency source sets: ${platformDependencies.joinToStringOrEmpty { it.name }}")
            allPlatformSourceSets.forEach { sourceSet ->
                platformDependencies.forEach { dependency ->
                    sourceSet.dependsOn(dependency)
                }
            }
        }
    }

    private fun calcAllDefaultPlatformsForSourceSet(target: Target): Set<KotlinSourceSet> {
        return target.targets.map { t ->
            val name = "${t}Main"
            sourceSets.findByName(name)
                ?: throw IllegalArgumentException("Source set $name not found for platform ${target.name}")
        }.toSet()
    }

    private fun calcAllCustomPlatformsForSourceSet(target: Target): Set<KotlinSourceSet> {
        return additionalSourceSets[target].orEmpty()
    }

    private fun <T> Iterable<T>.joinToStringOrEmpty(transform: (T) -> String): String {
        val empty = !this.iterator().hasNext()
        if (empty)
            return "-"
        val info = this.joinToString(transform = transform)
        return "[$info]"
    }

    fun printDependencies() {
        println("")

        // 1) Pro Platform alle source sets (default + custom) ausgeben
        println("Source sets per platform:")
        buildTargets.targetsList.forEach { platform ->
            val defaultSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            println("- ${platform.name}: ${defaultSourceSets.joinToStringOrEmpty { it.name }} (custom: ${customSourceSets.joinToStringOrEmpty { it.name }})")
        }

        // 2) Pro source set alle platforms ausgeben, die von der source set unterstützt werden
        println("")
        println("Platforms per source set:")
        val allSourceSets = buildTargets.targetsList.map { platform ->
            val defaultSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            defaultSourceSets + customSourceSets
        }.flatten().distinct()
        allSourceSets.forEach { sourceSet ->
            val supportedPlatforms = buildTargets.targetsList.filter { platform ->
                val defaultSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
                val customSourceSets = calcAllCustomPlatformsForSourceSet(platform)
                (defaultSourceSets + customSourceSets).contains(sourceSet)
            }
            println("- ${sourceSet.name} is supported by platforms: ${supportedPlatforms.joinToStringOrEmpty { it.name }}")
        }

        println("")
    }
}

fun setupDependencies(
    module: AppModuleConfig,
    buildTargets: Targets,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    block: SourceSetPlatformDsl.() -> Unit,
) {
    setupDependencies(
        config = module.config,
        buildTargets = buildTargets,
        sourceSets = sourceSets,
        block = block
    )
}

fun setupDependencies(
    module: LibraryModuleConfig,
    buildTargets: Targets,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    block: SourceSetPlatformDsl.() -> Unit,
) {
    setupDependencies(
        config = module.config,
        buildTargets = buildTargets,
        sourceSets = sourceSets,
        block = block
    )
}

private fun setupDependencies(
    config: Config,
    buildTargets: Targets,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    block: SourceSetPlatformDsl.() -> Unit,
) {
    val log = config.settings.logDependencies
    val dsl = SourceSetPlatformDsl(buildTargets, sourceSets)
    with(dsl, block)
    // wenn wir alle source sets zu platform zuordnungen haben, können wir die dependencies setzen
    if (log)
        dsl.printDependencies()
    dsl.setupDependencies(log)
}

fun BuildKonfigExtension.isDebug(project: Project): Boolean {
    return !project.gradle.startParameter.taskNames.any {
        it.contains("release", true) ||
                it.contains("launch4j", ignoreCase = true)
    }
}

fun BuildKonfigExtension.isExe(project: Project): Boolean {
    return project.gradle.startParameter.taskNames.any { it.contains("publishRelease", true) || it.contains("launch4j", ignoreCase = true) }
}

fun BuildKonfigExtension.setupBuildKonfig(
    project: Project,
    config: Config,
    versionFormatter: ChangelogVersionFormatter? = DefaultVersionFormatter(DefaultVersionFormatter.Format.MajorMinorPatch),
    exposeObjectWithName: String = "BuildKonfig",
    isDebug: Boolean? = null,
    isExe: Boolean? = null,
    configure: TargetConfigDsl.() -> Unit = {},
) {
    val app = config.requireApp(project)
    packageName.set(app.namespace)
    this.exposeObjectWithName.set(exposeObjectWithName)
    defaultConfigs {
        buildConfigField(Type.STRING, "versionName", app.versionName)
        if (versionFormatter != null)
            buildConfigField(
                Type.INT,
                "versionCode",
                versionFormatter.parseVersion(app.versionName).toString()
            )
        buildConfigField(Type.STRING, "namespace", app.namespace)
        buildConfigField(Type.STRING, "appName", app.name)
        if (isDebug != null)
            buildConfigField(Type.BOOLEAN, "DEBUG", isDebug.toString())
        if (isExe != null)
            buildConfigField(Type.BOOLEAN, "EXE", isExe.toString())
        configure()
    }
}

fun LibraryExtension.setupAndroid(
    libraryModuleConfig: LibraryModuleConfig,
    androidConfig: AndroidLibraryConfig,
    compose: Boolean = true,
    buildConfig: Boolean = true,
    useSupportLibrary: Boolean = true,
    lintAbortOnError: Boolean = false,
    releaseIsMinifyEnabled: Boolean = false,
    configure: LibraryExtension.() -> Unit = {}
) {
    val project = libraryModuleConfig.project
    val config = libraryModuleConfig.config

    namespace = androidConfig.namespace
    compileSdk = androidConfig.compileSdk.get().toInt()

    buildFeatures {
        this.compose = compose
        this.buildConfig = buildConfig
    }

    defaultConfig {
        minSdk = androidConfig.minSdk.get().toInt()
        vectorDrawables {
            this.useSupportLibrary = useSupportLibrary
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = releaseIsMinifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
        targetCompatibility = JavaVersion.toVersion(config.javaVersion)
    }

    lint {
        abortOnError = lintAbortOnError
    }

    configure()
}