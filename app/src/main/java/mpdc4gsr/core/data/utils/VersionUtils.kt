package mpdc4gsr.core.data.utils

import android.content.Context
import com.mpdc4gsr.component.shared.app.utils.SharedVersionUtils

object VersionUtils {
    fun getCodeStr(context: Context): String = SharedVersionUtils.getVersionName(context)

    fun getVersionName(context: Context): String = SharedVersionUtils.getVersionName(context)

    fun getVersionCode(context: Context): Long = SharedVersionUtils.getVersionCode(context)

    fun isUpdateNeeded(
        context: Context,
        serverVersion: String,
    ): Boolean = SharedVersionUtils.isUpdateNeeded(context, serverVersion)
}



