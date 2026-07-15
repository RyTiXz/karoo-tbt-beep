import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val env: MutableMap<String, String> = System.getenv()
val hasReleaseKeystore = !env["KEYSTORE_BASE64"].isNullOrBlank()

android {
    namespace = "io.github.rytixz.tbtbeep"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.rytixz.tbtbeep"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "0.5.0"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                keyAlias = env["KEY_ALIAS"]
                keyPassword = env["KEY_PASSWORD"]
                val keystoreFile: File = File.createTempFile("keystore", ".jks")
                keystoreFile.writeBytes(Base64.getDecoder().decode(env["KEYSTORE_BASE64"]))
                storeFile = keystoreFile
                storePassword = env["KEYSTORE_PASSWORD"]
            }
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Ohne Release-Keystore (KEYSTORE_BASE64 env) wird mit dem Debug-Key signiert
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.hammerhead.karoo.ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifeycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
}
