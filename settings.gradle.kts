pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io")}
    }
}

plugins {
    id("com.gradle.develocity") version ("3.19.2")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "Soundscape"
include(":app")
include(":shared")

// Apply local patches to ComposePreference submodule before includeBuild configures it
val patchFile = file("patches/ComposePreference.patch")
val patchMarker = file("ComposePreference/.patched")
if (patchFile.exists() && !patchMarker.exists()) {
    val cpDir = file("ComposePreference")
    ProcessBuilder("git", "checkout", ".").directory(cpDir).start().waitFor()
    val apply = ProcessBuilder("git", "apply", patchFile.absolutePath).directory(cpDir).start()
    if (apply.waitFor() == 0) {
        patchMarker.writeText("patched")
    }
}

includeBuild("ComposePreference") {
    dependencySubstitution {
        substitute(module("me.zhanghai.compose.preference:preference")).using(project(":preference"))
    }
}
 