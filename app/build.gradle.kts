// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
    id("jacoco")

}
jacoco{
    toolVersion = "0.8.10"
}

android {
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
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            enableAndroidTestCoverage = true
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.useJUnitPlatform() }
        animationsDisabled = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}
dependencies {
    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)

    // Hilt (KSP)
    implementation(libs.hilt.android)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.play.services.auth)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Otros
    implementation(libs.androidx.lifecycle.viewmodel.navigation3.android)
    implementation(libs.androidx.adaptive)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.facebook.login)

    // Navigation 3 (experimental)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Serialization
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(kotlin("test"))


    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(kotlin("test"))

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Kotlin
tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}
tasks.register<JacocoReport>("mergedDebugCoverage") {
    group = "verification"
    description = "Genera cobertura combinada (unit + androidTest) con API moderna"

    dependsOn("testDebugUnitTest")
    dependsOn("connectedDebugAndroidTest")

    // 🔽 directorios de clases
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(
            "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
            "**/*MembersInjector*.*", "**/*_Factory*.*", "**/*_Hilt*.*",
            "**/*_Component*.*", "**/*_Impl*.*", "**/hilt_*/**", "**/*Binding*.*"
        )
    }
    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(
            "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
            "**/*MembersInjector*.*", "**/*_Factory*.*", "**/*_Hilt*.*",
            "**/*_Component*.*", "**/*_Impl*.*", "**/hilt_*/**", "**/*Binding*.*"
        )
    }
    classDirectories.setFrom(kotlinClasses, javaClasses)

    // 🔽 directorios de código fuente
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    // 🔽 archivos de cobertura: usa layout.buildDirectory
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec",
                "outputs/code_coverage/connected/**/coverage.ec"
            )
        }
    )

    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/mergedDebug"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/mergedDebug.xml"))
        csv.required.set(false)
    }
}