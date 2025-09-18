plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.topdon.ble"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    val lmsAarCandidates = listOf(
        file("../shared/libs/lms_international-3.90.009.0.aar"),
        file("../app/libs/lms_international-3.90.009.0.aar"),
        file("../libapp/libs/lms_international-3.90.009.0.aar")
    )
    val lmsAar = lmsAarCandidates.firstOrNull { it.exists() && it.length() > 0L }
    if (lmsAar != null) {
        compileOnly(files(lmsAar))
        logger.lifecycle("BleModule: Using LMS AAR from ${lmsAar.absolutePath}")
    } else {
        logger.warn("BleModule: Skipping lms_international AAR because no valid file found in shared/app/libapp libs")
    }

    api(libs.androidx.appcompat)
    api(libs.eventbus)
    api(libs.utilcode)
    api(libs.gson)
    api(libs.xlog)

    api(libs.nordic.ble)
    api(libs.nordic.ble.ktx)

    implementation(files("libs/ini4j-0.5.5.jar"))
}
