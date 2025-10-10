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
            dirs("libunified/libs", "app/libs", "BleModule/libs")
        }
    }
}

rootProject.name = "MPDC4GSR"

include(":app")
include(":component:thermalunified")
project(":component:thermalunified").projectDir = file("thermalunified")
include(":component:user")
include(":libunified")
include(":BleModule")
