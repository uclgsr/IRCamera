package com.topdon.lib.core.utils

import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.content.ContextCompat
import com.topdon.lib.core.BaseApplication

/**
 * Unified handling of Android 14 permissions
 * Author: CaiSongL
 * Date: 2024/9/9 9:45
 */
object PermissionUtils {
    /**
     * Check if Android 14 has granted partial read permissions
     * @return Boolean
     */
    fun isVisualUser(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                BaseApplication.instance,
                READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PERMISSION_GRANTED
    }

    /**
     * Executes hascamerapermission functionality.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            BaseApplication.instance,
            android.Manifest.permission.CAMERA,
        ) == PERMISSION_GRANTED
    }
}
