import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.screenshot)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.jaredsburrows.license)
}

android {
    namespace = "org.scottishtecharmy.soundscape"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
        prefab = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    testOptions {
        unitTests {
            // Let JVM unit tests touch android.util.Log (etc.) without mocking - they
            // return default values instead of throwing "not mocked".
            isReturnDefaultValues = true
        }
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
        targetSdk = 37
        versionCode = 1011
        versionName = "2.0.11"


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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += listOf(
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DANDROID_STL=c++_shared"
                )
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
            // Embed native debug symbols in the App Bundle so native (NDK) crashes
            // symbolicate in Play Console / Crashlytics. Note: this can only extract
            // symbols from libraries that still contain them - prebuilt third party
            // libs that ship stripped (e.g. libmaplibre.so) must be symbolicated
            // manually using their BuildId and the matching debug-symbols package.
            ndk {
                debugSymbolLevel = "FULL"
            }
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
            // The :shared module only declares `debug` and `release`, so fall
            // back to its `release` variant when consuming it from this build.
            matchingFallbacks += listOf("release")
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
    androidResources {
        // Maintaining this list means that we can exclude translations that aren't complete yet
        localeFilters += listOf(
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
            "tr",
            "uk"
        )
        // Keep the map style assets stored uncompressed in the APK. "pbf" is unique to the
        // glyph fonts in this folder; the style JSON/sprite PNGs need exact paths since those
        // extensions are also used elsewhere in the app.
        noCompress += listOf(
            "pbf",
            "assets/osm-liberty-accessible/style.json",
            "assets/osm-liberty-accessible/originalStyle.json",
            "assets/osm-liberty-accessible/osm-liberty.json",
            "assets/osm-liberty-accessible/osm-liberty@2x.json",
            "assets/osm-liberty-accessible/osm-liberty.png",
            "assets/osm-liberty-accessible/osm-liberty@2x.png",
        )
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    lint {
        warning += "MissingTranslation"
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    //stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

// Generates app/res strings.xml files for the keys consumed by res/xml/shortcuts.xml
// (referenced from AndroidManifest as launcher app-shortcuts). Manifest-resolved
// @string/... lookups can't reach Compose Resources, so we project the relevant keys
// out of shared/src/commonMain/composeResources into Android resources at build time.
abstract class GenerateShortcutStringsTask @javax.inject.Inject constructor(
    objects: org.gradle.api.model.ObjectFactory
) : DefaultTask() {
    @get:InputFiles
    val sharedResources: ConfigurableFileCollection = objects.fileCollection()

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val keys = listOf("routes_title", "markers_title")
        val outDir = outputDir.get().asFile
        outDir.deleteRecursively()

        val localeDirs: Set<File> = sharedResources.files
            .mapNotNull { it.parentFile }
            .filter { it.isDirectory && (it.name == "values" || it.name.startsWith("values-")) }
            .toSet()

        for (localeDir in localeDirs) {
            val stringsFile = localeDir.resolve("strings.xml")
            if (!stringsFile.exists()) continue

            val content = stringsFile.readText()
            val extracted = keys.mapNotNull { key ->
                val pattern = Regex("""<string\s+name="$key"[^>]*>([\s\S]*?)</string>""")
                pattern.find(content)?.let { key to it.groupValues[1] }
            }

            val isDefault = localeDir.name == "values"
            if (extracted.isEmpty() && !isDefault) continue

            val target = outDir.resolve(localeDir.name).resolve("strings.xml")
            target.parentFile.mkdirs()

            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            sb.append("<!-- Generated by generateShortcutStrings - do not edit. -->\n")
            sb.append("<!-- Source: shared/src/commonMain/composeResources/${localeDir.name}/strings.xml -->\n")
            sb.append("<resources>\n")
            if (isDefault) {
                sb.append("    <string name=\"app_name\" translatable=\"false\">Soundscape</string>\n")
            }
            for ((key, value) in extracted) {
                sb.append("    <string name=\"$key\">$value</string>\n")
            }
            sb.append("</resources>\n")

            target.writeText(sb.toString())
        }
    }
}

val generateShortcutStrings = tasks.register<GenerateShortcutStringsTask>("generateShortcutStrings") {
    sharedResources.from(
        rootProject.fileTree("shared/src/commonMain/composeResources") {
            include("values*/strings.xml")
        }
    )
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            generateShortcutStrings,
            GenerateShortcutStringsTask::outputDir
        )
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

    implementation(project(":shared"))
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

    // JTS kept only for a test helper that calls union() directly
    testImplementation(libs.jts.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation (libs.kotlin.test.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.client.mock)

    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Viewmodel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Location permissions
    implementation(libs.accompanist.permissions)

    // GeoJSON parsing
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Dependency injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)


    // LiveData
    implementation(libs.androidx.runtime.livedata)

    // Realm for Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Datastore for onboarding and settings
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)

    // Audio engine
    implementation("com.google.oboe:oboe:1.9.3")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // GPX parser

    // MapLibre library
    implementation (libs.maplibre)
    implementation (libs.maplibre.annotations)
    implementation (libs.maplibre.compose)

    androidTestImplementation(libs.androidx.uiautomator)

    // In app review
    implementation(libs.review)
    implementation(libs.review.ktx)

    // Library for preferences in compose
    implementation(libs.composepreferencelibrary)
    implementation(libs.androidx.preference.ktx)

    // rtree2 kept only for parity tests against our :shared rtree port
    testImplementation(libs.rtree2)
    androidTestImplementation(libs.rtree2)

    // Dokka plugin
    dokkaPlugin(libs.html.mermaid.dokka.plugin)

    // Leak canary
    debugImplementation(libs.leakcanary.android)

    implementation(libs.symbol.processing.api)

    // Markdown to HTML converter for Help screens
    implementation(libs.commonmark)

    testImplementation(libs.json)
}

dokka {
    dokkaSourceSets.configureEach {
        if (name == "main") {
            suppress.set(true)
        }
    }
}
