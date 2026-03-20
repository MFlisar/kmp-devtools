package com.michaelflisar.kmpdevtools.core.configs

import org.gradle.api.plugins.ExtraPropertiesExtension

class AppConfig(
    val appName: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val androidAppId: String = packageName,
    val androidNamespace: String = packageName,
) {
    companion object {
        val KEY = "app-config"

        fun read(extra: ExtraPropertiesExtension) = extra[KEY] as AppConfig
    }

    fun save(extra: ExtraPropertiesExtension) {
        extra[KEY] = this
    }
}