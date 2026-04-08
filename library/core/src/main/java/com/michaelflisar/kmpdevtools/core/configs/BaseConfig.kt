package com.michaelflisar.kmpdevtools.core.configs

import com.charleskorn.kaml.Yaml
import com.michaelflisar.kmpdevtools.core.ConfigDefaults
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import java.io.File


abstract class ConfigReader<T>(
    val fileName: String,
    val serializer: () -> KSerializer<T>,
) {
    fun read(root: File): T {
        return ConfigReaderUtil.read(
            root = root,
            relativePath = "${ConfigDefaults.DEFAULT_FOLDER}/$fileName",
            serializer = serializer()
        )
    }

    fun tryRead(root: File): T? {
        return ConfigReaderUtil.tryRead(
            file = File(root, "${ConfigDefaults.DEFAULT_FOLDER}/$fileName"),
            serializer = serializer()
        )
    }

}


internal object ConfigReaderUtil {

    fun <T> readFromProject(
        root: File,
        relativePath: String,
        serializer: KSerializer<T>,
    ) = read(root, relativePath, serializer)

    fun <T> tryReadFromProject(
        root: File,
        relativePath: String,
        serializer: KSerializer<T>,
    ) = tryRead(File(root, relativePath), serializer)

    fun <T> read(
        root: File,
        relativePath: String,
        serializer: KSerializer<T>,
    ): T {
        return read(File(root, relativePath), serializer)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun <T> read(file: File, serializer: KSerializer<T>): T {
        return try {
            tryRead(file, serializer)!!
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(
                "Failed to read from path '${file.path}' with serializer '${serializer.descriptor.serialName}'! See stacktrace for details.",
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