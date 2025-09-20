buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.zohodl.com") }
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.ksp.gradle.plugin)
        classpath(libs.huawei.agconnect)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory.get().asFile)
}


tasks.register("buildRelease") {
    group = "build"
    description = "Builds all modules using only release variants"
    dependsOn(
        ":app:assembleRelease",
        ":BleModule:assembleRelease",
        ":libapp:assembleRelease",
        ":libir:assembleRelease",
        ":libui:assembleRelease",
        ":RangeSeekBar:assembleRelease",
        ":component:gsr-recording:assembleRelease",
        ":component:thermal:assembleRelease",
        ":component:thermal-ir:assembleRelease",
        ":component:thermal-lite:assembleRelease",
        ":component:user:assembleRelease"
    )
}
