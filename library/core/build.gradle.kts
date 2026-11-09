import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.maven.publish.plugin)
}

// -------------------
// Informations
// -------------------

val description =
    "a gradle plugin and plain kotlin dependency that provides core functions for kmp libraries for development tooling"

// Module
val artifactId = "core"

// Library
val libraryName = "kmp-devtools"
val libraryDescription = "$libraryName - $artifactId module - $description"
val groupID = "io.github.mflisar.kmpdevtools"
val release = 2025
val github = "https://github.com/MFlisar/kmp-devtools"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"

// -------------------
// Setup
// -------------------

kotlin {

    sourceSets {

        sourceSets {

            val main by getting { }
            val test by getting { }

            main.dependencies {

                implementation(gradleKotlinDsl())

                implementation(deps.yaml)

                implementation(libs.kotlinx.serialization.json )

            }

            test.dependencies {
                implementation(kotlin("test"))
            }

        }

    }
}

dependencies {
    testImplementation(kotlin("test"))
}

// -------------------
// Configurations
// -------------------

tasks.named<Jar>("jar") {
    dependsOn(tasks.named("compileJava"))
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = SourcesJar.Sources()
        )
    )

    coordinates(
        groupId = groupID,
        artifactId = artifactId,
        version = System.getenv("TAG") ?: "LOCAL-SNAPSHOT"
    )

    pom {
        name.set(libraryName)
        description.set(libraryDescription)
        inceptionYear.set("$release")
        url.set(github)

        licenses {
            license {
                name.set(license)
                url.set(licenseUrl)
            }
        }

        developers {
            developer {
                id.set("mflisar")
                name.set("Michael Flisar")
                email.set("mflisar.development@gmail.com")
            }
        }

        scm {
            url.set(github)
        }
    }

    // Configure the explicitly selected publishing destination.
    if (System.getenv("PUBLISH_TARGET") != "github")
        publishToMavenCentral(true)

    // Enable GPG signing for all publications
    if (System.getenv("CI")?.toBoolean() == true && System.getenv("PUBLISH_TARGET") != "github")
        signAllPublications()
}

if (System.getenv("PUBLISH_TARGET") == "github") {
    publishing.repositories.maven {
        name = "GitHub"
        url = layout.buildDirectory.dir("maven-repo").get().asFile.toURI()
    }
}

tasks.register<JavaExec>("runGithubUtilTestApp") {
    group = "verification"
    description = "Runs a small test app that exercises GithubUtil.getLastRelease"
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.michaelflisar.kmpdevtools.core.utils.GithubUtilTestAppKt")
}