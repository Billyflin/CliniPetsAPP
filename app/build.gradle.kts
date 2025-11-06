// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.hilt)
    alias(libs.plugins.secrets.gradle.plugin)
    kotlin("kapt")
}

android {

    signingConfigs {
        create("clinipets") {
        }
    }
    namespace = "cl.clinipets"
    compileSdk = 36

    // Valores leídos desde Secrets Plugin (gradleProperty), definidos en local.properties o secrets.defaults.properties
    val googleServerClientId: String = providers.gradleProperty("GOOGLE_SERVER_CLIENT_ID").orNull ?: ""
    val baseUrlDebug: String = providers.gradleProperty("BASE_URL_DEBUG").orNull ?: "https://clinipets.cl/"
    val baseUrlRelease: String = providers.gradleProperty("BASE_URL_RELEASE").orNull ?: "https://api.clinipets.example/"

    defaultConfig {
        applicationId = "cl.clinipets"
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")
        // MAPS_API_KEY se resuelve vía Secrets Plugin en el Manifest (${MAPS_API_KEY})
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
            buildConfigField("String", "BASE_URL", "\"$baseUrlRelease\"")
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

// Configuración de Secrets Gradle Plugin
secrets {
    // Usamos un archivo por defecto versionado para evitar fallas cuando local.properties no tiene la clave
    defaultPropertiesFileName = "secrets.defaults.properties"
}

// Configuración de generación OpenAPI
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(project.file("$rootDir/backend-openapi.yaml").toURI().toString())
    // La salida real utilizada por el generador en tu entorno
    outputDir.set(layout.buildDirectory.dir("generate-resources/main").get().asFile.absolutePath)
    apiPackage.set("cl.clinipets.openapi.apis")
    modelPackage.set("cl.clinipets.openapi.models")
    packageName.set("cl.clinipets.openapi")
    invokerPackage.set("cl.clinipets.openapi.infrastructure")

    validateSpec.set(false)

    configOptions.set(
        mapOf(
            "library" to "jvm-retrofit2",
            "dateLibrary" to "java8",
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
