plugins {
    id("com.android.library")
    kotlin("android")
    // Temporarily disable kapt to fix compilation issues
    // kotlin("kapt")
    id("kotlin-parcelize")
}

// Temporarily disable kapt configuration
// kapt {
//     arguments {
//         arg("AROUTER_MODULE_NAME", project.name)
//     }
//     // Enable Kotlin 2.1.0 compatibility
//     correctErrorTypes = true
//     useBuildCache = true
// }

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
    implementation(project(":component:CommonComponent"))  // Required for CommonComponent classes
    // Removed libmenu dependency - functionality consolidated into libui
    
    // ARouter removed - now using NavigationManager instead
    // implementation(libs.arouter.api)
    // kapt(libs.arouter.compiler)
    
    // Use shared UI bundle for common dependencies
    implementation(libs.bundles.ui.common)
    implementation(libs.utilcode)
    implementation(libs.mn.image.browser)
    
    // AAR dependencies - required for thermal camera functionality
    implementation(files("../../libir/libs/suplib-release.aar"))  // Required for SupHelp class
    implementation(files("../../libir/libs/ai-upscale-release.aar"))  // AI upscale functionality
    implementation(files("../../libir/libs/texturegesture-release.aar"))  // Texture gesture functionality
    implementation(files("../../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))  // Required for IRCMD classes
    implementation(files("../../libir/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))  // AC020 SDK 
    implementation(files("../../libir/libs/libirutils_1.2.0_2409241055.aar"))  // IR utilities
    implementation(files("../../shared/libs/lms_international-3.90.009.0.aar"))  // LMS SDK for thermal-ir classes
}
