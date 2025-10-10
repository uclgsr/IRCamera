import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.ktlint) apply false
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

subprojects {
    plugins.withType<KotlinBasePluginWrapper> {
        if (!project.pluginManager.hasPlugin("org.jlleitschuh.gradle.ktlint")) {
            project.pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        }
        project.extensions.configure<KtlintExtension> {
            android.set(
                project.pluginManager.hasPlugin("com.android.application") ||
                    project.pluginManager.hasPlugin("com.android.library")
            )
            ignoreFailures.set(false)
            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.CHECKSTYLE)
            }
            filter {
                exclude("**/generated/**")
                exclude("**/build/**")
            }
        }
    }
}

val formatCode = tasks.register("formatCode") {
    group = "formatting"
    description = "Runs ktlintFormat across all subprojects."
}

val lintAll = tasks.register("lintAll") {
    group = "verification"
    description = "Runs ktlintCheck and Android lint across all subprojects."
}

gradle.projectsEvaluated {
    subprojects.forEach { subproject ->
        subproject.tasks.findByName("ktlintFormat")?.let {
            formatCode.configure {
                dependsOn("${subproject.path}:ktlintFormat")
            }
        }
        subproject.tasks.findByName("ktlintCheck")?.let {
            lintAll.configure {
                dependsOn("${subproject.path}:ktlintCheck")
            }
        }
        subproject.tasks.findByName("lint")?.let {
            lintAll.configure {
                dependsOn("${subproject.path}:lint")
            }
        }
    }
}

val cleanTask = tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

val cleanAll = tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules including build cache"
    dependsOn(cleanTask)
}

subprojects {
    tasks.matching { it.name == "clean" }.configureEach {
        rootProject.tasks.named("cleanAll").configure {
            dependsOn(this@configureEach)
        }
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
        ":component:thermalunified:assembleRelease"
    )
}
