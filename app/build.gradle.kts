plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

import java.util.Properties

// Built-in search keys are injected at build time from local.properties (populated by CI secrets
// or a local local.properties). They are then seeded into DataStore on first launch so the app
// works out-of-the-box, but remain fully editable in the in-app Settings screen.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}
fun localProp(name: String): String =
    (localProperties[name] as? String)?.takeIf { it.isNotBlank() } ?: ""

android {
    namespace = "com.huqi.noveltracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.huqi.noveltracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 49
        versionName = "0.13.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Built-in search keys are injected at build time from local.properties (CI secrets),
        // then seeded into DataStore on first launch so the app works without manual setup.
        // They remain fully editable in the in-app Settings screen. The literal string below
        // is empty unless local.properties provides a value, so the public source has no key.
        buildConfigField("String", "SILICONFLOW_API_KEY", "\"${localProp("siliconflow.api.key")}\"")
        buildConfigField("String", "TAVILY_API_KEY", "\"${localProp("tavily.api.key")}\"")
    }

    signingConfigs {
        // AGP already creates a default "debug" config; override it with our
        // committed keystore so every CI build shares the same signature and
        // the app can be updated by overwriting instead of uninstalling first.
        // (Debug key only; not a release/secret key.)
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Image loading (covers)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // OCR: ML Kit Chinese text recognition (on-device, offline)
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0-beta3")

    // Networking (for real novel-search API later)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON helper for converters
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Settings (DataStore) for runtime API key / provider configuration
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
