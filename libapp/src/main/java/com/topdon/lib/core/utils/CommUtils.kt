package com.topdon.lib.core.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog

/**
 * des:
 * author: CaiSongL
 * date: 2024/2/22 17:06S
 **/
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
            XLog.w("Get/Retrieveappnameexception： ${e.message}")
        }
        return msg
    }
}
