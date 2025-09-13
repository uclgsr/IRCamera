package com.topdon.lib.core.tools

import com.elvishew.xlog.XLog
import java.util.regex.Pattern

object VersionTool {
    /**
     * V1.0 => 1.0
     */
    fun getVersion(str: String): String {
        var versionStr = "1.0"
        if (str.uppercase().contains("V")) {
            if (str.length > str.lastIndexOf("V") + 1) {
                versionStr = str.substring(startIndex = str.lastIndexOf("V") + 1)
            }
        } else {
            try {
                str.toFloat()
                versionStr = str
            } catch (e: Exception) {
                // str 不是1.01typedata
            }
        }

        return versionStr
    }

    /**
     * Check是否需要update最新version
     */
    fun checkNewVersion(
        serverVersionStr: String,
        localVersionStr: String,
    ): Boolean {
        try {
            val serverV = getVersion(serverVersionStr)
            val localV = getVersion(localVersionStr)
            return serverV.toFloat() > localV.toFloat()
//            return serverV.toFloat() != localV.toFloat()
        } catch (e: Exception) {
            XLog.e("对比firmwareversionexception: ${e.message}")
            return false
        }
    }

    /**
     * 比较appversion大小
     */
    fun checkVersion(
        remoteStr: String,
        localStr: String,
    ): Boolean {
        try {
            val regex = "[^(0-9).]"
            val remoteStrTemp = Pattern.compile(regex).matcher(remoteStr).replaceAll("").trim()
            val localStrTemp = Pattern.compile(regex).matcher(localStr).replaceAll("").trim()
            val remoteSplit = remoteStrTemp.split(".")
            val localSplit = localStrTemp.split(".")
            val minIndex = Integer.min(remoteSplit.size, localSplit.size)
            var result = false
            for (i in 0 until minIndex) {
                if (remoteSplit[i].toInt() != localSplit[i].toInt()) {
                    result = remoteSplit[i].toInt() > localSplit[i].toInt()
                    break
                }
            }
            return result
        } catch (e: Exception) {
            XLog.e("version比较出错: ${e.message}, remoteStr: $remoteStr, localStr: $localStr")
            return false
        }
    }
}
