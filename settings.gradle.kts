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
            dirs("libir/libs", "libapp/libs", "app/libs", "libmatrix/libs", "BleModule/libs")
        }
    }
}

rootProject.name = "MPDC4GSR"

include(":app")
include(":component:thermal")
include(":component:gsr-recording")
include(":component:thermal-ir")
include(":component:thermal-lite")
include(":component:pseudo")
include(":component:user")
include(":component:CommonComponent")
include(":libapp")
// include(":libcom") // CONSOLIDATED into :libapp
include(":libir")
// include(":libmatrix") // CONSOLIDATED into :libapp
include(":libui")
// include(":libmenu") // CONSOLIDATED into :libapp
include(":BleModule")
include(":RangeSeekBar")
