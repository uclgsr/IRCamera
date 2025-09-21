dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        flatDir {
            dirs("libunified/libs", "app/libs", "ble-core/libs")
        }
    }
}

rootProject.name = "MPDC4GSR"

include(":app")
include(":component:gsr-recording")
include(":component:thermalunified")
include(":component:user")
include(":libunified")
include(":ble-core")
include(":ble-shimmer")
include(":ble-topdon")
