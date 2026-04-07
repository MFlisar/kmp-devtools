package com.michaelflisar.kmpdevtools.core.configs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val name: String,
    @SerialName("namespace") val namespace: String,
    @SerialName("version-code") val versionCode: Int,
    @SerialName("version-name") val versionName: String,
) {
    companion object {

        const val fileName = "app-config.yml"

    }
}