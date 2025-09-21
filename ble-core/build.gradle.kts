plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.mpdc4gsr.ble.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    androidComponents {
        beforeVariants { variant ->
            variant.enable = variant.buildType == "release" || variant.buildType == "debug"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Use version catalog instead of hardcoded versions
    api(libs.androidx.appcompat)
    api(libs.eventbus)
    api(libs.utilcode)
    api(libs.gson)
    api(libs.xlog)
    api(libs.nordic.ble)
    api(libs.nordic.ble.ktx)
    
    implementation(files("libs/ini4j-0.5.5.jar"))
}