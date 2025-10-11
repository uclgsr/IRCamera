plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

android {
    namespace = "com.mpdc4gsr.component.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += listOf("armeabi-v7a", "x86", "arm64-v8a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
            )
        }
    }

    androidComponents {
        beforeVariants { variant ->
            variant.enable = variant.buildType == "release" || variant.buildType == "debug"
        }
    }

    compileOptions {
        // Toolchain config handles language level; retain desugaring flag here.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                    listOf(
                            "-opt-in=kotlin.RequiresOptIn",
                            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                            "-opt-in=kotlinx.coroutines.FlowPreview",
                    ),
            )
        }
    }

    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

    ndkVersion = libs.versions.ndkVersion.get()
    buildFeatures {
        buildConfig = true
        compose = true
    }

    sourceSets { getByName("main") { jniLibs.srcDirs("src/main/jniLibs") } }

    packaging {
        jniLibs {
            pickFirsts += listOf("**/libc++_shared.so")
            excludes +=
                    listOf(
                            "**/libavcodec.so",
                            "**/a.so",
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

    exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Vendor drop-in artifacts packaged with the module. These remain api to surface SDK types.
    api(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    api(
        mapOf(
            "name" to "libusbdualsdk_1.3.4_2406271906_standard",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "libAC020sdk_USB_IR_1.1.1_2408291439",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "libirutils_1.2.0_2409241055",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "opengl_1.3.2_standard",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "suplib-release",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "ai-upscale-release",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "texturegesture-release",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "jetified-tas_api-1.0.4.0",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "library_1.0",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "topdon",
            "ext" to "aar",
        ),
    )
    api(
        mapOf(
            "name" to "libcommon_1.2.0_24052117",
            "ext" to "aar",
        ),
    )

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.preference)
    implementation(libs.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.room.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.eventbus)
    implementation(libs.coil.compose)
    implementation(libs.xlog)
    implementation(libs.photoview)
    implementation(libs.lottie)
    implementation(libs.brvah)
    implementation(libs.logging.interceptor)
    implementation(libs.colorpickerview)
    implementation(libs.nifty)
    implementation(libs.javacv)
    implementation(libs.javacpp)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.xmlbeans)
    implementation(libs.stax.api)
    implementation(libs.aalto.xml)

    implementation("com.conghuahuadan:superlayout:1.1.0")
    implementation(libs.ir.layout)
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.sense)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    ksp(libs.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.test.espresso.core)
}

tasks.matching { it.name.startsWith("lint") }.configureEach {
    enabled = false
}

