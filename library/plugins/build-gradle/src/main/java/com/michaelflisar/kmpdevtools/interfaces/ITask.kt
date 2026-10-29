package com.michaelflisar.kmpdevtools.interfaces

import java.io.File

interface ITask {

    val name: String
    val description: String

    val shortcut: String

    fun run(root: File)
}