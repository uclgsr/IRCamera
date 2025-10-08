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
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.huawei.agconnect)
    }
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.yaml:snakeyaml:1.33")
        }
    }
}
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory.get().asFile)
}
tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules including build cache"
    dependsOn("clean")
    doLast {
        // Do NOT delete .gradle directory to avoid Windows file lock issues
        // Clean root build directory
        delete(file("${rootProject.projectDir}/build"))

        // Clean all subproject build directories
        subprojects.forEach { subproject ->
            delete(file("${subproject.projectDir}/build"))
        }

        println("All modules cleaned successfully (without deleting .gradle cache)")
    }
}
tasks.register("build") {
    group = "build"
    description = "Builds all modules using only release variants (starts with clean)"
    dependsOn(
        "cleanAll",
        ":app:assembleRelease",
        ":BleModule:assembleRelease",
        ":libunified:assembleRelease",
        ":component:gsr-recording:assembleRelease",
        ":component:thermalunified:assembleRelease",
        ":component:user:assembleRelease"
    )
}
