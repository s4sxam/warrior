plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tanay.warrior2026"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tanay.warrior2026"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing is optional at config time — only required when building a release.
    // Checking for env vars before creating the config prevents a hard failure
    // during assembleDebug, IDE sync, or gradle wrapper runs where CI secrets
    // are not present.
    val keystorePassword = System.getenv("KEY_STORE_PASSWORD")
    val keyPasswordVal   = System.getenv("KEY_PASSWORD")

    if (keystorePassword != null && keyPasswordVal != null) {
        signingConfigs {
            create("release") {
                storeFile     = file(System.getenv("KEY_STORE_PATH") ?: "warrior-release.jks")
                storePassword = keystorePassword
                keyAlias      = System.getenv("KEY_ALIAS") ?: "warrior"
                keyPassword   = keyPasswordVal
            }
        }
    }

    buildTypes {
        release {
            // Only attach signing config if it was created (i.e. CI secrets present)
            val releaseSigning = runCatching { signingConfigs.getByName("release") }.getOrNull()
            if (releaseSigning != null) signingConfig = releaseSigning
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // kotlinOptions is deprecated in AGP 8.5+ — migrated to compilerOptions
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose    = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}