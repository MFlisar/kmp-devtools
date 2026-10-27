package com.michaelflisar.kmpdevtools.tasks

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DownloadFilesTask(
    override val shortcut: String,
) : ITask {

    override val name = "downloadFiles"
    override val description =
        "Downloads files into the current project (github workflows, copilot skills, copilot actions)"

    override fun run(root: File) {

        // Settings
        val downloadRepoName = "https://github.com/MFlisar/kmp-devtools"
        val downloadRepoBranch = "main"

        val filesToDownloadNew = listOf<Download>(
            Download.Folder(
                name = ".github/workflows",
                sourceFolderPath = "files/workflows",
                targetFolderPath = ".github/workflows",
            ),
            Download.Folder(
                name = ".github/actions",
                sourceFolderPath = "files/actions",
                targetFolderPath = ".github/actions",
            ),
            Download.Folder(
                name = ".github/instructions",
                sourceFolderPath = "files/instructions",
                targetFolderPath = ".github/instructions",
            )
        )

        // Download files
        val repoPath = downloadRepoName
            .removePrefix("https://github.com/")
            .trimEnd('/')
        val rootPath = root.toPath().toAbsolutePath().normalize()
        val repositoryFiles = if (filesToDownloadNew.any { it is Download.Folder }) {
            getRepositoryFiles(repoPath, downloadRepoBranch)
        } else {
            emptyList()
        }

        filesToDownloadNew.forEach { download ->
            when (download) {
                is Download.Folder -> {
                    val sourceFolder = validateRelativePath(download.sourceFolderPath)
                    val targetFolder = validateRelativePath(download.targetFolderPath)
                    val sourcePrefix = "$sourceFolder/"
                    val files = repositoryFiles.filter { it.startsWith(sourcePrefix) }
                    if (files.isEmpty()) {
                        throw IOException("No files found in GitHub source folder: $sourceFolder")
                    }

                    files.forEach { sourceFile ->
                        val relativeFile = sourceFile.removePrefix(sourcePrefix)
                        val target = resolveTarget(rootPath, targetFolder, relativeFile)
                        downloadFile(
                            repoPath = repoPath,
                            branch = downloadRepoBranch,
                            sourcePath = sourceFile,
                            target = target,
                        )
                        println("Downloaded [${download.name}]: $sourceFile")
                    }
                }

                is Download.File -> {
                    val sourceFile = validateRelativePath(download.sourceFilePath)
                    val targetFolder = validateRelativePath(download.targetFolderPath)
                    val fileName = sourceFile.substringAfterLast('/')
                    val target = resolveTarget(rootPath, targetFolder, fileName)
                    downloadFile(
                        repoPath = repoPath,
                        branch = downloadRepoBranch,
                        sourcePath = sourceFile,
                        target = target,
                    )
                    println("Downloaded [${download.name}]: $sourceFile")
                }
            }
        }

    }

    private fun getRepositoryFiles(repoPath: String, branch: String): List<String> {
        val encodedBranch = encodePath(branch)
        val apiUrl =
            "https://api.github.com/repos/$repoPath/git/trees/$encodedBranch?recursive=1"
        val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "kmp-devtools")
            (System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN"))
                ?.takeIf { it.isNotBlank() }
                ?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Failed to list files in $repoPath: HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val tree = Json.parseToJsonElement(body).jsonObject
            if (tree["truncated"]?.jsonPrimitive?.booleanOrNull == true) {
                throw IOException("GitHub repository tree is truncated for $repoPath")
            }
            return tree["tree"]?.jsonArray?.mapNotNull { item ->
                val entry = item.jsonObject
                if (entry["type"]?.jsonPrimitive?.contentOrNull == "blob") {
                    entry["path"]?.jsonPrimitive?.contentOrNull
                } else {
                    null
                }
            } ?: throw IOException("GitHub returned no file tree for $repoPath")
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadFile(
        repoPath: String,
        branch: String,
        sourcePath: String,
        target: Path,
    ) {
        Files.createDirectories(target.parent)

        val sourceUrl =
            "https://raw.githubusercontent.com/$repoPath/${encodePath(branch)}/${encodePath(sourcePath)}"
        val connection = URI(sourceUrl).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "kmp-devtools")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Failed to download $sourcePath: HTTP $responseCode")
            }

            connection.inputStream.use { input ->
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveTarget(rootPath: Path, targetFolder: String, filePath: String) =
        rootPath.resolve("$targetFolder/$filePath").normalize().also { target ->
            require(target.startsWith(rootPath)) {
                "Download path must stay inside the project root: $targetFolder/$filePath"
            }
        }

    private fun validateRelativePath(path: String): String {
        require(path.isNotBlank() && !path.contains('\\')) {
            "Invalid repository-relative path: $path"
        }
        val segments = path.split('/')
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "Invalid repository-relative path: $path"
        }
        return path
    }

    private fun encodePath(path: String) = path.split('/').joinToString("/") {
        URLEncoder.encode(it, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

private sealed class Download {
    data class Folder(
        val name: String,
        val sourceFolderPath: String,
        val targetFolderPath: String,
    ) : Download()

    data class File(
        val name: String,
        val sourceFilePath: String,
        val targetFolderPath: String,
    ) : Download()
}
