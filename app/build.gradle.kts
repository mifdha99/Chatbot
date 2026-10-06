plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mesraai.app"
        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }

        release {
            isMinifyEnabled = false

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
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

dependencies {

    // =========================
    // Jetpack Compose
    // =========================

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.ui.tooling.preview)


    // =========================
    // AndroidX
    // =========================

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)


    // =========================
    // Room Database
    // =========================

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Room menggunakan annotationProcessor,
    // jadi KSP tidak diperlukan.
    annotationProcessor(libs.androidx.room.compiler)


    // =========================
    // Coroutines
    // =========================

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)


    // =========================
    // Retrofit + Gemini API
    // =========================

    implementation(libs.retrofit)

    implementation(libs.converter.kotlinx.serialization)

    implementation(libs.kotlinx.serialization.json)


    // =========================
    // OkHttp
    // =========================

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)


    // =========================
    // Unit Tests
    // =========================

    testImplementation(libs.junit)

    testImplementation(libs.androidx.junit)

    testImplementation(libs.androidx.core)

    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.robolectric)

    testImplementation(libs.androidx.compose.ui.test.junit4)


    // =========================
    // Android Tests
    // =========================

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.runner)


    // =========================
    // Debug
    // =========================

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
