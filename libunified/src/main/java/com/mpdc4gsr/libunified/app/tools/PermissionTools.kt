package com.mpdc4gsr.libunified.app.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.compose.dialogs.TipDialogState
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import java.lang.ref.WeakReference

object PermissionTools {
    private const val REQUEST_CODE_PERMISSIONS = 1001
    private const val REQUEST_CODE_BLUETOOTH = 1002
    private var permissionCallbacks = mutableMapOf<Int, PermissionCallback>()

    private data class PermissionCallback(
        val activityRef: WeakReference<FragmentActivity>,
        val type: Type,
        val callback: () -> Unit,
        val btCallback: Callback? = null,
        val isBtFirst: Boolean = false,
    )

    fun requestRecordAudio(
        activity: FragmentActivity,
        callback: () -> Unit,
    ) = request(activity, Type.RECORD_AUDIO, callback)

    fun requestCamera(
        activity: FragmentActivity,
        callback: () -> Unit,
    ) = request(activity, Type.CAMERA, callback)

    fun requestLocation(
        activity: FragmentActivity,
        callback: () -> Unit,
    ) = request(activity, Type.LOCATION, callback)

    fun requestImageRead(
        activity: FragmentActivity,
        callback: () -> Unit,
    ) = request(activity, Type.IMAGE, callback)

    fun requestFile(
        activity: FragmentActivity,
        callback: () -> Unit,
    ) = request(activity, Type.FILE, callback)

    private enum class Type { RECORD_AUDIO, CAMERA, LOCATION, IMAGE, FILE }

    private fun request(
        activity: FragmentActivity,
        type: Type,
        callback: () -> Unit,
    ) {
        val permissions: List<String> =
            when (type) {
                Type.RECORD_AUDIO -> listOf(Manifest.permission.RECORD_AUDIO)
                Type.CAMERA -> listOf(Manifest.permission.CAMERA)
                Type.LOCATION ->
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )

                Type.IMAGE ->
                    listOf(
                        if (activity.applicationInfo.targetSdkVersion < 33) {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        } else {
                            Manifest.permission.READ_MEDIA_IMAGES
                        },
                    )

                Type.FILE ->
                    if (activity.applicationInfo.targetSdkVersion < 30) {
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else if (activity.applicationInfo.targetSdkVersion < 33) {
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
                    }
            }
        // Check if permissions are already granted
        val allGranted =
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        if (allGranted) {
            callback.invoke()
            return
        }
        // Store callback for result handling
        permissionCallbacks[REQUEST_CODE_PERMISSIONS] =
            PermissionCallback(
                WeakReference(activity),
                type,
                callback,
            )
        // Request permissions using the standard API
        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            REQUEST_CODE_PERMISSIONS,
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> handlePermissionResult(requestCode, permissions, grantResults)
            REQUEST_CODE_BLUETOOTH -> handleBluetoothPermissionResult(requestCode, permissions, grantResults)
        }
    }

    private fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val callbackData = permissionCallbacks.remove(requestCode) ?: return
        val activity = callbackData.activityRef.get() ?: return
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            callbackData.callback.invoke()
        } else {
            val deniedPermissions =
                permissions.filterIndexed { index, _ ->
                    grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
                }
            val shouldShowRationale =
                deniedPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }
            if (!shouldShowRationale && deniedPermissions.isNotEmpty()) {
                val tipsResId: Int =
                    when (callbackData.type) {
                        Type.RECORD_AUDIO -> R.string.app_microphone_content
                        Type.CAMERA -> R.string.app_camera_content
                        Type.LOCATION -> R.string.app_location_content
                        Type.IMAGE -> R.string.app_album_content
                        Type.FILE -> R.string.app_storage_content
                    }
                if (BaseApplication.instance.isDomestic()) {
                    TToast.shortToast(activity, tipsResId)
                } else {
                    val tipDialogState = TipDialogState(activity)
                    tipDialogState.show(
                        title = activity.getString(R.string.app_tip),
                        message = activity.getString(tipsResId),
                        showCancel = true,
                        positiveText = activity.getString(R.string.app_open),
                        negativeText = activity.getString(R.string.app_cancel),
                        onPositive = {
                            openAppSettings(activity)
                        },
                    )
                }
            } else {
                TToast.shortToast(activity, R.string.scan_ble_tip_authorize)
            }
        }
    }

    private fun openAppSettings(context: Context) {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    fun hasBtPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < 31) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
        }

    fun requestBluetooth(
        activity: FragmentActivity,
        isBtFirst: Boolean,
        callback: Callback,
    ) {
        val permissionList: List<String> =
            if (Build.VERSION.SDK_INT < 31) {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            }
        // Check if permissions are already granted
        val allGranted =
            permissionList.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        if (allGranted) {
            callback.onResult(true)
            return
        }
        // Store callback for result handling
        permissionCallbacks[REQUEST_CODE_BLUETOOTH] =
            PermissionCallback(
                WeakReference(activity),
                Type.LOCATION, // Using LOCATION type as placeholder
                {},
                callback,
                isBtFirst,
            )
        // Request permissions using the standard API
        ActivityCompat.requestPermissions(
            activity,
            permissionList.toTypedArray(),
            REQUEST_CODE_BLUETOOTH,
        )
    }

    private fun handleBluetoothPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val callbackData = permissionCallbacks.remove(requestCode) ?: return
        val activity = callbackData.activityRef.get() ?: return
        val callback = callbackData.btCallback ?: return
        val isBtFirst = callbackData.isBtFirst
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            callback.onResult(true)
        } else {
            val deniedPermissions =
                permissions.filterIndexed { index, _ ->
                    grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
                }
            val shouldShowRationale =
                deniedPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }
            if (!shouldShowRationale && deniedPermissions.isNotEmpty()) {
                var isBtNever = false
                var isLocationNever = false
                for (permission in deniedPermissions) {
                    if (permission == Manifest.permission.BLUETOOTH_SCAN || permission == Manifest.permission.BLUETOOTH_CONNECT) {
                        isBtNever = true
                    }
                    if (permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                        permission == Manifest.permission.ACCESS_COARSE_LOCATION
                    ) {
                        isLocationNever = true
                    }
                }
                val tipDialogState = TipDialogState(activity)
                val messageResId =
                    if (!isLocationNever || (isBtNever && isBtFirst)) {
                        R.string.app_bluetooth_content
                    } else {
                        R.string.app_location_content
                    }
                tipDialogState.show(
                    title = activity.getString(R.string.app_tip),
                    message = activity.getString(messageResId),
                    showCancel = true,
                    positiveText = activity.getString(R.string.app_open),
                    negativeText = activity.getString(R.string.app_cancel),
                    cancelable = true,
                    onPositive = {
                        openAppSettings(activity)
                        callback.onNever(true)
                    },
                    onNegative = {
                        callback.onNever(false)
                    },
                )
            } else {
                callback.onResult(false)
            }
        }
    }

    interface Callback {
        fun onResult(allGranted: Boolean)

        fun onNever(isJump: Boolean)
    }
}
