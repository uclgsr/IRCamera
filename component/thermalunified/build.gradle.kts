plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.mpdc4gsr.module.thermalunified"
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
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":BleModule"))
    implementation(project(":libunified"))
    implementation(project(":component:user"))
    
    // IR-specific AAR files needed by thermal component (same as libunified uses)
    compileOnly(files("../../libunified/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    compileOnly(files("../../libunified/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    compileOnly(files("../../libunified/libs/libirutils_1.2.0_2409241055.aar"))
    compileOnly(files("../../libunified/libs/opengl_1.3.2_standard.aar"))
    compileOnly(files("../../libunified/libs/suplib-release.aar"))
    compileOnly(files("../../libunified/libs/ai-upscale-release.aar"))
    compileOnly(files("../../libunified/libs/texturegesture-release.aar"))
    compileOnly(files("../../libunified/libs/jetified-tas_api-1.0.4.0.aar"))
    compileOnly(files("../../libunified/libs/library_1.0.aar"))
    compileOnly(files("../../libunified/libs/topdon.aar"))
    compileOnly(files("../../app/libs/libcommon_1.2.0_24052117.aar"))
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.compose.icons)
    implementation(libs.coil.compose)
    debugImplementation(libs.bundles.compose.debug)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.bundles.camerax)
    implementation(libs.material)
    implementation(libs.lottie)
    implementation(libs.easyswipemenulayout)
    implementation(libs.mn.image.browser)
    implementation(libs.bundles.ui.common)
    implementation(libs.bundles.media3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.classics)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
