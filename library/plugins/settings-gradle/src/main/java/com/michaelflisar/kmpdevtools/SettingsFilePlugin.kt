package com.michaelflisar.kmpdevtools

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsFilePlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings
    }
}