package com.michaelflisar.kmpdevtools.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
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

abstract class CopyIconConfig @Inject constructor(objects: ObjectFactory) {
    abstract val enabled: Property<Boolean>
    abstract val sourceModule: Property<String>
    abstract val sourceFile: Property<String>
    abstract val targetComposeFile: Property<String>
    abstract val targetIcoFile: Property<String>
    abstract val createComposeResource: Property<Boolean>
    abstract val createIco: Property<Boolean>

    init {
        enabled.convention(false)
        sourceModule.convention("app/app/android")
        sourceFile.convention("src/main/ic_launcher-playstore.png")
        targetComposeFile.convention("src/commonMain/composeResources/drawable/icon.png")
        targetIcoFile.convention("icon.ico")
        createComposeResource.convention(false)
        createIco.convention(false)
    }

    fun enableAll() {
        enabled.set(true)
        createComposeResource.set(true)
        createIco.set(true)
    }
}


@CacheableTask
abstract class CopyIconTask : DefaultTask() {

    @get:Input
    abstract val config: Property<CopyIconConfig>

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceInputFile: File
        get() = rootDirectory.get().file("${config.get().sourceModule.get()}/${config.get().sourceFile.get()}").asFile

    @get:OutputFile
    @get:Optional
    val composeOutputFile: File?
        get() = if (config.get().createComposeResource.get()) {
            layout.projectDirectory.file(config.get().targetComposeFile.get()).asFile
        } else {
            null
        }

    @get:OutputFile
    @get:Optional
    val icoOutputFile: File?
        get() = if (config.get().createIco.get()) {
            layout.projectDirectory.file(config.get().targetIcoFile.get()).asFile
        } else {
            null
        }

    @TaskAction
    fun copy() {
        val src = sourceInputFile.toPath()

        if (!Files.exists(src)) {
            throw GradleException("Source icon does not exist: $src")
        }

        if (config.get().createComposeResource.get()) {
            val tgt = composeOutputFile!!.toPath()
            Files.createDirectories(tgt.parent)
            Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
        }

        if (config.get().createIco.get()) {
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