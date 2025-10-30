// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

android {

    signingConfigs {
        create("clinipets") {
        }
    }
    namespace = "cl.clinipets"
    compileSdk = 36

    // Lee GOOGLE_SERVER_CLIENT_ID una sola vez y úsalo en ambos tipos de build
    val googleServerClientId: String = providers.gradleProperty("GOOGLE_SERVER_CLIENT_ID").orNull
        ?: System.getenv("GOOGLE_SERVER_CLIENT_ID")
        ?: ""

    // Lee BASE_URL_DEBUG (para debug) con fallback a la IP indicada por el usuario
    val baseUrlDebug: String = providers.gradleProperty("BASE_URL_DEBUG").orNull
        ?: System.getenv("BASE_URL_DEBUG")
        ?: "https://clinipets.cl/"

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
            buildConfigField("String", "BASE_URL", "\"$baseUrlDebug\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.clinipets.example/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
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
    // Incluye fuentes generadas por OpenAPI en el source set principal
    sourceSets {
        getByName("main") {
            val genSrc =
                layout.buildDirectory.dir("generate-resources/main/src/main/kotlin").get().asFile
            java.srcDir(genSrc)
        }
    }
}

// Configuración de generación OpenAPI
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(project.file("$rootDir/backend-openapi.yaml").toURI().toString())
    // La salida real utilizada por el generador en tu entorno
    outputDir.set(layout.buildDirectory.dir("generate-resources/main").get().asFile.absolutePath)
    apiPackage.set("cl.clinipets.backend.openapi.apis")
    modelPackage.set("cl.clinipets.backend.openapi.models")
    packageName.set("cl.clinipets.backend.openapi")
    invokerPackage.set("cl.clinipets.backend.openapi.infrastructure")

    validateSpec.set(false)

    configOptions.set(
        mapOf(
            "library" to "jvm-retrofit2",
            "dateLibrary" to "java8",
            "serializationLibrary" to "gson",
            "useTags" to "false",
            "useOperationIdAsMethodName" to "false",
            "enumPropertyNaming" to "UPPERCASE",
            "nullableReferenceTypes" to "true",
            "useCoroutines" to "true",
            "useResponseAsReturnType" to "true",
            "sourceFolder" to "src/main/kotlin"
        )
    )
    skipValidateSpec.set(true)
    generateAliasAsModel.set(false)
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
    // Gson para Retrofit (cliente generado y legacy)
    implementation(libs.retrofit2.gson)

    implementation("com.google.crypto.tink:tink-android:1.13.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Seguridad para JWT storage
    implementation(libs.security.crypto)

    // Google Sign-In con Credential Manager (usa catálogo)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

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

tasks.named("preBuild") {
    dependsOn(tasks.named("openApiGenerate"))
}