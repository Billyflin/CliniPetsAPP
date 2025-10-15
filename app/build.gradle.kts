// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
android {
    signingConfigs {
        create("clinipets") {
        }
    }
    namespace = "cl.clinipets"
    compileSdk = 36

    defaultConfig {
        applicationId = "cl.clinipets"
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")

        // Inyecta la API Key de Google Maps desde local.properties o variable de entorno
        val mapsApiKey = providers.gradleProperty("MAPS_API_KEY").orNull
            ?: System.getenv("MAPS_API_KEY")
            ?: ""
        manifestPlaceholders += mapOf("MAPS_API_KEY" to mapsApiKey)
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.13:8080/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"REEMPLAZA_CON_CLIENT_ID_WEB\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.clinipets.example/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"REEMPLAZA_CON_CLIENT_ID_WEB\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)

    // Coroutines y Serialization
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)

    // Networking
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.scalars)
    implementation(libs.retrofit.kotlinx.serialization)

    // Seguridad para JWT storage
    implementation(libs.security.crypto)

    // Google Sign-In con Credential Manager
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.play.services.base)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)

    // Persistencia ligera
    implementation(libs.datastore.preferences)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp3.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(kotlin("test"))
}