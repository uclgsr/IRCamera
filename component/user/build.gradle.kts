plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mpdc4gsr.module.user"
    compileSdk = 36

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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        ignoreWarnings = true
        disable.addAll(
            listOf(
                "MissingClass",
                "Instantiatable",
                "UnusedResources",
                "IconMissingDensityFolder",
                "TypographyFractions",
                "TypographyQuotes",
                "ObsoleteLintCustomCheck",
                "GradleDependency",
                "NewerVersionAvailable",
                "UnusedIds",
                "SmallSp",
                "SpUsage",
                "HardcodedText",
                "RelativeOverlap",
            ),
        )
    }
    buildToolsVersion = "35.0.0"
}
dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":libunified"))
    implementation(project(":BleModule"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.lifecycle.runtime)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
