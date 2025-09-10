import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
    }
}

val buildDayStr = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
val buildTimeStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())

android {
    namespace = "com.csl.irCamera"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    defaultConfig {
        applicationId = "com.csl.irCamera"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        ndkVersion = libs.versions.ndkVersion.get()
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        buildConfigField("String", "VERSION_DATE", "\"$buildDayStr\"")
        buildConfigField("String", "SOFT_CODE", "\"TC001_DisplaySW_IRCamera_Adr\"")
        buildConfigField("String", "APP_KEY", "\"5B2F6F1FD80844FCB6E50BCA19222E76\"")
        buildConfigField("String", "APP_SECRET", "\"A4A2EE33347A4D7885C26689515567EC\"")
        
        manifestPlaceholders["JPUSH_PKGNAME"] = applicationId!!
        manifestPlaceholders["JPUSH_APPKEY"] = "cbd4eafc9049d751fc5a8c58"
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
        manifestPlaceholders["app_name"] = "IRCamera"
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("artibox_key/ArtiBox.jks")
            keyAlias = "Artibox"
            storePassword = "artibox2017"
            keyPassword = "artibox2017"
            // Removed deprecated isV1SigningEnabled and isV2SigningEnabled
            // Modern signing uses enableV1Signing and enableV2Signing
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
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

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    packaging {
        resources {
            merges += listOf(
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE.md",
                "META-INF/proguard/androidx-annotations.pro",
                "META-INF/proguard/coroutines.pro"
            )
            pickFirsts += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf(
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so",
                "lib/arm64-v8a/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/arm64-v8a/libnative-window.so",
                "lib/armeabi-v7a/libnative-window.so",
                "lib/armeabi-v7a/libyuv.so",
                "lib/arm64-v8a/libyuv.so",
                "lib/armeabi-v7a/libopencv_java4.so",
                "lib/arm64-v8a/libopencv_java4.so",
                "lib/armeabi-v7a/libomp.so",
                "lib/arm64-v8a/libomp.so",
                "lib/arm64-v8a/liblog.so",
                "lib/armeabi-v7a/liblog.so",
                "lib/arm64-v8a/libijkffmpeg.so",
                "lib/arm64-v8a/libijkplayer.so",
                "lib/arm64-v8a/libijksdl.so",
                "lib/armeabi/libijkffmpeg.so",
                "lib/armeabi/libijkplayer.so",
                "lib/armeabi/libijksdl.so",
                "lib/armeabi-v7a/libijkffmpeg.so",
                "lib/armeabi-v7a/libijkplayer.so",
                "lib/armeabi-v7a/libijksdl.so",
                "lib/x86/libijkffmpeg.so",
                "lib/x86/libijkplayer.so",
                "lib/x86/libijksdl.so",
                "lib/x86_64/libijkffmpeg.so",
                "lib/x86_64/libijkplayer.so",
                "lib/x86_64/libijksdl.so"
            )
            // Enhanced native library stripping configuration
            keepDebugSymbols += listOf(
                "**/*.so"  // Keep all debug symbols to prevent stripping issues
            )
            // Exclude libraries that can't be stripped due to corrupt headers
            excludes += listOf(
                "**/libSRImage.so"  // Primary culprit for stripping errors
            )
        }
    }
    
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    
    // D8 compilation is automatically optimized by AGP 8.0+
    // Removed obsolete dexOptions configuration
}

// Dependency resolution strategy to fix Guava conflicts and add ListenableFuture
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-android")
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Guava for CameraX ListenableFuture support
    implementation("com.google.guava:guava:31.1-android")
    
    // Core consolidated modules
    implementation(project(":component:thermal"))      // Consolidated thermal functionality
    implementation(project(":component:thermal-ir"))   // Thermal IR resources needed by app
    implementation(project(":component:thermal-lite")) // Thermal Lite functionality
    implementation(project(":component:pseudo"))       // Pseudo color functionality needed by app
    implementation(project(":component:gsr-recording"))
    implementation(project(":component:user"))         // User module for MoreActivity and settings
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))               // Menu resources needed by app
    
    // Enhanced BLE Module with Nordic BLE backend for systematic harmonization
    implementation(project(":BleModule"))

    // ARouter configuration
    implementation(libs.arouter.api)
    kapt(libs.arouter.compiler)

    // LocalRepo AAR files moved to app/libs
    implementation(files("libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    implementation(files("libs/libirutils_1.2.0_2409241055.aar"))
    implementation(files("libs/libcommon_1.2.0_24052117.aar"))
    
    // libapp AAR dependencies - now handled at app level due to AGP 8.0+ restrictions
    implementation(files("libs/lms_international-3.90.009.0.aar"))  // LMS SDK for libapp
    implementation(files("libs/abtest-1.0.1.aar"))
    implementation(files("libs/auth-number-2.13.2.1.aar"))
    implementation(files("libs/logger-2.2.1-release.aar"))
    implementation(files("libs/main-2.2.1-release.aar"))
    
    // Additional AAR dependencies from libir module - all libir AAR files now handled at app level  
    // Removed edit3d AAR dependency - module removed as unused
    // implementation(fileTree(mapOf("include" to listOf("opengl_1.3.2_standard.aar"), "dir" to "component/edit3d/libs")))
    implementation(fileTree(mapOf("include" to listOf("*.aar"), "dir" to "libir/libs")))  // All libir AAR files
    
    // Explicit AAR dependencies for app module compilation (ensuring classpath resolution)
    implementation(files("../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))  // Required for iruvc classes in app module

    implementation(libs.jsbridge)
    implementation(libs.fastjson)
    implementation(libs.ucrop)
    implementation(libs.play.app.update)
    implementation(libs.immersionbar)
    implementation(libs.xpopup)
    // implementation(libs.bundles.smart.refresh) // Temporarily commented out due to jitpack.io issues
    implementation(libs.wechat.sdk)
    implementation(libs.umeng.apm)
    // implementation(libs.zoho.salesiq) // Commented out - not essential for MPDC4GSR

    // UMeng - Simplified single implementation
    implementation(libs.umeng.common)
    
    // Enhanced charting and data visualization (provided by libui module)
    implementation("com.opencsv:opencsv:5.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Enhanced networking and serialization for Hub-Spoke
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Nordic BLE Library for robust Bluetooth communication
    implementation("no.nordicsemi.android:ble:2.6.1")
    implementation("no.nordicsemi.android:ble-ktx:2.6.1")
    
    // CameraX for RGB camera dual-stream capture
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    
    // Comprehensive Testing Dependencies
    // Unit testing framework
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    
    // Mocking framework
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("io.mockk:mockk-android:1.13.4")
    
    // Kotlin coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Android instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // AndroidX Test - Instrumented testing
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    
    // Robolectric for unit tests that need Android framework
    testImplementation("org.robolectric:robolectric:4.10.3")
    
    // Network testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    
    // Performance testing and benchmarking
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.2")
    
    // Truth assertions for better test readability
    testImplementation("com.google.truth:truth:1.1.4")
    androidTestImplementation("com.google.truth:truth:1.1.4")
    
    // Test data generation
    testImplementation("com.github.javafaker:javafaker:1.0.2")
}

// Utility functions for APK naming (converted from original Groovy)
fun getYearStr(): String {
    return SimpleDateFormat("yy", Locale.getDefault()).format(Date())
}

fun getDayStr(): String {
    return SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
}

fun getTimeStr(): String {
    return SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
}
