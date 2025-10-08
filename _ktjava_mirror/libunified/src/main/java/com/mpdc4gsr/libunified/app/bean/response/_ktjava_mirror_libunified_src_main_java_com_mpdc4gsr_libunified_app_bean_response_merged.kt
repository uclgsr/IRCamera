// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response\libunified_src_main_java_com_mpdc4gsr_libunified_app_bean_response_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response\ResponseUserInfo.kt =====

package com.mpdc4gsr.libunified.app.bean.response

data class ResponseUserInfo(
    val topdonId: String,
    val userName: String,
    val email: String,
    val url: String,
    val pwd: String,
    val remark: String,
    val createTime: Long,
    val updateTime: Long,
    val profilePicture: String,
    val lastVisitTime: String,
    val phone: String?,
    val avatar: String?,
)