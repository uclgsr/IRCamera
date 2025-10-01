plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

android {
    namespace = "com.mpdc4gsr.libunified"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndkVersion = libs.versions.ndkVersion.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += listOf("armeabi-v7a", "x86", "arm64-v8a", "x86_64")
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

    ndkVersion = libs.versions.ndkVersion.get()
    buildFeatures {
        buildConfig = true
//        dataBinding = true
//        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
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
                    "**/libSRImage.so",
                    "**/liblog.so",
                    "**/libopen3d.so",
                    "**/libopencv_java4.so",
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

    lint {
        disable += listOf("WrongThread")
        abortOnError = false
        warningsAsErrors = false
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

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Combined dependencies from all three libraries
    api(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    // IR-specific AAR files that need to be compiled
    api(files("libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    api(files("libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    api(files("libs/libirutils_1.2.0_2409241055.aar"))
    api(files("libs/opengl_1.3.2_standard.aar"))
    api(files("libs/suplib-release.aar"))
    api(files("libs/ai-upscale-release.aar"))
    api(files("libs/texturegesture-release.aar"))
    api(files("libs/jetified-tas_api-1.0.4.0.aar"))
    api(files("libs/library_1.0.aar"))
    api(files("libs/topdon.aar"))
    api(files("../app/libs/libcommon_1.2.0_24052117.aar"))

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
    api(libs.apache.poi.ooxml)
    api(libs.xmlbeans)
    api(libs.stax.api)
    api(libs.aalto.xml)

    // IR-specific dependencies
    api("com.conghuahuadan:superlayout:1.1.0")
    api(libs.ir.layout)
    api(libs.andromeda.core)
    api(libs.andromeda.sense)

    // UI-specific dependencies
    api(libs.bundles.ui.common)

    // Compose dependencies for shared base classes
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material3)
    api(libs.compose.activity)
    api(libs.compose.material.icons.core)
    api(libs.compose.material.icons.extended)

    // Compose debug dependencies
    debugApi(libs.compose.ui.tooling)
    debugApi(libs.compose.ui.test.manifest)

    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
