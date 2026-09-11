import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

// Local-only release signing. Credentials live in an untracked keystore.properties at the repo root
// (never committed). If that file is absent (e.g. a fresh clone or CI), the release build stays
// unsigned instead of failing — no secrets are ever hardcoded here.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "com.hoctin.gymtracker"
    // Health Connect 1.1.0's published AAR requires API 36 at compile time. targetSdk remains 35,
    // so this does not opt the app into new Android 16 runtime behavior.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hoctin.gymtracker"
        minSdk = 26
        targetSdk = 35
        // WEARABLE-2A split-sleep selection repair; upgrades release 1.1 (2) in place.
        versionCode = 3
        versionName = "1.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use the local release key when keystore.properties is present; otherwise leave the
            // release APK unsigned so the build still works without local credentials.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // Local performance-testing build. Non-debuggable (like release) so ART can fully optimize
        // the app -> this is the build to compare real-device smoothness against the debug APK.
        // Signed with the debug key so it installs by sideload without Google Play, and shares the
        // same applicationId/key as the debug build so it can replace it while keeping local data.
        create("profile") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
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

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

kapt {
    correctErrorTypes = true
    // Export Room schemas (DB-0 onward) so catalog/table migrations can be verified against the
    // generated schema JSON. Written to app/schemas/<db>/<version>.json.
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.lottie.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // WEARABLE-2A: stable, read-only Health Connect client. No Samsung-specific SDK and no
    // background/history capability are included.
    implementation(libs.androidx.health.connect.client)

    // Test scope only — not shipped in the APK. Used to verify the deterministic
    // ProgressiveOverloadEngine V2 rules (Task 2.4b).
    testImplementation("junit:junit:4.13.2")
    // DB-4B.2: real org.json on the unit-test classpath (Android stubs it to throw) so
    // CatalogJsonLoader can parse the bundled FED catalog asset in CatalogSearchTest.
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
