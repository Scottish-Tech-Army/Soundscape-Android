import com.google.protobuf.gradle.id
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.jaredsburrows.license)
}

android {
    namespace = "org.scottishtecharmy.soundscape"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

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
        versionCode = 158
        versionName = "0.2.15"

        // Maintaining this list means that we can exclude translations that aren't complete yet
        resourceConfigurations.addAll(listOf(
            "arz",
            "zh-rCN",
            "da",
            "de",
            "el",
            "en",
            "en-rGB",
            "es",
            "fa",
            "fi",
            "fr",
            "fr-rCA",
            "hi",
            "is",
            "it",
            "ja",
            "nb",
            "nl",
            "pl",
            "pt",
            "pt-rBR",
            "ro",
            "ru",
            "sv",
            "uk"
        ))

        // Retrieve the tile provider URL and API key from local.properties. This is not under
        // version control and must be configured by each developer locally. GitHub actions fill in
        // local.properties from a secret.
        var tileProviderUrl = ""
        var tileProviderApiKey = ""
        var searchProviderUrl = ""
        var searchProviderApiKey = ""
        var extractProviderUrl = ""
        try {
            val localProperties = Properties()
            localProperties.load(FileInputStream(rootProject.file("local.properties")))
            tileProviderUrl = localProperties["tileProviderUrl"].toString()
            tileProviderApiKey = localProperties["tileProviderApiKey"].toString()
            searchProviderUrl = localProperties["searchProviderUrl"].toString()
            searchProviderApiKey = localProperties["searchProviderApiKey"].toString()
            extractProviderUrl = localProperties["extractProviderUrl"].toString()
        } catch (e: Exception) {
            println("Failed to load local.properties for tile and search providers: $e")
        }
        buildConfigField("String", "TILE_PROVIDER_URL", "\"${tileProviderUrl}\"")
        buildConfigField("String", "TILE_PROVIDER_API_KEY", "\"${tileProviderApiKey}\"")
        buildConfigField("String", "SEARCH_PROVIDER_URL", "\"${searchProviderUrl}\"")
        buildConfigField("String", "SEARCH_PROVIDER_API_KEY", "\"${searchProviderApiKey}\"")
        buildConfigField("String", "EXTRACT_PROVIDER_URL", "\"${extractProviderUrl}\"")

        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("String", "FMOD_LIB", "\"fmod\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    buildTypes {

// For debugging proguard uncomment the following:
//        debug {
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }

        debug {
            buildConfigField("Boolean", "DUMMY_ANALYTICS", "true")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "DUMMY_ANALYTICS", "false")
        }

        create("releaseTest") {
            initWith(getByName("release"))
            buildConfigField("Boolean", "DUMMY_ANALYTICS", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

    kotlin {
        compilerOptions {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_19)
        }
    }
    buildFeatures {
        compose = true
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

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    //stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.3"
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

licenseReport {
    // Generate reports
    generateCsvReport = false
    generateHtmlReport = false
    generateJsonReport = true
    generateTextReport = false

    // Copy reports - These options are ignored for Java projects
    copyCsvReportToAssets = false
    copyHtmlReportToAssets = false
    copyJsonReportToAssets = true
    copyTextReportToAssets = false
    useVariantSpecificAssetDirs = false

    // Ignore licenses for certain artifact patterns
    ignoredPatterns = emptySet()

    // Show versions in the report - default is false
    showVersions = true
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
//    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.screenshot.validation.api)
    implementation(libs.core.google.shortcuts)

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
    implementation(libs.converter.protobuf) {
        exclude("com.google.protobuf")
    }

    // Location permissions
    implementation(libs.accompanist.permissions)

    // GeoJSON parsing
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    // Dependency injection
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)


    // LiveData
    implementation(libs.androidx.runtime.livedata)

    // Realm for Kotlin
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

    // MapLibre library
    implementation (libs.maplibre)
    implementation (libs.maplibre.annotations)
//    implementation (libs.maplibre.compose.material3)

    androidTestImplementation(libs.androidx.uiautomator)

    // Protobuf
    implementation(libs.protobuf.kotlin.lite)

    // In app review
    implementation(libs.review)
    implementation(libs.review.ktx)

    // Library for preferences in compose
    implementation(libs.composepreferencelibrary)
    implementation(libs.androidx.preference.ktx)

    // Rtree spatial search library
    implementation(libs.rtree2)

    // JTS for manipulating 2D geometry
    implementation(libs.jts.core)

    // Dokka plugin
    dokkaPlugin(libs.html.mermaid.dokka.plugin)

    // Leak canary
    debugImplementation(libs.leakcanary.android)

    implementation(libs.reorderable)
    implementation(libs.widgets)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.symbol.processing.api)

    // Markdown to HTML converter for Help screens
    implementation(libs.commonmark)

    // PMTiles reading libraries
    implementation(libs.pmtilesreader)

    // Address formatting library
    implementation(libs.androidaddressformatter)

    testImplementation(libs.json)
}
