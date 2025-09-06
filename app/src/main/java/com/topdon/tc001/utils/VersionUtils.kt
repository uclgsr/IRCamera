package com.topdon.tc001.utils

import android.content.Context
import android.text.TextUtils

object VersionUtils {

    fun getCodeStr(context: Context): String {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        return versionName ?: ""
    }

}