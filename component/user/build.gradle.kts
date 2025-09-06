plugins {
    id("com.android.library")
    kotlin("android")
    // Disable kapt since ARouter annotations are commented out in this module
    // kotlin("kapt")
}


// kapt {
//     arguments {
//         // arg("AROUTER_MODULE_NAME", project.name)  // Removed for NavigationManager migration
//     }
//     // Enable Kotlin 2.1.0 compatibility
//     correctErrorTypes = true
//     useBuildCache = true
// }

android {
    namespace = "com.topdon.module.user"
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
    
    // Compile-time access to LMS SDK for user component classes that import LMS classes
    compileOnly(files("../../shared/libs/lms_international-3.90.009.0.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.glide)
    
    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
