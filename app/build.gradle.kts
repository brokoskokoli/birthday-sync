import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release-Signatur aus signing/keystore.properties (lokal, nicht im Repo) oder aus Umgebungsvariablen (CI).
// Fehlt beides, entsteht ein unsigniertes Release-APK – bewusst kein Rückfall auf den Debug-Key.
val signingProps = Properties().apply {
    val f = rootProject.file("signing/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(prop: String, env: String): String? = signingProps.getProperty(prop) ?: System.getenv(env)
val releaseStoreFile = signingValue("storeFile", "SIGNING_STORE_FILE")

android {
    namespace = "io.github.brokoskokoli.birthdaysync"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.brokoskokoli.birthdaysync"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Sprachauswahl pro App (Android 13+): locales_config aus den values-xx-Ordnern, Basissprache in res/resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
    // Fehlende oder kaputte Übersetzungen brechen den Build ab.
    lint {
        error += listOf(
            "MissingTranslation", "ExtraTranslation", "MissingQuantity", "UnusedQuantity",
            "StringFormatInvalid", "StringFormatCount", "StringFormatMatches", "PluralsCandidate",
        )
    }
    testOptions {
        // TranslationsTest prüft die Übersetzungen und Store-Texte direkt in den Quelldateien.
        unitTests.all {
            it.systemProperty("resDir", file("src/main/res").absolutePath)
            it.systemProperty("fastlaneDir", rootProject.file("fastlane/metadata/android").absolutePath)
        }
    }
    // Kein verschlüsselter Abhängigkeits-Block im APK (stört F-Droid/IzzyOnDroid-Prüfungen).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

    testImplementation("junit:junit:4.13.2")
}
