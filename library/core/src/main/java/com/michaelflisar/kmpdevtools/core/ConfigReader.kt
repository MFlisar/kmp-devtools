package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.KSerializer
import java.io.File

internal object ConfigReader {

    inline fun <reified T> readFromProject(
        root: File,
        relativePath: String,
        serializer: KSerializer<T>,
    ) = read(root, relativePath, serializer)

    inline fun <reified T> read(
        root: File,
        relativePath: String,
        serializer: KSerializer<T>,
    ): T {
        return read(File(root, relativePath), serializer)
    }

    inline fun <reified T> read(file: File, serializer: KSerializer<T>): T {
        return try {
            tryRead(file, serializer)!!
        } catch (e: Exception) {
            e.printStackTrace()
            throw kotlin.RuntimeException(
                "Failed to read `${T::class.qualifiedName}` from path '${file.path}'",
                e
            )
        }
    }

    fun <T> tryRead(file: File, serializer: KSerializer<T>): T? {
        if (!file.exists()) {
            return null
        }
        val content = file.readText(Charsets.UTF_8)
        return Yaml.default.decodeFromString(serializer, content)
    }
}