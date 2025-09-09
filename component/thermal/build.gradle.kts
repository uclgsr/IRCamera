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
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
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
    implementation(project(":libmenu"))  // Required for MenuFirstTabView
    implementation(project(":libmatrix"))  // Required for GuideInterface and IrSurfaceView
    // Removed CommonComponent dependency - module removed as unused
    implementation(project(":component:pseudo"))  // Required for CustomPseudoBean class
    // Note: Cannot add thermal-ir or thermal-lite due to circular dependencies
    
    // ARouter removed - now using NavigationManager instead
    // implementation(libs.arouter.api)
    // kapt(libs.arouter.compiler)
    
    // Use shared UI bundle for common dependencies
    implementation(libs.bundles.ui.common)
    implementation(libs.utilcode)
    implementation(libs.mn.image.browser)
    
    // AAR dependencies moved to app module to avoid library AAR packaging issues
    // These will be provided by the app module at runtime
    compileOnly(files("../../libir/libs/suplib-release.aar"))  // Required for SupHelp class
    compileOnly(files("../../libir/libs/ai-upscale-release.aar"))  // AI upscale functionality
    compileOnly(files("../../libir/libs/texturegesture-release.aar"))  // Texture gesture functionality
    compileOnly(files("../../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))  // Required for IRCMD classes
    compileOnly(files("../../libir/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))  // AC020 SDK 
    compileOnly(files("../../libir/libs/libirutils_1.2.0_2409241055.aar"))  // IR utilities
    compileOnly(files("../../shared/libs/lms_international-3.90.009.0.aar"))  // LMS SDK for thermal-ir classes
}
