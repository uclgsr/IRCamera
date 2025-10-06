package mpdc4gsr.core.data.utils

import android.content.Context
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils

object VersionUtils {

    fun getCodeStr(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }

    fun getVersionName(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }

    fun getVersionCode(context: Context): Long {
        return UnifiedVersionUtils.getVersionCode(context)
    }

    fun isUpdateNeeded(context: Context, serverVersion: String): Boolean {
        return UnifiedVersionUtils.isUpdateNeeded(context, serverVersion)
    }
}