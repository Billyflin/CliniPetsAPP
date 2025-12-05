// app/build.gradle.kts

import java.net.URI
import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.hilt)
    alias(libs.plugins.secrets.gradle.plugin)
    kotlin("kapt")
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
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

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

val openApiSpecFile = layout.buildDirectory.file("openapi-spec.json")

// 2. Create a task to download the file from your Home Server
tasks.register("downloadOpenApiJson") {
    // We declare the URL as an input so Gradle knows if it changes (optional but good practice)
    inputs.property("url", "http://homeserver.local:8080/v3/api-docs")
    // We declare the file as an output so Gradle caches it
    outputs.file(openApiSpecFile)

    doLast {
        println("⬇️ Downloading OpenAPI Spec from Home Server...")
        val url = URI("http://homeserver.local:8080/v3/api-docs").toURL()
        // Use outputs.files.singleFile to avoid capturing the outer 'openApiSpecFile' variable
        val file = outputs.files.singleFile
        
        url.openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        println("✅ Download complete: ${file.absolutePath}")
    }
}
tasks.named("openApiGenerate") {
    dependsOn("downloadOpenApiJson")
}
// Configuración de generación OpenAPI
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(openApiSpecFile.get().asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generate-resources/main").get().asFile.absolutePath)
    apiPackage.set("cl.clinipets.openapi.apis")
    modelPackage.set("cl.clinipets.openapi.models")
    packageName.set("cl.clinipets.openapi")
    invokerPackage.set("cl.clinipets.openapi.infrastructure")

    validateSpec.set(false)

    configOptions.set(
        mapOf(
            "library" to "jvm-retrofit2",
            "serializationLibrary" to "gson",
            "generateOneOfAnyOfWrappers" to "true",
            "idea" to "true",
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

    // Imágenes en Compose
    implementation(libs.coil.compose)

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
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0") // TODO: Replace with latest version

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)

    // Persistencia ligera
    implementation(libs.datastore.preferences)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.firebase.crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    kapt(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // TODO: Check for latest version

    testImplementation(libs.junit)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(kotlin("test"))
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

tasks.named("preBuild") {
    dependsOn(tasks.named("openApiGenerate"))
}
