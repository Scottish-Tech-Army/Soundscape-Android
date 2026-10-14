// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.kotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.jaredsburrows.license) apply false
}

buildscript {
    configurations.classpath {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-daemon-client:2.3.10")
        }
    }
}
// Keep the Vulkan build of MapLibre out of the app: maplibre-compose and the annotation plugin
// depend on org.maplibre.gl:android-sdk, which from 13.x requires Vulkan and so hides the app on
// Play from phones without it. See the mapLibre entry in gradle/libs.versions.toml.
subprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.maplibre.gl:android-sdk"))
                .using(module("org.maplibre.gl:android-sdk-opengl:${libs.versions.mapLibre.get()}"))
                .because("the Vulkan build requires android.hardware.vulkan.version")
        }
    }
}
