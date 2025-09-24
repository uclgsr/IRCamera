plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.topdon.ble"
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
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // BLE and utilities - matching reference repository
    api("org.greenrobot:eventbus:3.3.1")
    api("com.blankj:utilcodex:1.31.1")
    api(libs.gson)
    api("com.elvishew:xlog:1.11.0")

    // Try to add LMS SDK if available (might not be in public repos)
    // api("com.topdon.lms.sdk2:lms:3.80.005")

    // Local JAR files
    implementation(files("libs/ini4j-0.5.5.jar"))

    // Analytics dependencies 
    // implementation("com.umeng.umsdk:analytics:9.6.8") // If needed

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
