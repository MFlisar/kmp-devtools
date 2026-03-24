pluginManagement {

    // repositories for build
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        mavenLocal()
    }

}

dependencyResolutionManagement {

    // repositories for dependencies
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        mavenLocal()
    }

    versionCatalogs {
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
        create("app") {
            from(files("gradle/app.versions.toml"))
        }
    }

}

// --------------
// Library
// --------------

include(":library:plugins:build-gradle")
include(":library:plugins:settings-gradle")
include(":library:core")