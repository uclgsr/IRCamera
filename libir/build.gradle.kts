plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.infisense.iruvc"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
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

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs", "src/main/jnilibs")
        }
    }

    flavorDimensions.add("app")
    productFlavors {
        create("development") {
            dimension = "app"
        }
        create("production") {
            dimension = "app"
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    
    // Add missing utility libraries
    implementation(libs.utilcode)
    
    // XLog for logging
    implementation("com.elvishew:xlog:1.10.1")
    
    // Existing AAR dependencies from libir/libs
    implementation(files("libs/suplib-release.aar"))
    implementation(files("libs/jetified-tas_api-1.0.4.0.aar"))
    implementation(files("libs/library_1.0.aar"))
    implementation(files("libs/opengl_1.3.2_standard.aar"))
    implementation(files("libs/texturegesture-release.aar"))
    implementation(files("libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    implementation(files("libs/ai-upscale-release.aar"))
    
    // Other dependencies
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}