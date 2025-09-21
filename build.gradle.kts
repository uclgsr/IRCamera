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

// Enhanced clean task that also cleans all subprojects  
tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules including build cache and gradle cache"
    dependsOn("clean")
    doLast {
        // Clean gradle build cache
        delete(file("${rootProject.projectDir}/.gradle"))
        delete(file("${rootProject.projectDir}/build"))

        // Clean all subproject build directories
        subprojects.forEach { subproject ->
            delete(file("${subproject.projectDir}/build"))
        }

        println("All modules and caches cleaned successfully")
    }
}


tasks.register("buildRelease") {
    group = "build"
    description = "Builds all modules using only release variants (starts with clean)"
    dependsOn(
        "cleanAll",
        ":app:assembleRelease",
        ":ble-core:assembleRelease",
        ":ble-shimmer:assembleRelease",
        ":ble-topdon:assembleRelease",
        ":libunified:assembleRelease",
        ":RangeSeekBar:assembleRelease",
        ":component:gsr-recording:assembleRelease",
        ":component:thermalunified:assembleRelease",
        ":component:user:assembleRelease"
    )
}

tasks.register("buildDebug") {
    group = "build"
    description = "Builds all modules using only debug variants (starts with clean)"
    dependsOn(
        "cleanAll",
        ":app:assembleDebug",
        ":ble-core:assembleDebug",
        ":ble-shimmer:assembleDebug",
        ":ble-topdon:assembleDebug",
        ":libunified:assembleDebug",
        ":RangeSeekBar:assembleDebug",
        ":component:gsr-recording:assembleDebug",
        ":component:thermalunified:assembleDebug",
        ":component:user:assembleDebug"
    )
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all modules with all variants (starts with clean)"
    dependsOn("cleanAll")
    finalizedBy("buildRelease", "buildDebug")
}

// Create safer wrapper tasks for common build operations
tasks.register("compileDebugSafe") {
    group = "build"
    description = "Safe debug compilation (clean + compile)"
    dependsOn("cleanAll")
    finalizedBy(
        ":app:compileDebugSources",
        ":ble-core:compileDebugSources",
        ":ble-shimmer:compileDebugSources",
        ":ble-topdon:compileDebugSources",
        ":libunified:compileDebugSources",
        ":RangeSeekBar:compileDebugSources",
        ":component:gsr-recording:compileDebugSources",
        ":component:thermalunified:compileDebugSources",
        ":component:user:compileDebugSources"
    )
}

tasks.register("compileReleaseSafe") {
    group = "build"
    description = "Safe release compilation (clean + compile)"
    dependsOn("cleanAll")
    finalizedBy(
        ":app:compileReleaseSources",
        ":ble-core:compileReleaseSources",
        ":ble-shimmer:compileReleaseSources",
        ":ble-topdon:compileReleaseSources",
        ":libunified:compileReleaseSources",
        ":RangeSeekBar:compileReleaseSources",
        ":component:gsr-recording:compileReleaseSources",
        ":component:thermalunified:compileReleaseSources",
        ":component:user:compileReleaseSources"
    )
}
