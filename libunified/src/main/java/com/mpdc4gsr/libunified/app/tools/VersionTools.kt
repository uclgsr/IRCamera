package com.mpdc4gsr.libunified.app.tools

import com.mpdc4gsr.libunified.app.utils.LibraryLogger
import java.util.regex.Pattern

object VersionTools {
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
                LibraryLogger.e("VersionTools", "Unexpected Exception in VersionTools catch block", e)
            }
        }
        return versionStr
    }

    fun checkNewVersion(
        serverVersionStr: String,
        localVersionStr: String,
    ): Boolean {
        try {
            val serverV = getVersion(serverVersionStr)
            val localV = getVersion(localVersionStr)
            return serverV.toFloat() > localV.toFloat()
        } catch (e: Exception) {
            return false
        }
    }

    fun checkVersion(
        remoteStr: String,
        localStr: String,
    ): Boolean {
        try {
            val regex = "[^(0-9).]"
            val remoteStrTemp =
                Pattern
                    .compile(regex)
                    .matcher(remoteStr)
                    .replaceAll("")
                    .trim()
            val localStrTemp =
                Pattern
                    .compile(regex)
                    .matcher(localStr)
                    .replaceAll("")
                    .trim()
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
            return false
        }
    }
}
