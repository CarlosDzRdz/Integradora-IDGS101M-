plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.utch.vendeta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.utch.vendeta"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ── ML Kit (escáner QR) ───────────────────────────────────────────────────
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── Firebase BOM — controla versiones de todos los módulos Firebase ───────
    // El BOM garantiza que Auth, Firestore y Analytics sean compatibles entre sí.
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))

    // Con BOM NO se especifica versión en cada módulo — la maneja el BOM.
    implementation("com.google.firebase:firebase-auth")       // Authentication
    implementation("com.google.firebase:firebase-firestore")  // Cloud Firestore
    implementation("com.google.firebase:firebase-analytics")  // Analytics (opcional)

    // ── ViewModel para Compose (sobrevive rotación) ───────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // ── AndroidX / Compose ────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ── Tests ─────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}