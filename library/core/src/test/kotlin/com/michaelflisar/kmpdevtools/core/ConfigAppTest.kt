package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigAppTest {

    @Test
    fun readsCurrentRelease() {
        val app = Yaml.default.decodeFromString(
            Config.App.serializer(),
            """
            name: Test
            namespace: com.example.test
            release:
              name: "2.0.0"
              number: 2
            module-paths:
              - demo/app
            """.trimIndent()
        )

        assertEquals("2.0.0", app.release.name)
        assertEquals(2, app.release.number)
    }

    @Test
    fun rejectsNonPositiveReleaseNumbers() {
        assertFailsWith<IllegalArgumentException> {
            app(Config.App.Release(name = "1.0.0", number = 0))
        }
    }

    private fun app(release: Config.App.Release) = Config.App(
        name = "Test",
        namespace = "com.example.test",
        release = release,
        modulePaths = listOf("demo/app"),
    )
}
