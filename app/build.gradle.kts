plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ── [FIX BUG 7] Version sourced from CI environment ──────────────────────────
//
// Previously versionCode and versionName were hardcoded constants. If a developer
// pushed a new GitHub release tag (e.g. "v2.3.0") but forgot to update this file,
// the uploaded APK would still report "2.2.0" and the isNewer() check would never
// trigger for existing users — they'd never see the update dialog.
//
// Fix: Read VERSION_NAME and VERSION_CODE from environment variables set by CI.
// The GitHub Actions workflow sets these from the git tag before building.
//
// Local development fallback: if the env vars are absent (e.g. IDE sync, debug
// build on a dev machine), the hardcoded defaults below are used so nothing breaks.
//
// How to use in CI (.github/workflows/build.yml):
//   - name: Build Release APK
//     env:
//       VERSION_NAME: ${{ github.ref_name }}          # e.g. "v2.3.0"
//       VERSION_CODE: ${{ github.run_number }}         # auto-increments each run
//       KEY_STORE_PATH: warrior-release.jks
//       KEY_STORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
//       KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
//       KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
//     run: ./gradlew assembleRelease --no-daemon
// ─────────────────────────────────────────────────────────────────────────────

val ciVersionName = System.getenv("VERSION_NAME")
    ?.removePrefix("v")   // strip leading "v" from git tags like "v2.3.0"
    ?.trim()
    ?: "2.3.0"            // local fallback — bump this when releasing manually

val ciVersionCode = System.getenv("VERSION_CODE")
    ?.toIntOrNull()
    ?: 5                  // local fallback — keep ahead of last published versionCode

android {
    namespace = "com.tanay.warrior"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tanay.warrior"
        minSdk = 26
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = ciVersionName

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
    implementation(libs.androidx.glance.appwidget)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}