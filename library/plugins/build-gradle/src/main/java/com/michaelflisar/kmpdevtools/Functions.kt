package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.Platform
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private sealed class Dependency {
    data class SourceSetDependency(val sourceSet: KotlinSourceSet, val platform: Platform) :
        Dependency()

    data class Custom(val sourceSet: KotlinSourceSet) : Dependency()
}

class SourceSetPlatformDsl internal constructor(
    private val buildTargets: Targets,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    private val dependencies = mutableMapOf<Platform, MutableSet<KotlinSourceSet>>()
    private val additionalSourceSets = mutableMapOf<Platform, MutableSet<KotlinSourceSet>>()

    infix fun Platform.addSourceSet(sourceSet: KotlinSourceSet) {
        if (buildTargets.isEnabled(this)) {
            additionalSourceSets.getOrPut(this) { mutableSetOf() }.add(sourceSet)
        }
    }

    infix fun KotlinSourceSet.supportedBy(platforms: List<Platform>) {
        platforms.forEach { platform ->
            if (buildTargets.isEnabled(platform))
                dependencies.getOrPut(platform) { mutableSetOf() }.add(this)
        }
    }

    infix fun KotlinSourceSet.supportedBy(platform: Platform) = supportedBy(listOf(platform))

    operator fun List<Platform>.not(): List<Platform> {
        return buildTargets.getPlatforms(this)
    }

    operator fun Platform.not() = !listOf(this)

    internal fun setupDependencies(log: Boolean) {

        buildTargets.platforms.forEach { platform ->

            if (log)
                println("Dependencies for platform ${platform.name}:")

            // alle source sets der platform suchen
            val defaultPlatformSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customPlatformSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            val allPlatformSourceSets = defaultPlatformSourceSets + customPlatformSourceSets

            // alle dependencies der platform suchen
            val platformDependencies = dependencies[platform].orEmpty()

            // alle default source sets der platform von den custom source sets der platform abhängig machen
            if (log)
                println("- ${defaultPlatformSourceSets.joinToStringOrEmpty { it.name }} has following custom source sets: ${customPlatformSourceSets.joinToStringOrEmpty { it.name }}")
            defaultPlatformSourceSets.forEach { defaultPlatformSourceSet ->
                customPlatformSourceSets.forEach { customPlatformSourceSet ->
                    customPlatformSourceSet.dependsOn(defaultPlatformSourceSet)
                }
            }

            // alle source sets der platform mit allen dependencies verbinden
            if (log)
                println("- ${allPlatformSourceSets.joinToStringOrEmpty { it.name }} depend on following source sets: ${platformDependencies.joinToStringOrEmpty { it.name }}")
            allPlatformSourceSets.forEach { sourceSet ->
                platformDependencies.forEach { dependency ->
                    sourceSet.dependsOn(dependency)
                }
            }
        }
    }

    private fun calcAllDefaultPlatformsForSourceSet(platform: Platform): Set<KotlinSourceSet> {
        return platform.targets.map { target ->
            val name = "${target}Main"
            sourceSets.findByName(name)
                ?: throw IllegalArgumentException("Source set $name not found for platform ${platform.name}")
        }.toSet()
    }

    private fun calcAllCustomPlatformsForSourceSet(platform: Platform): Set<KotlinSourceSet> {
        return additionalSourceSets[platform].orEmpty()
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
        buildTargets.platforms.forEach { platform ->
            val defaultSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            println("- ${platform.name}: ${defaultSourceSets.joinToStringOrEmpty { it.name }} (custom: ${customSourceSets.joinToStringOrEmpty { it.name }})")
        }

        // 2) Pro source set alle platforms ausgeben, die von der source set unterstützt werden
        println("")
        println("Platforms per source set:")
        val allSourceSets = buildTargets.platforms.map { platform ->
            val defaultSourceSets = calcAllDefaultPlatformsForSourceSet(platform)
            val customSourceSets = calcAllCustomPlatformsForSourceSet(platform)
            defaultSourceSets + customSourceSets
        }.flatten().distinct()
        allSourceSets.forEach { sourceSet ->
            val supportedPlatforms = buildTargets.platforms.filter { platform ->
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
    buildTargets: Targets,
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    log: Boolean = true,
    block: SourceSetPlatformDsl.() -> Unit,
) {
    val dsl = SourceSetPlatformDsl(buildTargets, sourceSets)
    with(dsl, block)
    // wenn wir alle source sets zu platform zuordnungen haben, können wir die dependencies setzen
    if (log)
        dsl.printDependencies()
    dsl.setupDependencies(log)
}