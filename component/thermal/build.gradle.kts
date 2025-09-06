plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
    }
    // Enable Kotlin 2.1.0 compatibility
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.topdon.module.thermal"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // targetSdk removed for library modules - only set in main app module per AGP 8.0+
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    buildFeatures {
        dataBinding = true
        // Enable synthetic views for Kotlin backward compatibility
        viewBinding = true
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))
    
    // ARouter removed - now using NavigationManager instead
    // implementation(libs.arouter.api)
    // kapt(libs.arouter.compiler)
    
    // Use shared UI bundle for common dependencies
    implementation(libs.bundles.ui.common)
    implementation(libs.utilcode)
    implementation(libs.mn.image.browser)
    
    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
