plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI defaults are DEMONSTRATION-ONLY. Supply real IDs before publishing.
val admobAppId = providers.environmentVariable("BOLETO_ADMOB_APP_ID")
    .orElse(providers.gradleProperty("boletoAdmobAppId"))
    .orNull ?: "ca-app-pub-3940256099942544~3347511713"
val rewardedUnitId = providers.environmentVariable("BOLETO_REWARDED_UNIT_ID")
    .orElse(providers.gradleProperty("boletoRewardedUnitId"))
    .orNull ?: "ca-app-pub-3940256099942544/5224354917"

// The upload keystore is intentionally NOT committed or generated inside CI.
// It must be supplied by the publisher through encrypted GitHub Secrets or
// a secure local environment outside the repository.
val keystorePath = providers.environmentVariable("BOLETO_UPLOAD_KEYSTORE_PATH").orNull
val keystorePassword = providers.environmentVariable("BOLETO_UPLOAD_STORE_PASSWORD").orNull
val signingAlias = providers.environmentVariable("BOLETO_UPLOAD_KEY_ALIAS").orNull
val keyPassword = providers.environmentVariable("BOLETO_UPLOAD_KEY_PASSWORD").orNull
val signingConfigured = listOf(keystorePath, keystorePassword, signingAlias, keyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "br.com.proximaera.boletorush"
    compileSdk = 36
    defaultConfig {
        applicationId = "br.com.proximaera.boletorush"
        minSdk = 26
        targetSdk = 36
        versionCode = 52
        versionName = "2.3.15"
        resValue("string", "admob_app_id", admobAppId)
        resValue("string", "admob_rewarded_unit_id", rewardedUnitId)
    }
    signingConfigs {
        if (signingConfigured) {
            create("playUpload") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword!!
                keyAlias = signingAlias!!
                keyPassword = keyPassword!!
            }
        }
    }
    buildTypes {
        getByName("debug") {
            // Install QA alongside the Play-signed app without replacing player data.
            // Never use a real AdMob account in debug, even when release secrets
            // happen to be supplied to the same Gradle invocation.
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa.debug"
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_rewarded_unit_id", "ca-app-pub-3940256099942544/5224354917")
        }
        getByName("release") {
            if (signingConfigured) {
                signingConfig = signingConfigs.getByName("playUpload")
            }
        }
    }
    buildFeatures { compose = true; resValues = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

/**
 * Safe to run in local QA and CI even before the publisher has a keystore.
 * This guards against accidentally shipping the QA package or '-qa' version.
 */
tasks.register("verifyPlayIdentity") {
    group = "verification"
    description = "Verify production package identity and keep QA suffix isolated to debug."
    doLast {
        val releaseId = android.defaultConfig.applicationId.orEmpty() +
            android.buildTypes.getByName("release").applicationIdSuffix.orEmpty()
        val baseName = android.defaultConfig.versionName.orEmpty()
        val releaseName = baseName +
            android.buildTypes.getByName("release").versionNameSuffix.orEmpty()
        val qaName = baseName +
            android.buildTypes.getByName("debug").versionNameSuffix.orEmpty()
        if (releaseId != "br.com.proximaera.boletorush" ||
            releaseName.isBlank() ||
            releaseName.contains("-qa", ignoreCase = true) ||
            releaseName.contains("debug", ignoreCase = true) ||
            !qaName.endsWith("-qa.debug")) {
            throw GradleException(
                "Incorrect Play identity. Release must use the production package " +
                "and a clean versionName; debug must retain the -qa.debug suffix."
            )
        }
        println("Play identity verified: $releaseId / $releaseName; QA version: $qaName")
    }
}

tasks.register("verifyPlayConfiguration") {
    group = "verification"
    description = "Prevent Play submission with test AdMob IDs or without secure upload signing."
    dependsOn("verifyPlayIdentity")
    doLast {
        if (!signingConfigured) {
            throw GradleException("Upload key not configured. An unsigned AAB is QA-only.")
        }
        val realApp = Regex("^ca-app-pub-[0-9]{16}~[0-9]{10}$")
        val realUnit = Regex("^ca-app-pub-[0-9]{16}/[0-9]{10}$")
        if (!realApp.matches(admobAppId) || !realUnit.matches(rewardedUnitId)
            || admobAppId == "ca-app-pub-3940256099942544~3347511713"
            || rewardedUnitId == "ca-app-pub-3940256099942544/5224354917") {
            throw GradleException("Provide your own AdMob app and rewarded IDs before submitting to Play.")
        }
        if (!file(keystorePath!!).isFile) {
            throw GradleException("Upload keystore file is missing.")
        }
        println("Upload signing and non-demo AdMob ID checks passed.")
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.2")
    implementation("androidx.compose.foundation:foundation:1.7.2")
    implementation("androidx.compose.material3:material3:1.3.0")
    testImplementation("junit:junit:4.13.2")
}

// A release bundle must NEVER bypass the upload-key and real AdMob ID checks.
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn("verifyPlayConfiguration")
}
