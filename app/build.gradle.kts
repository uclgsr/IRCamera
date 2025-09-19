import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp") 
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
                "proguard-rules.pro",
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf(
            "StringFormatInvalid",
            "StringFormatMatches",
            "StringFormatCount",
            "MissingTranslation",
            "ResourceType"
        )
    }

    androidResources {

        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
        additionalParameters += listOf("--allow-reserved-package-id", "--auto-add-overlay")
    }

    packaging {
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

    androidComponents {
        beforeVariants { variant ->
            
            variant.enable = variant.buildType == "release" || variant.buildType == "debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview",
                    "-Xjvm-default=all",
                )
            )
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    packaging {
        resources {
            merges +=
                listOf(
                    "META-INF/LICENSE-notice.md",
                    "META-INF/LICENSE.md",
                    "META-INF/proguard/androidx-annotations.pro",
                    "META-INF/proguard/coroutines.pro",
                )
            pickFirsts +=
                listOf(
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                )
            excludes +=
                listOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/ASL2.0",

                    "META-INF/com.android.art/baseline.prof",
                    "META-INF/com.android.art/baseline.profm",

                    "**/it/gerdavax/easybluetooth/**",

                    "**/android/bluetooth/IBluetoothDeviceCallback*",

                    "**/com/androidplot/**",

                    "**/com/shimmerresearch/biophysicalprocessing/**",
                    "**/com/shimmerresearch/utilityfunctions/**",
                )
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts +=
                listOf(
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
                    "lib/x86_64/libijksdl.so",
                )

            keepDebugSymbols +=
                listOf(
                    "**/*.so", 
                )

            excludes +=
                listOf(
                    "**/libSRImage.so", 
                )
        }
        jniLibs {
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libomp.so"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }


}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-android")
        
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")

        eachDependency {
            if (requested.group == "it.gerdavax.easybluetooth") {

                useTarget("${project.group}:${project.name}:${project.version}")
            }

            if (requested.name == "androidplot-core") {
                useVersion("0.5.0-release")
            }
        }
    }

    exclude(group = "android.bluetooth", module = "IBluetoothDeviceCallback")
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.guava)

    implementation(project(":component:thermal")) 
    implementation(project(":component:thermal-ir")) 
    implementation(project(":component:thermal-lite")) 
    implementation(project(":component:pseudo")) 
    implementation(project(":component:gsr-recording"))
    implementation(project(":component:user")) 
    implementation(project(":libapp"))
    implementation(project(":libir"))
    implementation(project(":libui")) 

    implementation(project(":BleModule"))

    
    
    

    implementation(files("libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    implementation(files("libs/libirutils_1.2.0_2409241055.aar"))
    implementation(files("libs/libcommon_1.2.0_24052117.aar"))

    implementation(files("libs/lms_international-3.90.009.0.aar")) 
    implementation(files("libs/abtest-1.0.1.aar"))
    implementation(files("libs/auth-number-2.13.2.1.aar"))
    implementation(files("libs/logger-2.2.1-release.aar"))
    implementation(files("libs/main-2.2.1-release.aar"))



    implementation(
        fileTree(
            mapOf(
                "include" to listOf("*.aar"),
                "dir" to "libir/libs"
            )
        )
    ) 

    implementation(files("../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar")) 

    implementation(libs.jsbridge)
    implementation(libs.fastjson2)
    implementation(libs.ucrop)
    implementation(libs.play.app.update)
    implementation(libs.immersionbar)
    implementation(libs.xpopup)

    implementation(libs.wechat.sdk)
    implementation(libs.umeng.apm)


    implementation(libs.umeng.common)

    implementation(libs.opencsv)
    implementation(libs.gson)

    
    implementation(libs.jmdns)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    implementation(libs.nordic.ble)
    implementation(libs.nordic.ble.ktx)


    
    implementation(files("libs/shimmerandroidinstrumentdriver-3.2.4_beta.aar"))
    implementation(files("libs/shimmerdriver-0.11.5_beta.jar"))
    implementation(files("libs/shimmerdriverpc-0.11.5_beta.jar"))
    implementation(files("libs/shimmerbluetoothmanager-0.11.5_beta.jar"))


    
    implementation(libs.bundles.camerax)


    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.hamcrest)

    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)

    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)

    androidTestImplementation(libs.test.ext.junit.ktx)
    androidTestImplementation(libs.test.espresso.contrib)
    androidTestImplementation(libs.test.espresso.intents)
    androidTestImplementation(libs.test.uiautomator)

    testImplementation(libs.robolectric)

    testImplementation(libs.mockwebserver)

    androidTestImplementation(libs.benchmark.junit4)

    testImplementation(libs.truth)
    androidTestImplementation(libs.truth)

    testImplementation(libs.javafaker)
}

fun getYearStr(): String {
    return SimpleDateFormat("yy", Locale.getDefault()).format(Date())
}

fun getDayStr(): String {
    return SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
}

fun getTimeStr(): String {
    return SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
}
