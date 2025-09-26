plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.mpdc4gsr.ble"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    implementation(libs.identity.jvm)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api("androidx.appcompat:appcompat:1.2.0")
    api("org.greenrobot:eventbus:3.2.0")
    api("com.blankj:utilcodex:1.31.1")
    api("com.google.code.gson:gson:2.13.2")
    api("com.elvishew:xlog:1.10.1")
    api("no.nordicsemi.android:ble:2.11.0")
    api(libs.nordic.ble.ktx)
    implementation(files("libs/ini4j-0.5.5.jar"))
}
