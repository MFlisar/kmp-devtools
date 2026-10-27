package com.michaelflisar.kmpdevtools

object TaskUtil {

    fun readUserInput(prompt: String, forceNotEmpty: Boolean = true): String {
        println(prompt)
        val input = readlnOrNull()?.trim().orEmpty()
        if (input.isEmpty() && forceNotEmpty) {
            println("Aborted: Input empty!")
            throw RuntimeException("Input empty")
        }
        return input
    }

}