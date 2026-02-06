import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.google.protobuf.gradle.id
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

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
}

android {
    namespace = "org.scottishtecharmy.soundscape"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
        compose = true
    }

    @Suppress("UnstableApiUsage")
    testFixtures {
        enable = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    @Suppress("UnstableApiUsage")
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
        versionCode = 120
        versionName = "0.0.119"

        // Maintaining this list means that we can exclude translations that aren't complete yet
        resourceConfigurations.addAll(listOf(
            "arz",
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
            "it",
            "ja",
            "nb",
            "nl",
            "pl",
            "pt",
            "pt-rBR",
            //"ru", in progress
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
        try {
            val localProperties = Properties()
            localProperties.load(FileInputStream(rootProject.file("local.properties")))
            tileProviderUrl = localProperties["tileProviderUrl"].toString()
            tileProviderApiKey = localProperties["tileProviderApiKey"].toString()
            searchProviderUrl = localProperties["searchProviderUrl"].toString()
            searchProviderApiKey = localProperties["searchProviderApiKey"].toString()
        } catch (e: Exception) {
            println("Failed to load local.properties for tile and search providers: $e")
        }
        buildConfigField("String", "TILE_PROVIDER_URL", "\"${tileProviderUrl}\"")
        buildConfigField("String", "TILE_PROVIDER_API_KEY", "\"${tileProviderApiKey}\"")
        buildConfigField("String", "SEARCH_PROVIDER_URL", "\"${searchProviderUrl}\"")
        buildConfigField("String", "SEARCH_PROVIDER_API_KEY", "\"${searchProviderApiKey}\"")

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

    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java", "src/testFixtures/java")
            assets.srcDirs("src/debug/assets")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java", "src/testFixtures/java")
            assets.srcDirs("src/androidTest/assets", "src/debug/assets")
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

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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


    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.ui.test.junit4)
    testImplementation(libs.androidx.core.testing)
    testImplementation(testFixtures(project(":app")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation (libs.kotlin.test.junit)
    testImplementation(libs.junit.jupiter)

    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(testFixtures(project(":app")))
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

    // Screenshots for tests
    //screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.uiautomator)

    // Regression file handling for tests
    androidTestImplementation(libs.androidx.media3.common)
//    androidTestImplementation(libs.androidx.media3.common.ktx)

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

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.symbol.processing.api)

    // Markdown to HTML converter for Help screens
    implementation(libs.commonmark)

    // PMTiles reading libraries
    implementation(libs.pmtilesreader)

    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.ui.test.junit4)
    testFixturesImplementation(libs.androidx.media3.common)
    testFixturesImplementation(libs.androidx.junit.v121)
    testFixturesImplementation(libs.ui)
    testFixturesImplementation(libs.material3)
    testFixturesImplementation(libs.androidx.navigation.compose)
    testFixturesImplementation(libs.junit)
}

fun adbPath(): String {
    // Get the Android SDK path directly from Gradle
    val sdkDir = project.extensions
        .getByType<com.android.build.gradle.BaseExtension>()
        .sdkDirectory
        .absolutePath
    val adbExtension = if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        ".exe"
    } else {
        ""
    }
    return "$sdkDir/platform-tools/adb$adbExtension"
}

// NOTE 2025-11-18 Hugh Greene: It's hacky to hard-code the "androidTest" source set name here, but
// there's no easy way to get it.
val composeBaselinesTempTargetDir = "${project.layout.buildDirectory.get()}/tmp/androidTest-baselines"

// We deliberately do not declare this as a Task output, otherwise the Gradle may hold the file
// open for monitoring, preventing it from being deleted.
val composeBaselinesTarFile = File(composeBaselinesTempTargetDir, "baselines.tar")

tasks.register<Exec>("pullComposeBaselines") {

    // Need to create these at configuration time, not execution time, so we can set standardOutput.
    Path(composeBaselinesTempTargetDir).createDirectories()
    composeBaselinesTarFile.createNewFile()

    doFirst {
        println("Pulling Compose baseline snapshots from emulator to '$composeBaselinesTarFile'")
    }

    isIgnoreExitValue = false
    standardOutput = composeBaselinesTarFile.outputStream()
    // Use adb to pull the whole folder as a TAR file.
    commandLine(adbPath(), "exec-out",
        "run-as",
        project.android.namespace,
        "tar",
        "-C",
        "files",
        "-cf",
        "-",
        "baselines"
    )
}

tasks.register<Copy>("extractComposeBaselines") {
    val pullTask = tasks.findByName("pullComposeBaselines")!!
    dependsOn(pullTask)

    doFirst {
        println(this.taskDependencies.toString())
    }

    from(tarTree(composeBaselinesTarFile))
    // NOTE 2025-11-18 Hugh Greene: It's hacky to hard-code the path here, but I don't know of a
    // better way to get it.
    val localTargetDir = "$projectDir/src/debug/assets/baselines"
    into(localTargetDir)
    // Remove leading 'baselines/' from entries if desired
    eachFile {
        // Strip the top-level "baselines" folder
        if (relativePath.segments.firstOrNull() == "baselines") {
            relativePath = RelativePath(
                relativePath.isFile,
                *relativePath.segments.drop(1).toTypedArray()
            )
        }
    }
    // Normalise line-endings of expected text files on Windows.
    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        filesMatching(listOf("**/*.txt")) {
            // The filter closure is called with the line contents stripped of any platform's line
            // endings, and the return value has local line endings added, so we just return "it".
            filter { it }
        }
    }
    includeEmptyDirs = false

    doLast {
        if (composeBaselinesTarFile.exists()) {
            composeBaselinesTarFile.delete()
        }
        println("Baselines moved from emulator to '$localTargetDir'. " +
                "You can now review the changes and commit them.")
    }
}

tasks.configureEach {
    if (this is AndroidTestTask) {
       finalizedBy("extractComposeBaselines")
    }
}

tasks.withType<Test> {
    systemProperty("test.baselineOutputDir", layout.buildDirectory.dir("outputs/robolectric-baselines").get().asFile.absolutePath)
}

