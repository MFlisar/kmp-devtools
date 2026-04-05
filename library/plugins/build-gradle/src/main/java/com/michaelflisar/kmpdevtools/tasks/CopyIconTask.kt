package com.michaelflisar.kmpdevtools.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import javax.inject.Inject


@CacheableTask
abstract class CopyIconTask : DefaultTask() {

    @get:Input
    var sourceModule: String = "app/app/android"

    @get:Input
    var sourceFile: String =  "src/main/ic_launcher-playstore.png"

    @get:Input
    var targetModule: String = "app/app/shared"

    @get:Input
    var targetComposeFile: String = "src/commonMain/composeResources/drawable/ic_launcher.png"

    @get:Input
    var targetIcoFile: String = "ic_launcher.ico"

    @get:Input
    var createComposeResource: Boolean = false

    @get:Input
    var createIco: Boolean = false

    @get:Inject
    abstract val layout: ProjectLayout

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceInputFile: File
        get() = layout.projectDirectory.file("$sourceModule/$sourceFile").asFile

    @get:OutputFile
    @get:Optional
    val composeOutputFile: File?
        get() = if (createComposeResource) {
            layout.projectDirectory.file("$targetModule/$targetComposeFile").asFile
        } else {
            null
        }

    @get:OutputFile
    @get:Optional
    val icoOutputFile: File?
        get() = if (createIco) {
            layout.projectDirectory.file("$targetModule/$targetIcoFile").asFile
        } else {
            null
        }

    @TaskAction
    fun copy() {
        val src = sourceInputFile.toPath()

        if (!Files.exists(src)) {
            throw GradleException("Source icon does not exist: $src")
        }

        if (createComposeResource) {
            val tgt = composeOutputFile!!.toPath()
            Files.createDirectories(tgt.parent)
            Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
        }

        if (createIco) {
            val tgt = icoOutputFile!!.toPath()
            Files.createDirectories(tgt.parent)
            writeIcoFromSource(src, tgt, listOf(16, 32, 48, 64, 128, 256))
        }
    }

    private fun writeIcoFromSource(source: Path, target: Path, sizes: List<Int>) {
        val image = ImageIO.read(source.toFile())
            ?: throw GradleException("Unsupported source image format for ICO conversion: $source")

        val frames = sizes.distinct().sorted().map { size ->
            require(size in 1..256) { "ICO size must be between 1 and 256, was $size" }

            val scaled =
                java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g = scaled.createGraphics()
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY
            )
            g.drawImage(image, 0, 0, size, size, null)
            g.dispose()

            val pngBytes = ByteArrayOutputStream().use { baos ->
                if (!ImageIO.write(scaled, "png", baos)) {
                    throw GradleException("Could not encode PNG frame for ICO (size ${size}x$size)")
                }
                baos.toByteArray()
            }

            size to pngBytes
        }

        val iconDirSize = 6
        val entrySize = 16
        var dataOffset = iconDirSize + (entrySize * frames.size)

        val output = ByteArrayOutputStream()

        writeLeShort(output, 0) // reserved
        writeLeShort(output, 1) // type = icon
        writeLeShort(output, frames.size)

        frames.forEach { (size, png) ->
            output.write(if (size == 256) 0 else size)
            output.write(if (size == 256) 0 else size)
            output.write(0) // color count
            output.write(0) // reserved
            writeLeShort(output, 1) // color planes
            writeLeShort(output, 32) // bits per pixel
            writeLeInt(output, png.size)
            writeLeInt(output, dataOffset)
            dataOffset += png.size
        }

        frames.forEach { (_, png) -> output.write(png) }

        Files.write(target, output.toByteArray())
    }

    private fun writeLeShort(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun writeLeInt(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }
}