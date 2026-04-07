package com.michaelflisar.kmpdevtools.readme

import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.readme.classes.CustomMarkdownFile
import com.michaelflisar.kmpdevtools.readme.classes.FolderLink
import com.michaelflisar.kmpdevtools.readme.classes.Partial
import com.michaelflisar.kmpdevtools.readme.classes.Placeholder
import java.io.File

object UpdateReadmeUtil {

    /**
     * Updates the README.md file in the given root directory based on the provided configuration and template.
     */
    fun update(
        rootDir: File,
        config: Config,
        libraryConfig: LibraryConfig,
        readmeTemplate: String = ReadmeDefaults.DefaultReadmeTemplate,
        folderModules: String = ReadmeDefaults.FOLDER_MODULES,
        folderScreenshots: String = ReadmeDefaults.FOLDER_SCREENSHOTS,
        hasApiDocs: Boolean = ReadmeDefaults.HAS_API_DOCS,
    ) {
        println("")
        println("################################")
        println("### BEGIN Updating README.md ###")
        println("################################")
        println("")

        val modules = libraryConfig.modules.filter { it.artifactId.isNotEmpty() }
        val pluginModules = modules.filter { it.plugin }
        val nonPluginModules = modules.filter { !it.plugin }

        // files
        val fileAppVersionToml = File(rootDir, "gradle/app.versions.toml")
        val fileLibsVersionToml = File(rootDir, "gradle/libs.versions.toml")
        val fileReadme = File(rootDir, "README.md")
        val folderDocumentation = File(rootDir, "documentation")
        val folderDocumentationModules = File(rootDir, "documentation/$folderModules")
        val folderDocumentationScreenshots = File(rootDir, "documentation/$folderScreenshots")

        // load data from project files
        val minSdk = readTOMLProperty(fileAppVersionToml, "versions", "minSdk").toInt()
        val supportedPlatforms = modules.map { module ->
            val platforms =
                getSupportedPlatformsFromModule(
                    File(
                        rootDir,
                        "${module.path}/build.gradle.kts"
                    )
                )
            module to platforms
        }
        val allSupportedPlatforms = supportedPlatforms.map { it.second }.flatten().distinct()
        val allSupportedPlatformsLowercase = allSupportedPlatforms.map { it.lowercase() }

        val isAndroidSupported = allSupportedPlatformsLowercase.contains("android")
        val isIosSupported = allSupportedPlatformsLowercase.contains("ios")
        val isWindowsSupported = allSupportedPlatformsLowercase.contains("windows")
        val isMacOsSupported = allSupportedPlatformsLowercase.contains("macos")
        val isLinuxSupported = allSupportedPlatformsLowercase.contains("linux")
        val isWasmSupported = allSupportedPlatformsLowercase.contains("wasm")
        val isJsSupported = allSupportedPlatformsLowercase.contains("js")

        // 1) load all markdown files from the documentation folder (excluding _partials)
        val markdownTitleRegex = Regex("^#\\s+(.*)$", RegexOption.MULTILINE)
        //val pathDocumentation = "documentation"
        //val pathModules = "documentation/modules"
        val markdownFilesWithName = folderDocumentation
            .walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .filter { !it.path.contains("_partials") }
            .toList()
            .sortedBy { it.path }
            .map {
                val content = it.readText()
                // extract name from first markdown header
                val nameMatch = markdownTitleRegex.find(content)
                val name = nameMatch?.groups?.get(1)?.value?.trim() ?: it.nameWithoutExtension
                CustomMarkdownFile(it, name)
            }

        // 2) get all modules and other markdown files als hierarchische Links
        val moduleLinks = markdownFilesWithName
            .filter { it.startsWithIgnoreCase(folderDocumentationModules) }
            .map {
                val relativePath = it.relativePathTo(rootDir)
                val encodedRelativePath = encodeRelativeLink(relativePath)
                "- [${it.name}]($encodedRelativePath)"
            }
        val otherLinks = buildMarkdownLinks(
            buildFolderLinkHierarchy(
                markdownFilesWithName.filter {
                    !it.startsWithIgnoreCase(
                        folderDocumentationModules
                    )
                },
                rootDir,
                folderDocumentation
            )
        )

        val ignoreExperimentalAnnoation = { annotation: String ->
            (annotation.startsWith("com.michaelflisar") && annotation.contains("InternalApi")) ||
                    // kein namespace, dh. es ist intern im gleichen Ordner
                    annotation == "InternalApi"
        }

        var experimentalAnnotations = listOf<ExperimentalAnnotationInfo>()
        modules
            .map { module -> File(rootDir, module.path) }
            .map { it.walkTopDown() }
            .flatMap { it }
            .filter { it.isFile && it.extension in listOf("kt") }
            .forEach {
                val lines = it.readLines()
                val allImports = lines
                    .filter { it.trim().startsWith("import ") }
                    .map { it.trim().removePrefix("import ").trim() }

                var experimentalAnnotationsInFile = listOf<ExperimentalAnnotationInfo>()

                lines
                    .forEach {

                        val line = it.trim()

                        // 1) OptIns suchen
                        val optIns = if (line.startsWith("@OptIn(")) {
                            val optIns = line.removePrefix("@OptIn(").removeSuffix(")").trim()
                            optIns.removePrefix("[").removeSuffix("]").split(",")
                                .map { it.trim() }
                        } else {
                            emptyList()
                        }

                        // 2) @Experimental suchen
                        val experimental = if (line.startsWith("@Experimental")) {
                            listOf(line.substringAfter("@").substringBefore("(").trim())
                        } else {
                            emptyList()
                        }

                        // 3) alle OptIn Infos erstellen
                        val optInInfos = optIns.map {
                            val optIn = it.removeSuffix("::class")
                            val matchingImport = allImports.find { it.endsWith(".$optIn") }
                            val fullOptIn = matchingImport ?: optIn
                            ExperimentalAnnotationInfo(
                                name = fullOptIn,
                                count = 1,
                                type = ExperimentalAnnotationInfo.Type.OptIn
                            )
                        }

                        // 4) alle Experimental Infos erstellen
                        val experimentalInfos = experimental.map {
                            val exp = it.removeSuffix("::class")
                            val matchingImport = allImports.find { it.endsWith(".$exp") }
                            val fullExp = matchingImport ?: exp
                            ExperimentalAnnotationInfo(
                                name = fullExp,
                                count = 1,
                                type = ExperimentalAnnotationInfo.Type.Experimental
                            )
                        }

                        val allInfos = optInInfos + experimentalInfos
                        experimentalAnnotationsInFile = ExperimentalAnnotationInfo.combine(
                            experimentalAnnotationsInFile,
                            allInfos
                        )
                            .filter { !ignoreExperimentalAnnoation(it.name) }
                    }

                if (experimentalAnnotationsInFile.isNotEmpty()) {
                    println("Found experimental annotations in file ${it.relativeTo(rootDir)}:")
                    experimentalAnnotationsInFile.forEach { println("- $it") }
                }

                experimentalAnnotations = ExperimentalAnnotationInfo.combine(
                    experimentalAnnotations,
                    experimentalAnnotationsInFile
                ).sorted()
            }

        // 3) create header replacement
        val imageMavenCentral = ReadmeDefaults.imageMavenCentral(libraryConfig)
        val imageAPI = ReadmeDefaults.imageAPI(minSdk)
        val imageKotlin = ReadmeDefaults.imageKotlin(config, libraryConfig)
        val imageKMP = ReadmeDefaults.imageKMP()
        //val imageLicence = ReadmeDefaults.imageLicence(config, libraryConfig)

        val headerLine1 = "$imageMavenCentral $imageAPI $imageKotlin $imageKMP"// $imageLicence"
        val headerLine2 = "# ${libraryConfig.library.name}"
        val headerLine3 = listOfNotNull(
            ReadmeDefaults.ImageSupportedPlatforms,
            if (isAndroidSupported) ReadmeDefaults.ImageAndroid else null,
            if (isIosSupported) ReadmeDefaults.ImageIOS else null,
            if (isWindowsSupported) ReadmeDefaults.ImageWindows else null,
            if (isMacOsSupported) ReadmeDefaults.ImageMacOS else null,
            if (isLinuxSupported) ReadmeDefaults.ImageLinux else null,
            if (isWasmSupported) ReadmeDefaults.ImageWASM else null,
            if (isJsSupported) ReadmeDefaults.ImageJS else null,
        )
            .joinToString(" ")
        val header = listOf(
            headerLine1,
            headerLine2,
            headerLine3
        ).joinToString("\n")

        // 4) create supported platforms table
        val supportedPlatformsTable = buildString {
            val header = listOf("Module") + allSupportedPlatforms
            appendLine("| " + header.joinToString(" | ") + " |")
            appendLine("|" + header.joinToString("|") { "---" } + "|")
            for ((module, platforms) in supportedPlatforms) {
                val row =
                    listOf(module.artifactId) + allSupportedPlatformsLowercase.map { platform ->
                        if (platforms.map { it.lowercase() }
                                .contains(platform.lowercase())) "✅" else "❌"
                    }
                appendLine("| " + row.joinToString(" | ") + " |")
            }
        }.takeIf { nonPluginModules.isNotEmpty() } ?: ""
        val versionsTable = buildString {
            val header = listOf("Dependency", "Version")
            appendLine("| " + header.joinToString(" | ") + " |")
            appendLine("|" + header.joinToString("|") { "---" } + "|")

            // kotlin
            val kotlinVersion = readTOMLProperty(fileLibsVersionToml, "versions", "kotlin")

            // org.jetbrains.compose
            val jetbrainsCompose =
                tryReadTOMLProperty(fileLibsVersionToml, "versions", "jetbrains-compose")
            val jetbrainsComposeMaterial3 =
                tryReadTOMLProperty(
                    fileLibsVersionToml,
                    "versions",
                    "jetbrains-compose-material3"
                )

            appendLine("| Kotlin | `$kotlinVersion` |")
            if (jetbrainsCompose != null)
                appendLine("| Jetbrains Compose | `$jetbrainsCompose` |")
            if (jetbrainsComposeMaterial3 != null)
                appendLine("| Jetbrains Compose Material3 | `$jetbrainsComposeMaterial3` |")
        }.takeIf { nonPluginModules.isNotEmpty() } ?: ""
        val experimentalInfo = buildString {
            if (experimentalAnnotations.isNotEmpty()) {
                appendLine("> :warning: Following experimental annotations are used:")
                val grouped = experimentalAnnotations.groupBy { it.type }
                grouped.forEach { type, infos ->
                    appendLine("> - **${type.name}**")
                    infos.forEach {
                        appendLine(">   - `${it.name}` (${it.count}x)")
                    }
                }
                appendLine(">")
                appendLine("> I try to use as less experimental features as possible, but in this case the ones above are needed!")
            }
        }

        // 5) create setup instructions
        val libraryName = libraryConfig.library.name.lowercase().replace(" ", "-")
        val setupViaDependencies = buildString {
            if (nonPluginModules.isNotEmpty()) {
                appendLine("val $libraryName = \"<LATEST-VERSION>\"")
                appendLine()
            }
            for (module in nonPluginModules) {
                appendLine("implementation(\"${libraryConfig.maven.groupId}:${module.artifactId}:\${$libraryName}\")")
            }
            if (pluginModules.isNotEmpty()) {
                appendLine("plugins {")
                for (module in pluginModules) {
                    appendLine("    id(\"${libraryConfig.maven.groupId}.${module.artifactId}\") version \"<LATEST-VERSION>\"")
                }
                appendLine("}")
            }
        }

        val setupViaVersionCatalogue1 = buildString {
            appendLine("[versions]")
            appendLine()
            appendLine("$libraryName = \"<LATEST-VERSION>\"")
            appendLine()
            if (nonPluginModules.isNotEmpty()) {
                appendLine("[libraries]")
                appendLine()
                for (module in nonPluginModules) {
                    appendLine("${libraryName}-${module.artifactId} = { module = \"${libraryConfig.maven.groupId}:${module.artifactId}\", version.ref = \"$libraryName\" }")
                }
            }
            if (pluginModules.isNotEmpty()) {
                appendLine("[plugins]")
                appendLine()
                for (module in pluginModules) {
                    appendLine("${libraryName}-${module.artifactId} = { id = \"${libraryConfig.maven.groupId}.${module.artifactId}\", version.ref = \"$libraryName\" }")
                }
            }
        }
        val setupViaVersionCatalogue2 = buildString {
            for (module in nonPluginModules) {
                val key = "${libraryName}-${module.artifactId}".replace("-", ".")
                appendLine("implementation(libs.$key)")
            }
            if (pluginModules.isNotEmpty())
            {
                appendLine("plugins {")
                for (module in pluginModules) {
                    val key = "${libraryName}-${module.artifactId}".replace("-", ".")
                    appendLine("    id(libs.$key)")
                }
                appendLine("}")
            }
        }

        // 6) create screenshot replacement
        val screenshots = if (folderDocumentationScreenshots.exists()) {
            val excludedScreenshots = mutableListOf<File>()
            val result = folderDocumentationScreenshots
                .walkTopDown()
                .filter { it.isFile }
                .toList()
                .filter {
                    val relativeFileInScreenshotsFolder =
                        it.relativeTo(folderDocumentationScreenshots)
                    val topFolderName =
                        relativeFileInScreenshotsFolder.invariantSeparatorsPath.substringBefore("/")
                    val exclude =
                        if (config.readme.screenshots.excludeRoot && it.parentFile == folderDocumentationScreenshots)
                            true
                        else if (config.readme.screenshots.excludedFolders.contains(topFolderName))
                            true
                        else if (config.readme.screenshots.excludedImages.contains(it.nameWithoutExtension))
                            true
                        else
                            false
                    if (exclude)
                        excludedScreenshots += it
                    !exclude
                }
                .map {
                    Screenshot(it, rootDir, folderDocumentationScreenshots)
                }
            if (excludedScreenshots.isNotEmpty()) {
                println("")
                println("Following screenshots are excluded from README based on the configuration:")
                excludedScreenshots.forEach {
                    println("- ${it.relativeTo(rootDir)}")
                }
            }
            result.sortedBy { it.relativeFile.invariantSeparatorsPath.lowercase() }
        } else {
            emptyList()
        }
        val screenshotsTable = if (config.readme.screenshots.groupByFolders) {
            val grouped = screenshots.groupBy { it.topFolderName() }.toSortedMap()
            println("")
            println("Building screenshots table with folder grouping. Found the following folders:")
            grouped.map { (key, screenshots) ->
                println("- folder '$key' with ${screenshots.size} screenshots")
                val table = buildMarkdownTable(null, screenshots, 3) { it.markdownImage }
                if (key.isEmpty())
                    table
                else
                    "### ${key}\n\n" + table
            }.joinToString("\n")
        } else
            buildMarkdownTable(null, screenshots, 3) { it.markdownImage }

        val demo = if (File(rootDir, "demo").exists())
            "A full [demo](/demo) is included inside the demo module, it shows nearly every usage with working examples."
        else ""

        val apiDocs = if (hasApiDocs) {
            val link =
                "https://${config.developer.githubUserName}.github.io/${libraryConfig.library.name}/"
            "Check out the [API documentation]($link)."
        } else {
            ""
        }

        // 7) read template content
        var readmeContent = readmeTemplate

        // 8) replace placeholders in readme with content from markdown files (all but table of contents)
        val replacements = listOf(
            Placeholder("{{ header }}", header),
            Partial(
                "{{ partials.info }}",
                File(rootDir, "documentation/_partials/info.md.partial")
            ),
            Partial(
                "{{ partials.usage }}",
                File(rootDir, "documentation/_partials/usage.md.partial")
            ),
            Placeholder("{{ modules }}", moduleLinks.joinToString("\n")),
            Placeholder("{{ links }}", otherLinks.joinToString("\n")),
            Placeholder("{{ supported_platforms }}", supportedPlatformsTable),
            Placeholder("{{ versions }}", versionsTable),
            Placeholder("{{ experimental }}", experimentalInfo),
            Placeholder("{{ setup-via-dependencies }}", setupViaDependencies),
            Placeholder("{{ setup-via-version-catalogue1 }}", setupViaVersionCatalogue1),
            Placeholder("{{ setup-via-version-catalogue2 }}", setupViaVersionCatalogue2),
            Placeholder("{{ screenshots }}", screenshotsTable),
            Placeholder("{{ other-libraries }}", ReadmeDefaults.GithubMyLibrariesLink),
            Placeholder("{{ demo }}", demo),
            Placeholder("{{ api-docs }}", apiDocs),
        )
        for (replacement in replacements) {
            readmeContent = replacement.replace(readmeContent)
        }

        // 9) remove headers (lines starting with #) without content after them until the next header
        // logik neu: falls 2 aufeinanderfolgende header aus ReadmeDefaults.allHeaders ohne content dazwischen existieren (nur whitespaces, leere zeilen),
        // dann wird der header ohne content entfernt
        val allMarkdownHeaders = ReadmeDefaults.allHeaders.map { it.markdownHeader() }
        var lastFoundHeaderIndexWithoutContentBelow = -1
        val lines = readmeContent.lines().toMutableList()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()
            val headerIndex = allMarkdownHeaders.indexOf(trimmedLine)

            if (headerIndex != -1) {
                if (lastFoundHeaderIndexWithoutContentBelow != -1 && lastFoundHeaderIndexWithoutContentBelow == headerIndex - 1) {
                    // der letzte header hatte keinen content darunter und ist direkt vor dem aktuellen header
                    // also entferne den letzten header aus cleanedLines
                    for (j in lines.size - 1 downTo 0) {
                        if (lines[j].trim() == allMarkdownHeaders[lastFoundHeaderIndexWithoutContentBelow]) {
                            lines.removeAt(j)
                            break
                        }
                    }
                }
                lastFoundHeaderIndexWithoutContentBelow = headerIndex
            } else {
                if (trimmedLine.isEmpty()) {
                    // leere Zeile
                } else {
                    // nicht leere Zeile - aktueller header hat content darunter
                    lastFoundHeaderIndexWithoutContentBelow = -1
                }
            }
            i++
        }
        readmeContent = lines.joinToString("\n")

        // 10)  replacement - table of content
        val tableOfContent = ReadmeDefaults.allHeaders
            .filter { it != ReadmeDefaults.headerTableOfContent }
            .filter {
                val header = it.markdownHeader()
                if (readmeContent.contains(header)) {
                    true
                } else {
                    false
                }
            }.joinToString("\n") { "- ${it.markdownLink()}" }
        readmeContent =
            Placeholder("{{ tableOfContent }}", tableOfContent).replace(readmeContent)

        // 11) remove multiple trimmed empty lines => max is 1 empty line
        val finalLines = mutableListOf<String>()
        var lastLineEmpty = false
        for (line in readmeContent.lines()) {
            if (line.isBlank()) {
                if (!lastLineEmpty) {
                    finalLines.add("")
                    lastLineEmpty = true
                }
            } else {
                finalLines.add(line)
                lastLineEmpty = false
            }
        }
        readmeContent = finalLines.joinToString("\n")

        // 12) write updated readme content to README.md
        fileReadme.writeText(readmeContent)

        println("")
        println("##############################")
        println("### END Updating README.md ###")
        println("##############################")
    }

    /**
     * Returns a Markdown string for an image, optionally wrapped in a link.
     * @param imageUrl The URL of the image (required)
     * @param altText The alt text for the image (optional, default: "")
     * @param linkUrl The URL to link to (optional, default: null)
     * @return Markdown string for the image, optionally as a link
     */
    internal fun markdownImage(
        imageUrl: String,
        altText: String = "",
        linkUrl: String? = null,
    ): String {
        val imageMarkdown = "![${altText}](${imageUrl})"
        return if (linkUrl != null) {
            "[${imageMarkdown}](${linkUrl})"
        } else {
            imageMarkdown
        }
    }

    /**
     * Reads a property from a TOML file.
     *
     * @param file The TOML file to read from.
     * @param region The region in the TOML file (e.g. "versions")
     * @param key The key to read (e.g. "minSDK")
     */
    private fun readTOMLProperty(file: File, region: String, key: String): String {
        val content = file.readText(Charsets.UTF_8)
        val regionStart = content.indexOf("[$region]")
        if (regionStart == -1) {
            throw RuntimeException("Region [$region] not found in TOML file: ${file.path}")
        }
        val regionEnd =
            content.indexOf("[", regionStart + 1).let { if (it == -1) content.length else it }
        val regionContent = content.substring(regionStart, regionEnd)
        val regex = Regex("""$key\s*=\s*["']?([^"'\n\r]+)["']?""")
        val matchResult = regex.find(regionContent)
        return matchResult?.groups?.get(1)?.value
            ?: throw RuntimeException("Key '$key' not found in region [$region] of TOML file: ${file.path}")
    }

    /**
     * Reads a property from a TOML file.
     *
     * @param file The TOML file to read from.
     * @param region The region in the TOML file (e.g. "versions")
     * @param key The key to read (e.g. "minSDK")
     */
    private fun tryReadTOMLProperty(file: File, region: String, key: String): String? {
        val content = file.readText(Charsets.UTF_8)
        val regionStart = content.indexOf("[$region]")
        if (regionStart == -1) {
            return null
        }
        val regionEnd =
            content.indexOf("[", regionStart + 1).let { if (it == -1) content.length else it }
        val regionContent = content.substring(regionStart, regionEnd)
        val regex = Regex("""$key\s*=\s*["']?([^"'\n\r]+)["']?""")
        val matchResult = regex.find(regionContent)
        return matchResult?.groups?.get(1)?.value
    }

    /**
     * Parses the supported platforms from a module's build.gradle.kts file.
     *
     * content may look like this:
     * val buildTargets = Targets(
     *     // mobile
     *     android = true,
     *     iOS = true,
     *     // desktop
     *     windows = true,
     *     macOS = true,
     *     // web
     *     wasm = true
     * )
     *
     *
     * @param file The root directory of the module.
     * @return A list of supported platform names.
     */
    private fun getSupportedPlatformsFromModule(file: File): List<String> {

        // 1) get buildTargets block
        val regex = Regex("val buildTargets = Targets\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        val matchResult = regex.find(file.readText())

        // 2) extract platforms
        val platforms = mutableListOf<String>()
        matchResult?.groups?.get(1)?.value
            ?.lines()
            ?.filter { !it.trim().startsWith("//") && it.contains("=") }
            ?.forEach {
                val parts = it.trim()
                    .removeSuffix(",")
                    .split("=")
                if (parts.size == 2) {
                    val platformName = parts[0].trim()
                    val isEnabled = parts[1].trim().toBoolean()
                    if (isEnabled) {
                        platforms.add(platformName)
                    }
                }
            }

        return platforms
    }

    private fun buildMarkdownLinks(folders: List<FolderLink>): List<String> {
        fun build(folderLinks: List<FolderLink>, indent: String = ""): List<String> {
            return folderLinks.sortedBy { it.name }.flatMap { folder ->
                val lines = mutableListOf<String>()
                if (folder.link != null) {
                    lines.add("$indent- ${folder.link}")
                } else {
                    lines.add("$indent- ${folder.name}")
                }
                if (folder.children.isNotEmpty()) {
                    lines.addAll(build(folder.children, indent + "  "))
                }
                lines
            }
        }
        return build(folders)
    }

    // Hilfsfunktion: Erzeugt die FolderLink-Hierarchie aus einer Liste von CustomMarkdownFile
    private fun buildFolderLinkHierarchy(
        customMarkdownFiles: List<CustomMarkdownFile>,
        rootDir: File,
        folderDocumentation: File,
    ): List<FolderLink> {

        // 1. Alle relativen Pfade zu den Markdown-Dateien bestimmen
        val relPaths = customMarkdownFiles.map { it.relativePathTo(folderDocumentation) }

        // 2. Hierarchie-Baum aufbauen
        fun buildTree(paths: List<String>, parent: String = ""): List<FolderLink> {
            val grouped = paths.groupBy {
                val key = it.substringBefore("/", it)
                key
            }
            return grouped.entries.sortedBy { it.key }.map { (key, group) ->
                val children = group.filter { it.contains("/") }.map { it.substringAfter("/") }
                val fullPath = if (parent.isEmpty()) key else "$parent/$key"
                if (children.isNotEmpty()) {
                    FolderLink(
                        name = key,
                        link = null,
                        children = buildTree(children, fullPath)
                    )
                } else {
                    val fileObj =
                        customMarkdownFiles.find { it.file.relativeTo(folderDocumentation).invariantSeparatorsPath == fullPath }
                    FolderLink(
                        name = key,
                        link = fileObj?.let {
                            val rel = it.file.relativeTo(rootDir).invariantSeparatorsPath
                            val encodedRelativePath = encodeRelativeLink(rel)
                            "[${it.name}]($encodedRelativePath)"
                        },
                        children = emptyList()
                    )
                }
            }
        }
        return buildTree(relPaths)
    }

    private fun encodeRelativeLink(link: String): String {
        return link.replace(" ", "%20")
    }

    private fun <T> buildMarkdownTable(
        headers: List<String>?,
        items: List<T>,
        columns: Int,
        itemToCell: (item: T) -> String,
    ): String {
        val rows = items.chunked(columns)
        val table = StringBuilder()
        if (rows.isNotEmpty()) {
            if (headers != null) {
                table.append("| " + headers.joinToString(" | ") + " |\n")
                table.append("|" + headers.joinToString("|") { "---" } + "|\n")
            } else {
                val emptyHeaders = List(columns) { "" }
                table.append("| " + emptyHeaders.joinToString(" | ") + " |\n")
                table.append("|" + emptyHeaders.joinToString("|") { "---" } + "|\n")
            }
        }
        for (row in rows) {
            table.append("| " + row.joinToString(" | ") { itemToCell(it) } + " |\n")
        }
        return table.toString()
    }

    private data class ExperimentalAnnotationInfo(
        val name: String,
        val count: Int,
        val type: Type,
    ) : Comparable<ExperimentalAnnotationInfo> {

        enum class Type {
            OptIn, Experimental
        }

        companion object {

            fun combine(
                list1: List<ExperimentalAnnotationInfo>,
                list2: List<ExperimentalAnnotationInfo>,
            ): List<ExperimentalAnnotationInfo> {
                val combined = mutableListOf<ExperimentalAnnotationInfo>()
                val all = list1 + list2
                for (info in all) {
                    val index =
                        combined.indexOfFirst { it.name == info.name && it.type == info.type }
                    if (index != -1) {
                        val existing = combined.removeAt(index)
                        combined.add(existing.copy(count = existing.count + info.count))
                    } else {
                        combined.add(info)
                    }
                }
                return combined
            }
        }

        override fun compareTo(other: ExperimentalAnnotationInfo): Int {
            // zuerst nach type sortieren (Experimental vor OptIn), dann nach name alphabetisch (ignoriere case)
            return when {
                this.type != other.type -> this.type.compareTo(other.type)
                else -> this.name.lowercase().compareTo(other.name.lowercase())
            }
        }
    }

    private class Screenshot private constructor(
        val relativeFile: File,
        val relativeDocumentationFile: File,
    ) {
        constructor(
            file: File,
            rootDir: File,
            documentationDir: File,
        ) : this(
            relativeFile = file.relativeTo(rootDir),
            relativeDocumentationFile = file.relativeTo(documentationDir)
        )

        fun topFolderName(): String {
            val parts = relativeDocumentationFile.path.replace("\\", "/").split("/")
            return if (parts.size > 1) parts.first() else ""
        }

        val relativePath = relativeFile.path.replace("\\", "/")
        val markdownImage = "![${relativeFile.nameWithoutExtension}]($relativePath)"
    }
}
