plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Play uploads need a signed build. CI supplies the keystore through the environment; local and
// debug builds keep working without one.
val playKeystore = System.getenv("BOOKREADER_KEYSTORE_PATH")
    ?.takeIf { it.isNotBlank() }
    ?.let(::File)
    ?.takeIf(File::exists)

// Voice cloning and cloud OCR call the project's own backend. CI sets
// BOOKREADER_API_BASE_URL and BOOKREADER_API_KEY so testers do not type Settings.
fun envOrProperty(env: String, property: String, default: String = ""): String =
    (findProperty(property) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: default

fun buildConfigQuoted(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun trailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

val packedApiBaseUrl = envOrProperty("BOOKREADER_API_BASE_URL", "apiBaseUrl")
    .ifBlank { envOrProperty("BOOKREADER_API_BASE_URL", "releaseApiBaseUrl") }
val packedApiKey = envOrProperty("BOOKREADER_API_KEY", "apiKey")
val releaseApiBaseUrl = trailingSlash(packedApiBaseUrl.ifBlank { "https://api.example.com/" })
val debugApiBaseUrl = trailingSlash(packedApiBaseUrl.ifBlank { "http://10.0.2.2:8080/" })

android {
    namespace = "com.multilingualbookreader"
    // Play requires new apps and updates to target Android 16 from 31 August 2026.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.multilingualbookreader"
        minSdk = 26
        targetSdk = 36
        // Bump versionCode whenever testers should receive an in-app update.
        versionCode = 27
        versionName = "0.3.13"
        testInstrumentationRunner = "com.multilingualbookreader.HiltTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // Checked in on purpose. Gradle's default debug key is generated per machine, so test
        // builds from a new CI runner or a rebuilt workstation could not update an existing
        // install: Android rejects a signer change with "package conflicts with an existing
        // package". This is a throwaway test key and must never be the Play upload key.
        getByName("debug") {
            storeFile = file("svara-debug.jks")
            storePassword = "svaradebug"
            keyAlias = "svara-debug"
            keyPassword = "svaradebug"
        }
        if (playKeystore != null) {
            create("play") {
                storeFile = playKeystore
                storePassword = System.getenv("BOOKREADER_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BOOKREADER_KEY_ALIAS")
                keyPassword = System.getenv("BOOKREADER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGS", "true")
            buildConfigField("boolean", "SELF_INSTALL_SUPPORTED", "true")
            buildConfigField("String", "API_BASE_URL", buildConfigQuoted(debugApiBaseUrl))
            buildConfigField("String", "API_KEY", buildConfigQuoted(packedApiKey))
            buildConfigField("String", "UPDATE_OWNER", "\"thrinadsanjay\"")
            buildConfigField("String", "UPDATE_REPO", "\"MultilingualBookReader\"")
        }
        release {
            signingConfigs.findByName("play")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGS", "false")
            // Release builds omit REQUEST_INSTALL_PACKAGES, so updates always come from Play.
            buildConfigField("boolean", "SELF_INSTALL_SUPPORTED", "false")
            buildConfigField("String", "API_BASE_URL", buildConfigQuoted(releaseApiBaseUrl))
            buildConfigField("String", "API_KEY", buildConfigQuoted(packedApiKey))
            buildConfigField("String", "UPDATE_OWNER", "\"thrinadsanjay\"")
            buildConfigField("String", "UPDATE_REPO", "\"MultilingualBookReader\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // These ship the OCR models inside the app; the Play Services recogniser they depend on then
    // reads them locally instead of waiting for a download. The document scanner was never used.
    implementation(libs.mlkit.text)
    implementation(libs.mlkit.text.devanagari)
    implementation(libs.mlkit.language)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    implementation(libs.pdfbox.android)

    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
