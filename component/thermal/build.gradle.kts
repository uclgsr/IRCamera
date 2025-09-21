plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.mpdc4gsr.module.thermal"
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
        dataBinding = true
        viewBinding = true
    }

    lint {
        abortOnError = false
        ignoreWarnings = true
        checkReleaseBuilds = false
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
                "ContentDescription",
                "SmallSp",
                "SpUsage",
                "HardcodedText",
                "RelativeOverlap",
            ),
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":libunified"))
    implementation(project(":ble-topdon"))
    implementation(libs.bundles.ui.common)
    implementation(libs.utilcode)
    implementation(libs.mn.image.browser)
    compileOnly(files("../../libunified/libs/suplib-release.aar"))
    compileOnly(files("../../libunified/libs/ai-upscale-release.aar"))
    compileOnly(files("../../libunified/libs/texturegesture-release.aar"))
    compileOnly(files("../../libunified/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    compileOnly(files("../../libunified/libs/libAC020sdk_USB_IR_1.1.1_2408291439.aar"))
    compileOnly(files("../../libunified/libs/libirutils_1.2.0_2409241055.aar"))
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
