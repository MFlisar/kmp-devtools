package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigAppTest {

    @Test
    fun readsCurrentVersion() {
        val app = Yaml.default.decodeFromString(
            Config.App.serializer(),
            """
            name: Test
            namespace: com.example.test
            version:
              code: 2
              name: "2.0.0"
            module-paths:
              - demo/app
            """.trimIndent()
        )

        assertEquals("2.0.0", app.version.name)
        assertEquals(2, app.version.code)
    }

    @Test
    fun rejectsNonPositiveVersionCodes() {
        assertFailsWith<IllegalArgumentException> {
            app(Config.App.Version(code = 0, name = "1.0.0"))
        }
    }

    private fun app(version: Config.App.Version) = Config.App(
        name = "Test",
        namespace = "com.example.test",
        version = version,
        modulePaths = listOf("demo/app"),
    )
}
