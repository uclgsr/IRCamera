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
        // Only release build type - no debug variants
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Disable all debug variants completely
    variantFilter {
        if (buildType.name == "debug") {
            ignore = true
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
            keepDebugSymbols += listOf("**/*.so")
        }
    }
    
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
}

// Dependency resolution strategy to fix Guava conflicts
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-android")
        exclude(group = "com.google.guava", module = "listenablefuture")
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Core consolidated modules
    implementation(project(":component:thermal"))      // Consolidated thermal functionality
    implementation(project(":component:thermal-ir"))   // Thermal IR resources needed by app
    implementation(project(":component:thermal-lite")) // Thermal Lite functionality
    implementation(project(":component:pseudo"))       // Pseudo color functionality needed by app
    implementation(project(":component:gsr-recording"))
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))               // Menu resources needed by app

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
    implementation(fileTree(mapOf("include" to listOf("opengl_1.3.2_standard.aar"), "dir" to "component/edit3d/libs")))
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
