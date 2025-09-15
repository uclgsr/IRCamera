plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.topdon.module.thermal.ir"
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
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))
    implementation(project(":component:pseudo"))
    implementation(project(":component:thermal"))
    implementation(project(":component:user"))
    compileOnly(files("../../libir/libs/suplib-release.aar")) // Required for SupHelp class
    compileOnly(files("../../libir/libs/ai-upscale-release.aar")) // AI upscale functionality
    compileOnly(files("../../libir/libs/texturegesture-release.aar")) // Texture gesture functionality
    compileOnly(files("../../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar")) // Required for IRCMD classes
    compileOnly(files("../../libir/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar")) // AC020 SDK
    compileOnly(files("../../libir/libs/libirutils_1.2.0_2409241055.aar")) // IR utilities
    compileOnly(files("../../app/libs/lms_international-3.90.009.0.aar")) // LMS SDK for thermal-ir classes
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.glide)
    implementation(libs.lottie)
    implementation("com.github.anzaizai:EasySwipeMenuLayout:1.1.4")
    implementation(libs.mn.image.browser)
    implementation("com.github.CarGuo.GSYVideoPlayer:gsyVideoPlayer-java:v8.6.0-release-jitpack") {
        exclude(group = "androidx.media3", module = "media3-cast")
        exclude(group = "androidx.media3", module = "media3-session")
        exclude(group = "androidx.media3", module = "media3-ui")
        exclude(group = "com.google.android.gms", module = "play-services-cast-framework")
        exclude(group = "com.aliyun.sdk.android", module = "AliyunPlayer")
    }
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("io.github.scwang90:refresh-layout-kernel:2.1.1")
    implementation("io.github.scwang90:refresh-header-classics:2.1.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}
