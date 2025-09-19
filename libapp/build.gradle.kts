plugins {
    id("com.android.library")
    kotlin("android") 
    id("kotlin-parcelize") 
    id("com.google.devtools.ksp") 
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

android {
    namespace = "com.mpdc4gsr.lib.core"
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
                )
            )
        }
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
                    "**/libavcodec.so", 
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

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutines.get()}")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.coroutines.get()}")
    }
}

dependencies {
    
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":RangeSeekBar"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    api(libs.androidx.appcompat)
    api(libs.androidx.preference)
    api(libs.fragment.ktx)
    api(libs.material)
    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.livedata.ktx)
    ksp(libs.room.compiler) 
    api(libs.room.ktx)
    api(libs.work.runtime.ktx)
    api(libs.retrofit2)
    api(libs.converter.gson)
    api(libs.adapter.rxjava2)
    api(libs.eventbus)
    api(libs.glide)
    ksp(libs.glide.compiler) 
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
    
    // Apache POI dependencies for Excel export functionality
    api(libs.apache.poi.ooxml)
    api(libs.xmlbeans)
    api(libs.stax.api)
    api(libs.aalto.xml)

    // LMS AAR dependency removed - replaced with stub implementations
    // val lmsAarCandidates = listOf(
    //     file("libs/lms_international-3.90.009.0.aar"),
    //     file("../app/libs/lms_international-3.90.009.0.aar"),
    //     file("../shared/libs/lms_international-3.90.009.0.aar")
    // )
    // val lmsAar = lmsAarCandidates.firstOrNull { it.exists() && it.length() > 0L }
    // if (lmsAar != null) {
    //     compileOnly(files(lmsAar))
    //     logger.lifecycle("libapp: Using LMS AAR from ${lmsAar.absolutePath}")
    // } else {
    //     logger.warn("libapp: Skipping lms_international AAR because no valid file found in libapp/app/shared libs")
    // }
}
