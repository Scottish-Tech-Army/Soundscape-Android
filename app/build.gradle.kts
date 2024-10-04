import com.google.protobuf.gradle.id
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("io.realm.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.screenshot)
    id("com.google.protobuf")
    id("org.jetbrains.dokka")
}

android {
    namespace = "org.scottishtecharmy.soundscape"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "org.scottishtecharmy.soundscape"
        minSdk = 30
        targetSdk = 35
        versionCode = 43
        versionName = "0.0.42"

        // Retrieve the tile provider API from local.properties. This is not under version control
        // and must be configured by each developer locally. GitHb actions fill in local.properties
        // from a secret.
        var tileProviderApiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        try {
            val localProperties = Properties()
            localProperties.load(FileInputStream(rootProject.file("local.properties")))
            tileProviderApiKey = localProperties["tileProviderApiKey"].toString()
        } catch (e: Exception) {
            println("Failed to load local.properties for TILE_PROVIDER_API_KEY: $e")
        }
        buildConfigField("String", "TILE_PROVIDER_API_KEY", "\"${tileProviderApiKey}\"")

        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("String", "FMOD_LIB", "\"fmod\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        resourceConfigurations += listOf("da", "de", "el", "en", "es", "fi", "fr", "it", "ja", "nb", "nl", "pt", "sv")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
        experimentalProperties["android.experimental.enableScreenshotTest"] = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.0"
    }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                id("kotlin") {
                    option("lite")
                }
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)


    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.services.location)

    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.core.ktx)
    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.lifecycle.runtime.compose.android)


    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation (libs.kotlin.test.junit)
    testImplementation(libs.junit.jupiter)

    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Viewmodel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    // logging interceptor
    implementation (libs.logging.interceptor)

    // Retrofit with Scalar Converter
    implementation(libs.converter.scalars)
    implementation(libs.converter.moshi)

    // Location permissions
    implementation(libs.accompanist.permissions)

    // GeoJSON parsing
    implementation(libs.moshi)
    //kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    ksp(libs.moshi.kotlin.codegen)

    // Dependency injection
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)


    // LiveData
    implementation(libs.androidx.runtime.livedata)

    // Realm for Kotlin
    implementation(libs.library.base)
    implementation(libs.kotlinx.coroutines.core)

    // Datastore for onboarding and settings
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)

    // Audio engine
    implementation(files("libs/fmod.jar"))

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // GPX parser
    implementation (libs.android.gpx.parser)

    // Open Street Map compose library
    implementation (libs.mapcompose)
    // MapLibre library
    implementation (libs.maplibre)
    implementation (libs.maplibre.annotations)

    // Screenshots for tests
    //screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Protobuf
    implementation(libs.protobuf.kotlin.lite)

    // In app review
    implementation(libs.review)
    implementation(libs.review.ktx)

    // Library for preferences in compose
    implementation(libs.composepreferencelibrary)
    implementation(libs.androidx.preference.ktx)

    // Dokka plugin
    dokkaPlugin(libs.html.mermaid.dokka.plugin)

}