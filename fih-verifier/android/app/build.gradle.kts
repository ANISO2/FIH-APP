import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Load the release signing config from android/key.properties. That file holds
// the keystore passwords and is kept OUT of version control (see
// android/.gitignore). If it is absent on this machine (e.g. a dev box that only
// ever builds debug), we fall back to debug signing further below so
// `flutter run --release` still works.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.bitaka.fih_verifier"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.bitaka.fih_verifier"
        // mobile_scanner 7.x AND audioplayers 6.x both require Android API 23
        // (Marshmallow) as their minimum. Flutter's default (flutter.minSdkVersion)
        // is 21, which makes the manifest-merger / AAR-metadata check FAIL at build
        // time. Pin to 23 so a release build actually compiles.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Derived from pubspec.yaml `version:` (currently 1.0.0+1). Bump there.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        // Only defined when key.properties is present, so the build never fails
        // just because a dev machine has no keystore.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?
                storeFile = (keystoreProperties["storeFile"] as String?)?.let { file(it) }
                storePassword = keystoreProperties["storePassword"] as String?
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (keystorePropertiesFile.exists()) {
                // Real release signing with the upload/release key.
                signingConfigs.getByName("release")
            } else {
                // No key.properties on this machine: fall back to debug signing so
                // `flutter run --release` still works during development. A real
                // DISTRIBUTABLE build MUST be produced on a machine that has
                // android/key.properties + the keystore.
                signingConfigs.getByName("debug")
            }
            // NOTE: R8/resource shrinking is intentionally left OFF. If you ever
            // enable isMinifyEnabled, add ML Kit keep rules or barcode scanning
            // can break in release.
        }
    }
}

flutter {
    source = "../.."
}
