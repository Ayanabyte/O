import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ayanabyte.one"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ayanabyte.one"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-alpha01"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions += "tvEngine"
    productFlavors {
        create("standard") { dimension = "tvEngine" }
        create("tclHbbtv") {
            dimension = "tvEngine"
            applicationIdSuffix = ".hbbtvdiag5"
            ndk { abiFilters += listOf("armeabi-v7a") }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")

    // TV-specific components for upcoming remote-control flows.
    implementation("androidx.tv:tv-material:1.1.0")

    // Populated only for the TCL experimental flavor by scripts/prepare_tcl_hbbtv.py.
    add("tclHbbtvImplementation", files("libs/lovestv-runtime.jar"))
    debugImplementation("androidx.compose.ui:ui-tooling")
}
