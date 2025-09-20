plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.mpdc4gsr.gsr"
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

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.service)
    implementation(libs.work.runtime.ktx)
    implementation(libs.gson)
    implementation(project(":BleModule"))
    implementation(libs.coroutines.core.legacy)
    implementation(libs.coroutines.android.legacy)
    implementation(libs.bundles.camerax)
    implementation(libs.opencsv)
    implementation(libs.guava.legacy)
    implementation(libs.vecmath)
    implementation(libs.commons.lang3)
    implementation(libs.fastble)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.coroutines.test.legacy)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
