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
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
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
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.lifecycle:lifecycle-service:2.5.1")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.google.code.gson:gson:2.9.1")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    
    // For CSV writing
    implementation("com.opencsv:opencsv:5.7.1")
    
    // Official Shimmer Android API Integration
    // JAR files from https://github.com/ShimmerEngineering/ShimmerAndroidAPI/releases
    implementation(files("libs/ShimmerBiophysicalProcessingLibrary_Rev_0_11.jar"))
    implementation(files("libs/AndroidBluetoothLibrary.jar"))
    implementation(files("libs/androidplot-core-0.5.0-release.jar"))
    
    // Additional dependencies for Shimmer API compatibility
    implementation("com.google.guava:guava:20.0")
    implementation("java3d:vecmath:1.3.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    
    // BLE support for Shimmer3R and other modern devices
    implementation("com.github.Jasonchenlijian:FastBle:2.4.0")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:4.6.1")
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
