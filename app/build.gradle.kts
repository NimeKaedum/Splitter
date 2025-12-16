plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    ksp {
        arg("jvmTarget", "21")
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
    implementation(libs.kotlinx.coroutines.play.services)

    // --- Lifecycle / ViewModel / SavedState ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // --- Jetpack Compose (usa BOM o versiones estables) ---
    implementation(platform(libs.androidx.compose.bom.v20240800)) // ejemplo
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core:1.7.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.material:material:1.10.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // --- Room (DB local) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Hilt (DI) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("com.google.dagger:hilt-android:2.57.1")
    implementation(libs.androidx.hilt.navigation.compose)
    implementation("com.jakewharton.timber:timber:5.0.1")
    // Check GitHub for the latest version

    // DataStore preferences
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // --- Firebase (Auth, Firestore, Storage) usando BoM ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth) // Google Sign-In
    implementation("io.coil-kt.coil3:coil-compose:3.0.4") // Make sure this matches
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4") // <--- ADD THIS


    // --- Reconocimiento de voz: no requiere dependencia (SpeechRecognizer está en SDK) ---
    // Si optas por ML Kit o STT externo, añade su SDK aquí.

    // --- Otras utilidades ---
    implementation(libs.accompanist.permissions) // permisos runtime compose (opcional)
}

// Configurar el argumento para KSP
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
