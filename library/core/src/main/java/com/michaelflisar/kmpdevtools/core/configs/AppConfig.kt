package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.BaseConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val name: String,
    @SerialName("namespace") val namespace: String,
    @SerialName("version-code") val versionCode: Int,
    @SerialName("version-name") val versionName: String,
) : BaseConfig() {
    companion object : BaseConfigCompanion<AppConfig>("app-config.yml", AppConfig.serializer())
}