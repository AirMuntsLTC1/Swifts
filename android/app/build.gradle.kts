plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// A fixed signing key keeps the SHA-1 the same on every build. Google sign-in depends on it.
// CI passes these with -P flags from GitHub secrets. Without them, the default debug key is used.
val swiftsKeystore: String? = project.findProperty("SWIFTS_KEYSTORE")?.toString()?.takeIf { it.isNotBlank() }

android {
    namespace = "com.swifts.app"
    compileSdk = 35

    signingConfigs {
        if (swiftsKeystore != null) {
            create("swifts") {
                storeFile = project.file(swiftsKeystore)
                storePassword = project.findProperty("SWIFTS_STORE_PASSWORD")?.toString()
                keyAlias = project.findProperty("SWIFTS_KEY_ALIAS")?.toString()
                keyPassword = project.findProperty("SWIFTS_KEY_PASSWORD")?.toString()
            }
        }
    }

    defaultConfig {
        applicationId = "com.swifts.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.5.0"

        // Phones built for 64-bit ARM only: smaller APK, fewer native libs to package.
        ndk {
            abiFilters += "arm64-v8a"
        }

        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${project.findProperty("SUPABASE_PUBLISHABLE_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
    }

    buildTypes {
        debug {
            if (swiftsKeystore != null) {
                signingConfig = signingConfigs.getByName("swifts")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
}
