plugins {
    id("com.android.library")
    kotlin("android") // Modern plugin ID format
    id("kotlin-parcelize") // Correct plugin ID for Parcelize
}

android {
    namespace = "com.infisense.usbir"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndkVersion = libs.versions.ndkVersion.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
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
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        }
    }

    packaging {
        jniLibs {
            // Exclude libc++_shared.so to avoid conflicts with app dependencies
            excludes += listOf("**/libc++_shared.so")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs", "src/main/jnilibs")
        }
    }

    packaging {
        jniLibs {

            pickFirsts += listOf("**/libc++_shared.so")

            excludes +=
                listOf(
                    "**/libSRImage.so", // Corrupted ELF header - exclude to prevent stripping errors
                    "**/liblog.so", // System library - handled by OS
                    "**/libopen3d.so", // Third-party library with stripping issues
                    "**/libopencv_java4.so", // OpenCV library - large and causes stripping issues
                )
            keepDebugSymbols += listOf("**/*.so")
        }
        resources {
            excludes +=
                listOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly(files("libs/libusbdualsdk_1.3.4_2406271906_standard.aar")) // Required for infisense thermal camera classes
    compileOnly(files("libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar")) // AC020 SDK for thermal-lite functionality
    compileOnly(files("libs/libirutils_1.2.0_2409241055.aar")) // IR utilities for thermal-lite
    compileOnly(files("libs/opengl_1.3.2_standard.aar")) // OpenGL functionality
    compileOnly(files("libs/suplib-release.aar")) // Required for thermal-lite iruvc classes
    compileOnly(files("libs/ai-upscale-release.aar")) // AI upscale functionality
    compileOnly(files("libs/texturegesture-release.aar")) // Texture gesture functionality
    compileOnly(files("libs/jetified-tas_api-1.0.4.0.aar")) // TAS API
    compileOnly(files("libs/library_1.0.aar")) // Additional library support
    api("com.conghuahuadan:superlayout:1.1.0")
    api(libs.ir.layout) // IR layout utilities from CoderCaiSL jitpackMvn
    api(libs.andromeda.core) // Andromeda core for sensor functionality
    api(libs.andromeda.sense) // Andromeda sense for compass and sensors
    api(libs.javacv) // JavaCV for IR image processing
    api(libs.javacpp) // JavaCV native dependencies
    implementation(project(":libapp"))
    implementation(project(":BleModule"))
    compileOnly(files("../app/libs/libcommon_1.2.0_24052117.aar"))
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
