plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    // Removed KAPT - no longer needed without ARouter
    // id("kotlin-kapt")
}

// Removed KAPT configuration - no longer needed without ARouter
// kapt {
//     arguments {
//         arg("AROUTER_MODULE_NAME", project.name)
//     }
// }

android {
    namespace = "com.infisense.usbir"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // targetSdk = libs.versions.targetSdk.get().toInt()  // Deprecated in library modules
        ndkVersion = libs.versions.ndkVersion.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
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
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs", "src/main/jnilibs")
        }
    }
    
    packaging {
        jniLibs {
            // Enhanced native library conflict resolution
            pickFirsts += listOf("**/libc++_shared.so")
            // Exclude corrupted native libraries that can't be stripped
            excludes += listOf(
                "**/libSRImage.so",     // Corrupted ELF header - exclude to prevent stripping errors
                "**/liblog.so",         // System library - handled by OS
                "**/libopen3d.so",      // Third-party library with stripping issues
                "**/libopencv_java4.so" // OpenCV library - large and causes stripping issues
            )
            // Keep debug symbols for remaining native libraries
            keepDebugSymbols += listOf("**/*.so")
        }
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // All AAR dependencies as compileOnly for compilation but not packaging (runtime provided by app module)
    // This fixes AGP 8.0+ restrictions on local AAR files in library modules
    compileOnly(files("libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))  // Required for infisense thermal camera classes
    compileOnly(files("libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))  // AC020 SDK for thermal-lite functionality
    compileOnly(files("libs/libirutils_1.2.0_2409241055.aar"))  // IR utilities for thermal-lite
    compileOnly(files("libs/opengl_1.3.2_standard.aar"))  // OpenGL functionality
    compileOnly(files("libs/suplib-release.aar"))  // Required for thermal-lite iruvc classes
    compileOnly(files("libs/ai-upscale-release.aar"))  // AI upscale functionality
    compileOnly(files("libs/texturegesture-release.aar"))  // Texture gesture functionality
    compileOnly(files("libs/jetified-tas_api-1.0.4.0.aar"))  // TAS API
    compileOnly(files("libs/library_1.0.aar"))  // Additional library support
    
    // Enhanced IR-specific dependencies from user's Deps object
    api("com.conghuahuadan:superlayout:1.1.0")
    api(libs.ir.layout)  // IR layout utilities from CoderCaiSL jitpackMvn
    api(libs.andromeda.core)  // Andromeda core for sensor functionality
    api(libs.andromeda.sense)  // Andromeda sense for compass and sensors
    api(libs.javacv)  // JavaCV for IR image processing
    api(libs.javacpp)  // JavaCV native dependencies
    
    implementation(project(":libapp"))
    
    // Add unified BLE module for comprehensive Shimmer Nordic and Topdon BLE support
    implementation(project(":BleModule"))
    
    // libcommon needs to be available directly to libir since it uses SurfaceNativeWindow
    // Changed to compileOnly since library modules cannot include AAR dependencies directly
    compileOnly(files("../app/libs/libcommon_1.2.0_24052117.aar"))
    
    // Test dependencies for Robolectric testing
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
}