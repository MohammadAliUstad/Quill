plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.devtools.ksp") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0"
}