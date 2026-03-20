package com.michaelflisar.kmpdevtools.configs.app

import org.gradle.api.provider.Provider

class AndroidAppConfig(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val targetSdk: Provider<String>,
    val stringResourceIdForAppName: String = "app_name",
)