import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.github.triplet.play")
}

play {
    serviceAccountCredentials.set(file("../play-store-key.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}

android {
    bundle {
        language {
            enableSplit = false
        }
    }

    namespace = "com.mtpnda.biocue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtpnda.biocue"
        minSdk = 29
        targetSdk = 36
        versionCode = 50
        versionName = project.findProperty("biocue.versionName").toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("local.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }
            storeFile = file(keystoreProperties["RELEASE_STORE_FILE"] as String? ?: System.getenv("RELEASE_STORE_FILE") ?: "")
            storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String? ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String? ?: System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String? ?: System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize = "2G"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=androidx.credentials.ExperimentalDigitalCredentialApi")
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(11)
}

play {
    serviceAccountCredentials.set(rootProject.file("play-account.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")


    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(project(":core-detection"))
    implementation("androidx.credentials:credentials:1.7.0-alpha03")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha03")
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.mockk)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
dependencies { implementation("androidx.wear:wear-remote-interactions:1.1.0") }

play {
    serviceAccountCredentials.set(rootProject.file("play-account.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}


fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
        process.inputStream.bufferedReader().readText().trim().toInt() + 20 // baseline offset
    } catch (e: Exception) {
        100
    }
}
