plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "br.com.proximaera.boletorush"
    compileSdk = 36
    defaultConfig {
        applicationId = "br.com.proximaera.boletorush"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.2")
    implementation("androidx.compose.foundation:foundation:1.7.2")
    implementation("androidx.compose.material3:material3:1.3.0")
}
