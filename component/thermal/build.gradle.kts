plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.mpdc4gsr.component.thermal"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

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
                ),
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":BleModule"))
    implementation(project(":component:shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.compose.icons)
    implementation(libs.coil.compose)
    debugImplementation(libs.bundles.compose.debug)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.bundles.camerax)
    implementation(libs.material)
    implementation(libs.lottie)
    implementation(libs.easyswipemenulayout)
    implementation(libs.mn.image.browser)
    implementation(libs.brvah)
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.sense)
    implementation(libs.room.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.javacv)
    implementation(libs.javacpp)
    implementation(libs.bundles.ui.common)
    implementation(libs.bundles.media3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.classics)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
}

tasks.matching { it.name.startsWith("lint") }.configureEach {
    enabled = false
}
