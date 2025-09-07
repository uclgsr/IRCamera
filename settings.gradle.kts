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

// Essential sensor and recording modules
include(":component:thermal")
include(":component:gsr-recording")
include(":BleModule")

// Core library modules
include(":libapp")
include(":libcom")
include(":libir")
include(":libui")
include(":RangeSeekBar")

// Note: Removed redundant modules:
// - component:thermal-ir and component:thermal-lite (consolidated into component:thermal)
// - component:house, component:pseudo, component:user (non-essential for MPDC4GSR)
// - component:edit3d, component:transfer, component:CommonComponent (unused)
// - libhik, libmatrix, libmenu (redundant functionality)