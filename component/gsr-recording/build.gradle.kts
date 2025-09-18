plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.topdon.gsr"
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
        dataBinding = false
        viewBinding = true
    }
}

// Force consistent Kotlin version to resolve conflicts
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.service)
    implementation(libs.work.runtime.ktx)
    implementation(libs.gson)
    implementation(project(":BleModule"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // CameraX dependencies - Compatible with AGP 8.5.2
    implementation(libs.bundles.camerax)
    implementation(libs.opencsv)
    implementation(libs.guava.legacy)
    implementation(libs.vecmath)
    implementation(libs.commons.lang3)
    implementation(libs.fastble)

    // Shimmer GSR+ Sensor SDK - Official Shimmer Research Libraries
    // Note: ShimmerBiophysicalProcessingLibrary and androidplot-core are now provided by 
    // shimmerandroidinstrumentdriver-3.2.4_beta.aar in the main app module to prevent duplicate classes
    // implementation(files("libs/ShimmerBiophysicalProcessingLibrary_Rev_0_11.jar"))
    // implementation(files("libs/androidplot-core-0.5.0-release.jar"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
