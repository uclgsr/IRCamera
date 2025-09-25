plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false  
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

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


tasks.register("buildRelease") {
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

tasks.register("buildDebug") {
    group = "build"
    description = "Builds all modules using only debug variants (starts with clean)"
    dependsOn(
        "cleanAll",
        ":app:assembleDebug",
        ":BleModule:assembleDebug",
        ":libunified:assembleDebug",
        ":component:gsr-recording:assembleDebug",
        ":component:thermalunified:assembleDebug",
        ":component:user:assembleDebug"
    )
}

// Simplified unified build task for clean + build
tasks.register("build") {
    group = "build"
    description = "Clean and build all modules (release)"
    dependsOn("cleanAll", "buildRelease")
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
        ":BleModule:compileDebugSources",
        ":libunified:compileDebugSources",
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
        ":BleModule:compileReleaseSources",
        ":libunified:compileReleaseSources",
        ":component:gsr-recording:compileReleaseSources",
        ":component:thermalunified:compileReleaseSources",
        ":component:user:compileReleaseSources"
    )
}

// Apply static analysis plugins to all subprojects
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.diffplug.spotless")

    // Configure ktlint
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        outputColorName.set("RED")
        enableExperimentalRules.set(true)
        additionalEditorconfig.set(
            mapOf(
                "max_line_length" to "120",
                "indent_size" to "4"
            )
        )
    }

    // Configure Detekt
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        basePath = projectDir.absolutePath
    }

    // Configure Spotless
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint()
            indentWithSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }
        
        java {
            target("**/*.java")
            googleJavaFormat()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

// Add quality gate tasks
tasks.register("codeQuality") {
    group = "verification"
    description = "Run all code quality checks"
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(subprojects.map { "${it.path}:detekt" })
    dependsOn(subprojects.map { "${it.path}:spotlessCheck" })
}

tasks.register("formatCode") {
    group = "formatting"
    description = "Format all code using Spotless and ktlint"
    dependsOn(subprojects.map { "${it.path}:ktlintFormat" })
    dependsOn(subprojects.map { "${it.path}:spotlessApply" })
}
