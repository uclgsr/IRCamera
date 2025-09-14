plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.topdon.gsr"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // targetSdk = libs.versions.targetSdk.get().toInt()  // Deprecated in library modules

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
        freeCompilerArgs +=
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
    }

    buildFeatures {
        dataBinding = false
        viewBinding = true
    }
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-service:2.9.3")
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    implementation("com.google.code.gson:gson:2.13.2")

    // Enhanced BLE Module with Nordic BLE backend
    implementation(project(":BleModule"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // CameraX for advanced camera integration
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-video:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.camera:camera-extensions:1.5.0")

    // For CSV writing
    implementation("com.opencsv:opencsv:5.12.0")

    // Official Shimmer Android API Integration - Latest v3.2.3Beta
    // JAR files from https://github.com/ShimmerEngineering/ShimmerAndroidAPI/releases
    // Exclude ShimmerBiophysicalProcessingLibrary - already provided by main Shimmer SDK AAR
    // implementation(files("libs/ShimmerBiophysicalProcessingLibrary_Rev_0_11.jar"))
    // Exclude AndroidBluetoothLibrary.jar - already provided by main Shimmer SDK AAR
    // implementation(files("libs/AndroidBluetoothLibrary.jar"))
    // Exclude androidplot-core - already provided by main Shimmer SDK AAR
    // implementation(files("libs/androidplot-core-0.5.0-release.jar"))

    // Additional dependencies for Shimmer API compatibility
    implementation("com.google.guava:guava:20.0")
    implementation("java3d:vecmath:1.3.1")
    implementation("org.apache.commons:commons-lang3:3.18.0")

    // BLE support for Shimmer3R and other modern devices
    implementation("com.github.Jasonchenlijian:FastBle:2.4.0")

    // Testing with Robolectric for context-based tests
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
