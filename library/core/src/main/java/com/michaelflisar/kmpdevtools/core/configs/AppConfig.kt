package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.ConfigDefaults
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val name: String,
    @SerialName("namespace") val namespace: String,
    @SerialName("version-name") val versionName: String,
) {
    companion object : ConfigReader<AppConfig>(
        ConfigDefaults.FILE_APP_CONFIG,
        { AppConfig.serializer() }
    )


}