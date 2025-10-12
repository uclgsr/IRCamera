plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.mpdc4gsr.ble"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
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
    androidComponents {
        beforeVariants { variant ->
            variant.enable = variant.buildType == "release" || variant.buildType == "debug"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(
                    libs.versions.jvmTargetApp.get()
                )
            )
        }
    }
    java {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(
                    libs.versions.javaVersionApp.get().toInt()
                )
            )
        }
    }
    buildToolsVersion = libs.versions.buildToolsVersion.get()
}
dependencies {
    implementation(libs.identity.jvm)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    api(libs.androidx.appcompat)
    api(libs.eventbus)
    api(libs.gson)
    api(libs.xlog)
    api(libs.nordic.ble) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }
    api(libs.nordic.ble.ktx)
    implementation(files("libs/ini4j-0.5.5.jar"))
    implementation(project(":component:shared"))
}
