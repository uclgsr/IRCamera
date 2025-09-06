plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

kapt {
    arguments {
        // Remove unrecognized AROUTER arguments to fix kapt warnings
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
    // Enable Kotlin 2.1.0 compatibility
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.topdon.lib.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

//kotlin {
//    experimental {
//        coroutines 'enable'
//    }
//}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Using only JAR files to avoid AGP 8.0+ AAR dependency issues
    api(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    // Note: AAR dependencies moved to app module to avoid AGP 8.0+ issues
    api(libs.androidx.appcompat)
    api(libs.fragment.ktx)
    api(libs.material)

    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.livedata.ktx)

    kapt(libs.room.compiler)
    api(libs.room.ktx)

    api(libs.work.runtime.ktx)

    api(libs.retrofit2)
    api(libs.converter.gson)
    api(libs.adapter.rxjava2)

    api(libs.eventbus)

    api(libs.glide)
    kapt(libs.glide.compiler)

    api(libs.rxjava2)
    api(libs.rxandroid)
    // Commented out problematic RxLifecycle dependencies to identify actual compilation issues
    // api(libs.rxpermissions)
    // api(libs.rxlifecycle)
    // api(libs.rxlifecycle.android)
    // api(libs.rxlifecycle.components)
    // api(libs.rxlifecycle.ktx)
    // api(libs.rxlifecycle.android.lifecycle.ktx)

    api(libs.utilcode)
    api(libs.xxpermissions)
    api(libs.xlog)
    api(libs.photoview)
    // api(libs.android.pdf.viewr) // Temporary comment out due to dependency resolution issues
    api(libs.lottie)

    api(libs.brvah)
    // Commented out problematic refresh layout dependency  
    // api(libs.refresh.layout.kernel)
    // api(libs.refresh.header.classics) // Temporary comment out
    // api(libs.refresh.header.material) // Temporary comment out

    api(libs.logging.interceptor)
    api(libs.colorpickerview)
    api(libs.nifty)
    // api(libs.nifty.effect) // Temporary comment out

//    "devApi"(libs.lms2.user)
//    "betaApi"(libs.lms2.user)
//    "prodApi"(libs.lms2.user)
//    "prodTopdonApi"(libs.lms2.user)
//    "insideChinaApi"(libs.lms3.user)
//    "prodTopdonInsideChinaApi"(libs.lms3.user)

    // JavaCV
    api(libs.javacv)
    api(libs.javacpp)
    
    // Compile-time access to LMS SDK classes without packaging in AAR
    // The app module provides the actual implementation
    compileOnly(files("../shared/libs/lms_international-3.90.009.0.aar"))
    
    // NOTE: All local AAR dependencies MUST be handled at the app level due to AGP 8.0+ restrictions
    // Library modules cannot include local AAR files when building AARs
    // Classes from LMS SDK and other AARs will be available transitively from the app module
    
    // The following dependencies are now handled by the app module:
    // - lms_international-3.90.009.0.aar (LMS SDK) - using compileOnly above for compilation
    // - abtest-1.0.1.aar
    // - auth-number-2.13.2.1.aar
    // - logger-2.2.1-release.aar
    // - main-2.2.1-release.aar
}