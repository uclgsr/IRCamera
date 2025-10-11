pluginManagement {
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
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.scijava.org/content/repositories/public") }
        maven { url = uri("https://maven.scijava.org/content/groups/public") }

        flatDir {
            dirs("component/shared/libs", "app/libs", "BleModule/libs")
        }
    }
}

rootProject.name = "MPDC4GSR"

include(":app")
include(":component:thermal")
project(":component:thermal").projectDir = file("component/thermal")
include(":component:shared")
project(":component:shared").projectDir = file("component/shared")
include(":BleModule")
