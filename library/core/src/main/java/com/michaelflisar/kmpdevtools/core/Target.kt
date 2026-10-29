package com.michaelflisar.kmpdevtools.core

enum class Target(
    val targetName: String,
    val targets: List<String>,
    val color: String,
) {
    ANDROID("android", listOf("android"), "3DDC84"),                  // Android Green
    IOS("ios", listOf("iosArm64", "iosSimulatorArm64"), "A2AAAD"),    // Apple Grey
    JVM("jvm",  listOf("jvm"), "5382A1"),                         // JVM Blue (official Java blue)
    MACOS("macos",listOf("macosArm64"), "B0B0B0"),                    // macOS Light Grey
    LINUX("linux", listOf("linuxX64", "linuxArm64"), "FF6600"),       // Linux Orange
    WASM("wasm", listOf("wasmJs"), "624DE7"),                         // WebAssembly Purple
    JS("js", listOf("js", "js(IR)"), "F7DF1E")                        // JavaScript Yellow
    ;

    companion object {

        /**
         * parser function names from the Targets class
         */
        fun parseParameterName(name: String): Target {
            return entries.find { it.targetName.equals(name, true) } ?:
                throw IllegalArgumentException("Unknown target name: $name")
        }


        /**
         * Targets: [JVM], [MACOS], [LINUX]
         *
         * not supported: [ANDROID], [IOS], [WASM], [JS]
         */
        val LIST_COMPUTER = listOf(JVM, MACOS, LINUX)

        /**
         * Targets: [IOS], [MACOS]
         *
         * not supported: [ANDROID], [JVM], [LINUX], [WASM], [JS]
         */
        val LIST_APPLE = listOf(IOS, MACOS)

        /**
         * Targets: [ANDROID], [IOS]
         *
         * not supported: [JVM], [MACOS], [LINUX], [WASM], [JS]
         */
        val LIST_MOBILE = listOf(ANDROID, IOS)

        /**
         * Targets: [ANDROID], [JVM], [IOS], [MACOS], [LINUX]
         *
         * not supported: [WASM], [JS]
         */
        val LIST_FILE_SUPPORT = listOf(ANDROID, JVM, IOS, MACOS, LINUX)

        /**
         * Targets: [JVM], [ANDROID]
         *
         * not supported: [MACOS], [LINUX], [IOS], [WASM], [JS]
         */
        val LIST_JAVA = listOf(JVM, ANDROID)
    }
}