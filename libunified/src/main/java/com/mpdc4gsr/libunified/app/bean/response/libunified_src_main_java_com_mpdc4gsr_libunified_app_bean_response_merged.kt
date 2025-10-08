// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response\ResponseUserInfo.kt =====

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