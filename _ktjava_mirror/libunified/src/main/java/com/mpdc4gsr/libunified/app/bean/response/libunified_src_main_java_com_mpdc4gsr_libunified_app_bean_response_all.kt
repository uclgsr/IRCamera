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


