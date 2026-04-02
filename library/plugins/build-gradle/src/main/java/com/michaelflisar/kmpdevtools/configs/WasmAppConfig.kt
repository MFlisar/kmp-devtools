package com.michaelflisar.kmpdevtools.configs

class WasmAppConfig(
    val moduleName: String = DEFAULT_WASM_MODULE_NAME,
    val outputFileName: String = DEFAULT_WASM_OUTPUT_FILENAME,
) {
    companion object {
        const val DEFAULT_WASM_MODULE_NAME: String = "app"
        const val DEFAULT_WASM_OUTPUT_FILENAME: String = "app.js"
    }

}