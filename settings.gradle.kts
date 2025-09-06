dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.zohodl.com") }
        // Aliyun repositories as fallback
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // Local AAR files directories
        flatDir {
            dirs("libir/libs")
        }
        flatDir {
            dirs("libapp/libs")
        }
        flatDir {
            dirs("app/libs")  // Added for LocalRepo AAR files
        }
        
        flatDir {
            dirs("commonlibrary")
        }

        flatDir {
            dirs("component/edit3d/libs")
        }
    }
}

rootProject.name = "TopInfrared"

include(":app")
include(":BleModule")
include(":commonlibrary")
include(":component:CommonComponent")
include(":component:edit3d")
include(":component:house")
include(":component:pseudo")
include(":component:thermal")
include(":component:thermal-ir")
include(":component:thermal-lite")
include(":component:transfer")
include(":component:user")
include(":component:gsr-recording")
include(":libapp")
include(":libcom")
include(":libhik")
include(":libir")
include(":libmatrix")
include(":libmenu")
include(":libui")
include(":RangeSeekBar")