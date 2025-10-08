// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\json' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\json\CheckVersionJson.kt =====

package com.mpdc4gsr.libunified.app.bean.json

data class CheckVersionJson(
    val downloadPackageUrl: String,
    val downloadPageUrl: String,
    val forcedUpgradeFlag: String?,
    val googleVerCode: Int,
    val softConfigOtherTypeVOList: List<SoftConfigOtherTypeVO>,
    val versionCode: Int,
    val versionNo: String?,
    val notUnZipSize: Double,
)

data class SoftConfigOtherTypeVO(
    val descType: Int,
    val descTypeName: String,
    val fileUrl: Any,
    val textDescription: String,
)


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\json\StatementJson.kt =====

package com.mpdc4gsr.libunified.app.bean.json

data class StatementJson(
    val content: Any,
    val createTime: String,
    val createUserName: Any,
    val current: Int,
    val htmlContent: String,
    val id: Any,
    val language: Any,
    val languageId: Any,
    val languageIds: Any,
    val revieweContent: Any,
    val size: Int,
    val softCode: Any,
    val status: Any,
    val statusList: Any,
    val type: Any,
    val versionNum: Any,
)