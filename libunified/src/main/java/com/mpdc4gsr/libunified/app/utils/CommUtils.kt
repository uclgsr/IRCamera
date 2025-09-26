package com.mpdc4gsr.libunified.app.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication

/**
 * des:
 * author: CaiSongL
 * date: 2024/2/22 17:06S
 **/
object CommUtils {

    fun getAppName() : String{
        var msg = ""
        var appInfo: ApplicationInfo? = null
        try {
            appInfo = BaseApplication.instance.packageManager
                .getApplicationInfo(
                    BaseApplication.instance.packageName,
                    PackageManager.GET_META_DATA
                )
            msg = appInfo.metaData?.getString("app_name")?.toString() ?: ""
        } catch (e : Exception){
            XLog.w("获取app名称异常： ${e.message}")
        }
        return msg
    }
}