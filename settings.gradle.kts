dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        // Aliyun repositories as fallback
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // Consolidated local AAR files directories
        flatDir {
            dirs("libir/libs", "libapp/libs", "app/libs", "commonlibrary")
        }
    }
}

rootProject.name = "MPDC4GSR"

// Core application modules
include(":app")
include(":commonlibrary")

// All sensor and recording modules (restored)
include(":component:thermal")
include(":component:gsr-recording")
include(":component:thermal-ir")
include(":component:thermal-lite")
include(":component:CommonComponent")
include(":component:edit3d")
include(":component:house")
include(":component:pseudo")

include(":component:user")
include(":BleModule")

// All library modules (restored)
include(":libapp")
include(":libcom")
include(":libir")
include(":libui")
include(":libhik")
include(":libmatrix")
include(":libmenu")
include(":RangeSeekBar")