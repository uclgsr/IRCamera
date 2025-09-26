package com.mpdc4gsr.libunified.app.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.blankj.utilcode.util.Utils


object CommUtils {
    fun getAppName(): String {
        var msg = ""
        var appInfo: ApplicationInfo? = null
        appInfo =
            Utils.getApp().packageManager
                .getApplicationInfo(
                    Utils.getApp().packageName,
                    PackageManager.GET_META_DATA,
                )
        try {
            msg = appInfo.metaData.getString("app_name")?.toString() ?: ""
        } catch (e: Exception) {
            X        }
        return msg
    }
}
