package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigAppTest {

    @Test
    fun latestReleaseUsesHighestNumberRegardlessOfListOrder() {
        val app = Yaml.default.decodeFromString(
            Config.App.serializer(),
            """
            name: Test
            namespace: com.example.test
            releases:
              - name: "2.0.0"
                number: 2
              - name: "1.0.0"
                number: 1
            module-paths:
              - demo/app
            """.trimIndent()
        )

        assertEquals("2.0.0", app.latestRelease.name)
        assertEquals(2, app.latestRelease.number)
    }

    @Test
    fun rejectsAppsWithoutReleases() {
        assertFailsWith<IllegalArgumentException> { app(emptyList()) }
    }

    @Test
    fun rejectsNonPositiveReleaseNumbers() {
        assertFailsWith<IllegalArgumentException> {
            app(listOf(Config.App.Release(name = "1.0.0", number = 0)))
        }
    }

    @Test
    fun rejectsDuplicateReleaseNumbers() {
        assertFailsWith<IllegalArgumentException> {
            app(
                listOf(
                    Config.App.Release(name = "1.0.0", number = 1),
                    Config.App.Release(name = "2.0.0", number = 1),
                )
            )
        }
    }

    @Test
    fun serializesReleaseHistoryAsJson() {
        val releases = listOf(
            Config.App.Release(name = "1.0.0", number = 1),
            Config.App.Release(name = "2.0.0", number = 2),
        )

        assertEquals(
            """[{"name":"1.0.0","number":1},{"name":"2.0.0","number":2}]""",
            Json.encodeToString(releases)
        )
    }

    private fun app(releases: List<Config.App.Release>) = Config.App(
        name = "Test",
        namespace = "com.example.test",
        releases = releases,
        modulePaths = listOf("demo/app"),
    )
}
