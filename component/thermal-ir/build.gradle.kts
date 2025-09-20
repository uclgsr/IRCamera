plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mpdc4gsr.module.thermal.ir"
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

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lint {
        abortOnError = false
        ignoreWarnings = true
        checkReleaseBuilds = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":BleModule"))
    implementation(project(":libapp"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":component:thermal"))
    implementation(project(":component:user"))
    compileOnly(files("../../libir/libs/suplib-release.aar"))
    compileOnly(files("../../libir/libs/ai-upscale-release.aar"))
    compileOnly(files("../../libir/libs/texturegesture-release.aar"))
    compileOnly(files("../../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    compileOnly(files("../../libir/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    compileOnly(files("../../libir/libs/libirutils_1.2.0_2409241055.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.localbroadcastmanager)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.glide)
    implementation(libs.lottie)
    implementation(libs.easyswipemenulayout)
    implementation(libs.mn.image.browser)








    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.refresh.layout.kernel.legacy)
    implementation(libs.refresh.header.classics.legacy)
}
