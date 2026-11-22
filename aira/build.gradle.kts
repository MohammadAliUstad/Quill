@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.yugentech.quill.aira"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(project(":database"))
    implementation(project(":domain"))

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.onnxruntime.android)
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    implementation(libs.androidx.core.ktx)
}
