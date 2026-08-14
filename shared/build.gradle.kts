plugins {
    alias(libs.plugins.android.kotlinMultiplatformLibrary)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.squareup.wire)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    // Suppress KT-61573 Beta warning for MarkersAndRoutesDatabase's
    // expect/actual class pair (source + Room-generated constructor).
    // Applied at the KMP-extension level so all target compilations pick it up.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "org.scottishtecharmy.soundscape.shared"
        compileSdk = 37
        minSdk = 30
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
        androidResources.enable = true
        // Run commonTest on the JVM too - previously it was only compiled (never executed)
        // for iosSimulatorArm64Test on the macOS CI runner, and never run at all on Android.
        withHostTest {}
    }
    // Only configure iOS targets on macOS (local dev) or if explicitly requested via -PincludeIos=true.
    // This speeds up build/sync times on Linux/Windows where iOS targets cannot be compiled.
    val includeIos = System.getProperty("os.name").lowercase().contains("mac") || project.hasProperty("includeIos")
    if (includeIos) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach {
            it.binaries.framework {
                baseName = "Shared"
                isStatic = true
                freeCompilerArgs += listOf("-Xbinary=bundleId=org.scottishtecharmy.soundscape.shared")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.material.icons.extended)
            implementation(libs.compose.multiplatform.ui)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.10.3")
            api(libs.compose.multiplatform.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.maplibre.compose)
            implementation(libs.okio)
            implementation(libs.composepreferencelibrary)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.intellij.markdown)
            implementation(libs.reorderable)
            implementation(libs.kable.core)
            implementation(libs.composepreferencelibrary)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain {
            dependencies {
                implementation(libs.jts.core)
                implementation(libs.ktor.client.okhttp)
            }
            // Reuse shared resources for the Android target so JSON data files live
            // in a single canonical location consumed by both platforms.
            resources.srcDir("src/commonMain/resources")
        }
        
        // iosMain is created automatically by the default hierarchy template
        // when iOS targets are enabled. We configure its dependencies below
        // using configureEach to avoid "SourceSet not found" errors when 
        // iOS targets are disabled.
    }

    sourceSets.configureEach {
        if (name == "iosMain") {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

wire {
    kotlin {
    }
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    val includeIos = System.getProperty("os.name").lowercase().contains("mac") || project.hasProperty("includeIos")
    if (includeIos) {
        add("kspIosArm64", libs.androidx.room.compiler)
        add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.scottishtecharmy.soundscape.resources"
}
