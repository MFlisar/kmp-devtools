package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.core.Platform
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * usage:
 *
 * dependencySubstitution {
 *    if (projectPlugin.checkGradleProperty("useLocalToolbox") == true) {
 *         substitute(deps.toolbox.core, ":toolbox:core")
 *         substitute(deps.toolbox.app, ":toolbox:app")
 *         ...
 *     }
 * }
 *
 * @param lib the library to substitute, e.g. deps.toolbox.core
 * @param module the module to use instead, e.g. ":toolbox:core"
 */
fun DependencySubstitutions.substitute(
    lib: Provider<MinimalExternalModuleDependency>,
    module: String,
) {
    val dep = lib.get()
    val notation = "${dep.module.group}:${dep.module.name}"
    //println("substitute: $notation => $module")
    substitute(module(notation)).using(project(module))
}

fun Project.dependencySubstitution(
    block: DependencySubstitutions.() -> Unit
) {
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                block()
            }
        }
    }
    subprojects {
        configurations.configureEach {
            resolutionStrategy {
                dependencySubstitution {
                    block()
                }
            }
        }
    }
}