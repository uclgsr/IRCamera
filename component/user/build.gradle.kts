plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.topdon.module.user"
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

            variant.enable = variant.buildType == "release"
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
        viewBinding = true
        dataBinding = true
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
    implementation(project(":libapp"))
    implementation(project(":libcom"))
    implementation(project(":libir"))
    implementation(project(":libui"))
    implementation(project(":libmenu"))
    implementation(project(":BleModule"))
    compileOnly(files("../../app/libs/lms_international-3.90.009.0.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.glide)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}
