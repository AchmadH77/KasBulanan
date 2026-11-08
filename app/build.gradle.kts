import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android
// plugin is no longer applied; the JVM target moves to this block.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Signing uses ONE fixed keystore (kasbulan.p12 at the repo root, restored
// in CI from the KASBULAN_KEYSTORE_BASE64 secret) so every build can be
// installed over the previous one. Until the secret exists, builds fall
// back to the debug key.
val ciKeystoreExists = rootProject.file("kasbulan.p12").exists()

android {
    namespace = "id.kasbulan.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.kasbulan.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("ci") {
            storeFile = rootProject.file("kasbulan.p12")
            storePassword = "kasbulan"
            keyAlias = "kasbulan"
            keyPassword = "kasbulan"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            if (ciKeystoreExists) signingConfig = signingConfigs.getByName("ci")
            // R8 is on for debug too: the everyday installed APK is the
            // minified build, so shrinking problems show up early.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            if (ciKeystoreExists) signingConfig = signingConfigs.getByName("ci")
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    // Local database (Room) - all data stays on the phone, no server.
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for BackupHelper unit tests (Android provides it on device).
    testImplementation("org.json:json:20240303")
}
