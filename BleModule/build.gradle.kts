plugins {
    id("com.android.library")
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
                "proguard-rules.pro"
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
}

dependencies {
    // Core library desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Add libapp dependency to get access to LMS SDK
    api(project(":libapp"))
    
    // Compile-time access to LMS SDK for BleModule classes that directly import LMS classes
    compileOnly(files("../shared/libs/lms_international-3.90.009.0.aar"))
    
    api("androidx.appcompat:appcompat:1.2.0")
    api("org.greenrobot:eventbus:3.2.0")
    api("com.blankj:utilcodex:1.30.6") // 工具包
    api("com.google.code.gson:gson:2.8.8")
    api("com.elvishew:xlog:1.10.1")
    // UMeng Analytics - now available via version catalog
    // api(libs.umeng.analytics) 
    // FastJSON - testing dependency availability
    // api("com.alibaba:fastjson:1.2.83") 
    implementation(files("libs/ini4j-0.5.5.jar"))
}