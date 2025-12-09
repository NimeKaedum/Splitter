plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    //id("com.google.gms.google-services")
}

android {
    namespace = "com.example.livesplitlike"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.livesplitlike"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // --- Core ---
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android) // versión ejemplo
    // --- Lifecycle / ViewModel / SavedState ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // --- Jetpack Compose (usa BOM o versiones estables) ---
    implementation(platform(libs.androidx.compose.bom.v20240800)) // ejemplo
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // --- Room (DB local) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Hilt (DI) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.hilt.navigation.compose)

    // --- Firebase (Auth, Firestore, Storage) usando BoM ---
    //implementation(platform(libs.firebase.bom)) // ejemplo
    //implementation(libs.firebase.auth.ktx)
    //implementation(libs.firebase.firestore.ktx)
    //implementation(libs.firebase.storage.ktx)
    //implementation(libs.play.services.auth) // Google Sign-In

    // --- Reconocimiento de voz: no requiere dependencia (SpeechRecognizer está en SDK) ---
    // Si optas por ML Kit o STT externo, añade su SDK aquí.

    // --- Otras utilidades ---
    implementation(libs.accompanist.permissions) // permisos runtime compose (opcional)
}
