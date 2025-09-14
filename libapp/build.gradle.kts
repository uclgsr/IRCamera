plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
    correctErrorTypes = true
    useBuildCache = true
    includeCompileClasspath = false
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    androidComponents {
        beforeVariants { variant ->
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
        freeCompilerArgs +=
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
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

    packaging {
        jniLibs {
            pickFirsts += listOf("**/libc++_shared.so")
            excludes +=
                listOf(
                    "**/libavcodec.so", // FFmpeg libraries with stripping issues
                    "**/libavdevice.so",
                    "**/libavfilter.so",
                    "**/libavformat.so",
                    "**/libavutil.so",
                    "**/libjniavcodec.so",
                    "**/libjniavdevice.so",
                    "**/libjniavfilter.so",
                    "**/libjniavformat.so",
                    "**/libjniavutil.so",
                    "**/libjniswresample.so",
                    "**/libjniswscale.so",
                    "**/libswresample.so",
                    "**/libswscale.so",
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
    api(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    api(libs.androidx.appcompat)
    api(libs.androidx.preference)
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
    api(libs.utilcode)
    api(libs.xxpermissions)
    api(libs.xlog)
    api(libs.photoview)
    api(libs.lottie)
    api(libs.brvah)
    api(libs.logging.interceptor)
    api(libs.colorpickerview)
    api(libs.nifty)
    api(libs.javacv)
    api(libs.javacpp)
    api(project(":BleModule"))

    val lmsAarCandidates = listOf(
        file("libs/lms_international-3.90.009.0.aar"),
        file("../app/libs/lms_international-3.90.009.0.aar"),
        file("../shared/libs/lms_international-3.90.009.0.aar")
    )
    val lmsAar = lmsAarCandidates.firstOrNull { it.exists() && it.length() > 0L }
    if (lmsAar != null) {
        compileOnly(files(lmsAar))
        logger.lifecycle("libapp: Using LMS AAR from ${lmsAar.absolutePath}")
    } else {
        logger.warn("libapp: Skipping lms_international AAR because no valid file found in libapp/app/shared libs")
    }
}
