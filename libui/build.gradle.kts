plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize") // Use modern kotlin-parcelize instead of kotlin-android-extensions for Parcelable
}

kapt {
    arguments {
        // Removed AROUTER_MODULE_NAME - migrating to NavigationManager
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
    // Enable Kotlin 2.1.0 compatibility
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.topdon.lib.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // targetSdk = libs.versions.targetSdk.get().toInt()  // Deprecated in library modules

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    // Configure single release variant for easier maintenance
    androidComponents {
        beforeVariants { variant ->
            // Only enable release variant for single-developer maintenance
            variant.enable = variant.buildType == "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Project dependencies
    implementation(project(":libapp"))
    implementation(project(":libmenu"))  // Required for menu references in widget files

    // Use shared UI bundle instead of individual dependencies
    implementation(libs.bundles.ui.common)
    
    // Smart Refresh Layout for LoadingFooter - temporarily commented out due to jitpack.io issues
    // implementation(libs.bundles.smart.refresh)
}