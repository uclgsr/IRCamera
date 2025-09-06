import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

val dayStr = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
val timeStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())

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

        buildConfigField("String", "VERSION_DATE", "\"$dayStr\"")
        
        manifestPlaceholders["JPUSH_PKGNAME"] = applicationId!!
        manifestPlaceholders["JPUSH_APPKEY"] = "cbd4eafc9049d751fc5a8c58"
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
    }

    base {
        archivesName = "TC001-v${libs.versions.versionName.get()}.google"
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
                "lib/armeabi-v7a/liblog.so"
            )
        }
    }
    
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    
    packaging {
        resources {
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
            pickFirsts += listOf(
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
        }
    }
    
    flavorDimensions += "app"

    productFlavors {
        create("prod") {
            dimension = "app"
            buildConfigField("int", "ENV_TYPE", "0")
            buildConfigField("String", "SOFT_CODE", "\"${libs.versions.softcodeTopinfrared.get()}\"")
            buildConfigField("String", "APP_KEY", "\"${libs.versions.appkeyTopinfrared.get()}\"")
            buildConfigField("String", "APP_SECRET", "\"${libs.versions.appsecretTopinfrared.get()}\"")
            manifestPlaceholders["app_name"] = "IRCamera"
        }
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

// APK naming function - Simplified for prodRelease only
fun getApkName(variantName: String, versionName: String): String {
    val nameStr = "TopInfrared_${versionName}.$dayStr"
    return when (variantName) {
        "prodRelease" -> "$nameStr.apk"
        else -> "TopInfrared.apk"
    }
}

// APK naming will be configured later
// android.applicationVariants.all { variant ->
//     variant.outputs.forEach { output ->
//         if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
//             output.outputFileName = getApkName(variant, AndroidConfig.versionName)
//         }
//     }
// }

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":component:edit3d"))
    implementation(project(":component:pseudo"))
    implementation(project(":component:thermal-ir"))
    implementation(project(":component:thermal-lite"))
    implementation(project(":component:transfer"))
    implementation(project(":component:user"))
    implementation(project(":component:gsr-recording"))
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libmenu"))
    implementation(project(":libui"))

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
    implementation(libs.zoho.salesiq)

    // Core library desugaring for Java 8+ APIs on older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // UMeng - Referenced directly from Maven Central
    implementation(libs.umeng.common)
}

