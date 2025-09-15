plugins {
    id("com.android.library")
    kotlin("android")
    // Disable kapt since ARouter annotations are commented out in this module
    // kotlin("kapt")
    id("kotlin-parcelize")
}


// kapt {
//     arguments {
//         arg("AROUTER_MODULE_NAME", project.name)
//     }
//     // Enable Kotlin 2.1.0 compatibility
//     correctErrorTypes = true
//     useBuildCache = true
// }

android {
    namespace = "com.topdon.module.thermal.ir"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // targetSdk removed for library modules - only set in main app module per AGP 8.0+

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Configure single release variant for easier maintenance
    androidComponents {
        beforeVariants { variant ->
            // Only enable release variant for single-developer maintenance
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
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))
    implementation(project(":component:pseudo"))
    implementation(project(":component:thermal"))
    // Removed house dependency - module removed as unused
    
    // AAR dependencies as compileOnly for compilation but not packaging
    compileOnly(files("../../libir/libs/suplib-release.aar"))  // Required for SupHelp class
    compileOnly(files("../../libir/libs/ai-upscale-release.aar"))  // AI upscale functionality
    compileOnly(files("../../libir/libs/texturegesture-release.aar"))  // Texture gesture functionality
    compileOnly(files("../../libir/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))  // Required for IRCMD classes
    compileOnly(files("../../libir/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))  // AC020 SDK 
    compileOnly(files("../../libir/libs/libirutils_1.2.0_2409241055.aar"))  // IR utilities
    compileOnly(files("../../shared/libs/lms_international-3.90.009.0.aar"))  // LMS SDK for thermal-ir classes
    
    // ARouter compiler - disabled since annotations are commented out in this module
    // kapt(libs.arouter.compiler)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.glide)
    
    // Lottie animation library
    implementation(libs.lottie)
    // EasySwipeMenuLayout
    implementation("com.github.anzaizai:EasySwipeMenuLayout:1.1.4")
    // Image browser library
    implementation(libs.mn.image.browser)
    
    // GSY VideoPlayer for video playback - using standard version
    implementation("com.github.CarGuo.GSYVideoPlayer:gsyVideoPlayer-java:v8.6.0-release-jitpack") {
        exclude(group = "androidx.media3", module = "media3-cast")
        exclude(group = "androidx.media3", module = "media3-session")
        exclude(group = "androidx.media3", module = "media3-ui")
        exclude(group = "com.google.android.gms", module = "play-services-cast-framework")
        exclude(group = "com.aliyun.sdk.android", module = "AliyunPlayer")
    }
    
    // SmartRefreshLayout for pull-to-refresh functionality
    implementation("io.github.scwang90:refresh-layout-kernel:2.1.0")
    implementation("io.github.scwang90:refresh-header-classics:2.1.0")
}
