// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\tools' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\libunified_src_main_java_com_mpdc4gsr_libunified_app_tools_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\tools' subtree
// Files: 15; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\CheckDoubleClick.kt =====

package com.mpdc4gsr.libunified.app.tools

object CheckDoubleClick {
    private val records: MutableMap<String, Long> = HashMap()
    fun isFastDoubleClick(): Boolean {
        if (records.size > 1000) {
            records.clear()
        }
        val ste = Throwable().stackTrace[1]
        val key = ste.fileName + ste.lineNumber
        var lastClickTime = records[key]
        val thisClickTime = System.currentTimeMillis()
        records[key] = thisClickTime
        if (lastClickTime == null) {
            lastClickTime = 0L
        }
        val timeDuration = thisClickTime - lastClickTime
        return timeDuration in 1..499
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\CoilLoader.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoilLoader {
    private const val TAG = "CoilLoader"
    private const val CORNER_RADIUS_DP = 6f
    private fun getPhotoOptions(context: Context): RoundedCornersTransformation {
        return RoundedCornersTransformation(CORNER_RADIUS_DP.dpToPx(context))
    }

    private fun loadCircleWithData(
        img: ImageView,
        data: Any,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(data)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        resourceId: Int,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, resourceId, options)

    fun loadCircle(
        img: ImageView,
        url: String,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, url, options)

    fun loadCircle(
        img: ImageView,
        drawable: Drawable,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, drawable, options)

    fun loadCircle(
        img: ImageView,
        uri: Uri,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, uri, options)

    fun loadCircle(
        img: ImageView,
        url: String,
        resourceId: Int,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .error(resourceId)
            .placeholder(resourceId)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    private fun loadRoundedWithData(
        img: ImageView,
        data: Any,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(data)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        resourceId: Int,
    ) = loadRoundedWithData(img, resourceId)

    fun loadRounded(
        img: ImageView,
        url: String,
    ) = loadRoundedWithData(img, url)

    fun loadRounded(
        img: ImageView,
        drawable: Drawable,
    ) = loadRoundedWithData(img, drawable)

    fun loadRounded(
        img: ImageView,
        uri: Uri,
    ) = loadRoundedWithData(img, uri)

    fun load(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.mipmap.bg_default_img)
            .error(R.mipmap.bg_default_img)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadGallery(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_gallery_default_shape)
            .error(R.drawable.ic_gallery_default_shape)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadFit(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_default_search_svg)
            .error(R.drawable.ic_default_search_svg)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun load(
        img: ImageView,
        resourceId: Int,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(resourceId)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadP(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_default_search_svg)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    suspend fun getDrawable(
        context: Context,
        url: String?,
    ): Drawable? {
        if (url == null) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                val result = context.imageLoader.execute(request)
                result.drawable
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load drawable from URL: $url", e)
                null
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\DeviceTools.kt =====

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package com.mpdc4gsr.libunified.app.tools

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isHik256
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcLiteDevice
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.utils.ByteUtils

object DeviceTools {
    fun isConnect(
        isSendConnectEvent: Boolean = false,
        isAutoRequest: Boolean = true,
    ): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcTsDevice()) {
                return if (usbManager.hasPermission(usbDevice)) {
                    XLog.i("[ph][ph][ph][ph][ph][ph][ph][ph][ph]")
                    if (isSendConnectEvent) {
                        DeviceEventManager.emitDeviceConnectionSync(true, usbDevice)
                    }
                    true
                } else {
                    XLog.w("[ph][ph][ph][ph][ph][ph][ph][ph][ph]")
                    if (isAutoRequest) {
                        DeviceEventManager.emitDevicePermissionRequestSync(usbDevice)
                    }
                    false
                }
            }
        }
        return false
    }

    fun findUsbDevice(): UsbDevice? {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcTsDevice()) {
                val productID = ByteUtils.toHexString(
                    ByteUtils.numberToBytes(
                        true,
                        usbDevice.productId.toLong(),
                        2
                    )
                )
                val vendorID = ByteUtils.toHexString(
                    ByteUtils.numberToBytes(
                        true,
                        usbDevice.vendorId.toLong(),
                        2
                    )
                )
                XLog.i("[ph][ph][ph][ph]usb[ph][ph] productId:$productID, vendorId:$vendorID, deviceName:${usbDevice.deviceName}")
                return usbDevice
            }
        }
        XLog.i("[ph][ph][ph]${deviceList.size}[ph][ph][ph], [ph][ph][ph][ph][ph][ph]usb[ph][ph]")
        return null
    }

    fun isTC001PlusConnect(): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        var usbCameraNumber = 0
        var isTcTsDev = false
        for (usbDevice in deviceList.values) {
            if ("USB Camera" == usbDevice.productName) {
                usbCameraNumber++
            }
            if (!isTcTsDev) {
                isTcTsDev = usbDevice.isTcTsDevice() && usbManager.hasPermission(usbDevice)
            }
        }
        return isTcTsDev && usbCameraNumber > 1
    }

    fun isTC001LiteConnect(): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcLiteDevice()) {
                return true
            }
        }
        return false
    }

    fun isHikConnect(): Boolean {
        val usbManager: UsbManager =
            ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        for (usbDevice in usbManager.deviceList.values) {
            if (usbDevice.isHik256()) {
                return true
            }
        }
        return false
    }

    fun requestUsb(
        activity: Activity,
        requestCode: Int,
        device: UsbDevice,
    ) {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val intent = Intent(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(activity, requestCode, intent, flag)
        usbManager.requestPermission(device, pendingIntent)
        XLog.i("[ph][ph]usb[ph][ph]")
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\FileTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.mpdc4gsr.libunified.compat.ContextProvider
import java.io.File

object FileTools {
    fun getFileSize(path: String): String {
        var str = ""
        try {
            val file = File(path)
            var len = file.length()
            if (len < 1024) {
                str = "${len}Byte"
            } else if (len < 1024 * 1024) {
                str = "${len / 1024}KB"
            } else if (len < 1024 * 1024 * 1024) {
                str = "${len / 1024 / 1024}MB"
            }
        } catch (e: Exception) {
            str = "0KB"
        }
        return str
    }

    fun getUri(file: File): Uri {
        val context = ContextProvider.getContext()
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun getImagePathFromURI(path: String): Uri? {
        val cr: ContentResolver = ContextProvider.getContext().contentResolver
        val buffer = StringBuffer()
        buffer.append("(").append(MediaStore.Images.ImageColumns.DATA)
            .append("=").append("'").append(path).append("'")
            .append(")")
        val cur: Cursor? =
            cr.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.ImageColumns._ID),
                buffer.toString(),
                null,
                null,
            )
        var index = 0
        if (cur == null) {
            return null
        }
        cur.moveToFirst()
        while (!cur.isAfterLast) {
            index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID)
            index = cur.getInt(index)
            cur.moveToNext()
        }
        return if (index != 0) {
            Uri.parse("content://media/external/images/media/$index")
        } else {
            null
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\InputTextFilterTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.widget.EditText
import java.util.regex.Pattern

class InputTextFilterTool {
    fun setEditTextFilter(editText: EditText) {
        val oldFilters = editText.filters
        val oldFiltersLength = oldFilters.size
        val newFilters = arrayOfNulls<InputFilter>(oldFiltersLength + 1)
        if (oldFiltersLength > 0) {
            System.arraycopy(oldFilters, 0, newFilters, 0, oldFiltersLength)
        }
        newFilters[oldFiltersLength] = mInputFilter
        editText.filters = newFilters
    }

    private var mInputFilter: InputFilter =
        object : InputFilter {
            var emoji =
                Pattern.compile(
                    "[^\u0020-\u007E\u00A0-\u00BE\u2E80-\uA4CF\uF900-\uFAFF\uFE30-\uFE4F\uFF00-\uFFEF\u0080-\u009F\u2000-\u201f\\r\\n]",
                    Pattern.UNICODE_CASE or Pattern.CASE_INSENSITIVE,
                )

            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int,
            ): CharSequence? {
                val emojiMatcher = emoji.matcher(source)
                if (emojiMatcher.find()) {
                    Log.w("123", "[ph][ph][ph][ph][ph][ph][ph]")
                    return ""
                }
                return null
            }
        }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\LanguageTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.Context

object LanguageTools {
    fun showLanguage(context: Context): String {
        return "English"
    }

    fun useLanguage(context: Context): String {
        return "en-WW"
    }

    fun useStatementLanguage(): String {
        return "EN"
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\NumberTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

object NumberTools {
    fun to01(float: Float): String {
        return String.format(Locale.ENGLISH, "%.1f", float)
    }

    fun to01f(float: Float): Float {
        return to01(float).toFloat()
    }

    fun to02(float: Float): String {
        return String.format(Locale.ENGLISH, "%.2f", float)
    }

    fun to02f(float: Float): Float {
        return to02(float).toFloat()
    }

    fun scale(
        value: Float,
        newScale: Int,
    ): Float {
        return BigDecimal(value.toDouble()).setScale(newScale, RoundingMode.HALF_UP).toFloat()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\PermissionTools.kt =====

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
import com.elvishew.xlog.XLog
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
        val isBtFirst: Boolean = false
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
                Type.LOCATION -> listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
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
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            callback.invoke()
            return
        }
        // Store callback for result handling
        permissionCallbacks[REQUEST_CODE_PERMISSIONS] = PermissionCallback(
            WeakReference(activity),
            type,
            callback
        )
        // Request permissions using the standard API
        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            REQUEST_CODE_PERMISSIONS
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> handlePermissionResult(requestCode, permissions, grantResults)
            REQUEST_CODE_BLUETOOTH -> handleBluetoothPermissionResult(requestCode, permissions, grantResults)
        }
    }

    private fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val callbackData = permissionCallbacks.remove(requestCode) ?: return
        val activity = callbackData.activityRef.get() ?: return
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            callbackData.callback.invoke()
        } else {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
            }
            val shouldShowRationale = deniedPermissions.any { permission ->
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
                        }
                    )
                }
            } else {
                TToast.shortToast(activity, R.string.scan_ble_tip_authorize)
            }
        }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun hasBtPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < 31) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        }
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
        val allGranted = permissionList.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            callback.onResult(true)
            return
        }
        // Store callback for result handling
        permissionCallbacks[REQUEST_CODE_BLUETOOTH] = PermissionCallback(
            WeakReference(activity),
            Type.LOCATION, // Using LOCATION type as placeholder
            {},
            callback,
            isBtFirst
        )
        // Request permissions using the standard API
        ActivityCompat.requestPermissions(
            activity,
            permissionList.toTypedArray(),
            REQUEST_CODE_BLUETOOTH
        )
    }

    private fun handleBluetoothPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val callbackData = permissionCallbacks.remove(requestCode) ?: return
        val activity = callbackData.activityRef.get() ?: return
        val callback = callbackData.btCallback ?: return
        val isBtFirst = callbackData.isBtFirst
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        XLog.i("onGranted($allGranted)")
        if (allGranted) {
            callback.onResult(true)
        } else {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
            }
            val shouldShowRationale = deniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            XLog.i("onDenied(never=${!shouldShowRationale})")
            if (!shouldShowRationale && deniedPermissions.isNotEmpty()) {
                var isBtNever = false
                var isLocationNever = false
                for (permission in deniedPermissions) {
                    if (permission == Manifest.permission.BLUETOOTH_SCAN || permission == Manifest.permission.BLUETOOTH_CONNECT) {
                        isBtNever = true
                    }
                    if (permission == Manifest.permission.ACCESS_FINE_LOCATION || permission == Manifest.permission.ACCESS_COARSE_LOCATION) {
                        isLocationNever = true
                    }
                }
                val tipDialogState = TipDialogState(activity)
                val messageResId = if (!isLocationNever || (isBtNever && isBtFirst))
                    R.string.app_bluetooth_content
                else
                    R.string.app_location_content
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
                    }
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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\ScreenTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.util.DisplayMetrics
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import kotlin.math.pow
import kotlin.math.sqrt

object ScreenTools {
    fun isLandPhone(): Boolean {
        val displayMetrics: DisplayMetrics = ContextProvider.getContext().resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        return (width / height) < 0.75f
    }

    fun isIPad(context: Context): Boolean {
        val width = ScreenUtils.getScreenWidth(context)
        val height = ScreenUtils.getScreenHeight(context)
        val densityDpi = context.resources.displayMetrics.densityDpi
        val diagonalPixels = sqrt(width.toDouble().pow(2) + height.toDouble().pow(2))
        val screenInches = diagonalPixels / densityDpi
        return screenInches >= 7f
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\SpanBuilder.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

class SpanBuilder : SpannableStringBuilder {
    constructor() : super()
    constructor(text: CharSequence) : super(text)
    constructor(text: CharSequence, start: Int, end: Int) : super(text, start, end)

    fun appendDrawable(
        context: Context,
        @DrawableRes resourceId: Int,
        @Px wantHeight: Int,
    ): SpanBuilder {
        this.append(" ")
        val oldLength = this.length
        this.append("a")
        this.setSpan(
            MyImageSpan(context, resourceId, wantHeight),
            oldLength,
            this.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        this.append(" ")
        return this
    }

    fun appendColor(
        text: CharSequence,
        @ColorInt color: Int,
    ): SpanBuilder {
        if (text.isEmpty()) {
            return this
        }
        val oldLength = this.length
        this.append(text)
        this.setSpan(
            ForegroundColorSpan(color),
            oldLength,
            this.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return this
    }

    fun appendColorAndClick(
        text: CharSequence,
        @ColorInt color: Int,
        listener: OnClickListener,
    ): SpanBuilder {
        if (text.isEmpty()) {
            return this
        }
        val oldLength = this.length
        this.append(text)
        this.setSpan(
            MyClickSpan(listener, color, false),
            oldLength,
            this.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return this
    }

    fun appendColorAndClick(
        context: Context,
        @StringRes resId: Int,
        formatArg: String,
        @ColorInt color: Int,
        hasUnderLine: Boolean = false,
        listener: OnClickListener,
    ): SpanBuilder {
        append(context.getString(resId, formatArg))
        val startIndex: Int = lastIndexOf(formatArg)
        val endIndex: Int = startIndex + formatArg.length
        this.setSpan(
            MyClickSpan(listener, color, hasUnderLine),
            startIndex,
            endIndex,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return this
    }

    private class MyClickSpan(
        val listener: OnClickListener,
        val color: Int,
        val hasUnderLine: Boolean
    ) : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            ds.color = color
            ds.isUnderlineText = hasUnderLine
        }

        override fun onClick(widget: View) {
            listener.onClick(widget)
        }
    }

    private class MyImageSpan(
        val context: Context,
        @DrawableRes val resourceId: Int,
        @Px val wantHeight: Int,
    ) : ReplacementSpan() {
        private var weakReference: WeakReference<Drawable>? = null
        fun getCachedDrawable(): Drawable {
            val weakDrawable = weakReference?.get()
            if (weakDrawable != null) {
                return weakDrawable
            }
            val drawable: Drawable = ContextCompat.getDrawable(context, resourceId)!!
            drawable.setBounds(
                0,
                0,
                (drawable.intrinsicWidth * wantHeight * 1f / drawable.intrinsicHeight).toInt(),
                wantHeight
            )
            weakReference = WeakReference(drawable)
            return drawable
        }

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            val rect = getCachedDrawable().bounds
            if (fm != null) {
                fm.ascent = -rect.bottom
                fm.descent = 0
                fm.top = fm.ascent
                fm.bottom = fm.descent
            }
            return rect.right
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint,
        ) {
            val drawable: Drawable = getCachedDrawable()
            val transY = top + (bottom - top) / 2f - drawable.getBounds().height() / 2f
            canvas.save()
            canvas.translate(x, transY)
            drawable.draw(canvas)
            canvas.restore()
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\TimeTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.annotation.SuppressLint
import android.util.Log
import com.mpdc4gsr.libunified.app.utils.CommUtils
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

object TimeTools {
    fun formatDetectTime(timeMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }

    @SuppressLint("SimpleDateFormat")
    fun getNowTime(): String {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return dateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun reportTime(time: Long): String {
        val date = Date(time)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val timeZone =
            TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun strToTime(timeStr: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val timeZone =
                TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
            dateFormat.timeZone = timeZone
            dateFormat.parse(timeStr, ParsePosition(0))?.time ?: 1609430400000
        } catch (e: Exception) {
            1609430400000
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun showDateType(
        time: Long,
        type: Int = 0,
    ): String {
        val date = Date(time)
        val pattern =
            when (type) {
                1 -> "HH:mm:ss.SSS"
                2 -> "HH:mm"
                3 -> "MM-dd HH:00"
                4 -> "yyyy-MM-dd"
                else -> "yyyy-MM-dd HH:mm:ss"
            }
        val dateFormat = SimpleDateFormat(pattern)
        val timeZone =
            TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun timeToMinute(
        time: Long,
        type: Int,
    ): Long {
        val dateFormat =
            when (type) {
                1 -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                2 -> SimpleDateFormat("yyyy-MM-dd HH:mm:00")
                3 -> SimpleDateFormat("yyyy-MM-dd HH:00:00")
                4 -> SimpleDateFormat("yyyy-MM-dd 00:00:0")
                else -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            }
        val date = Date(time)
        val str = dateFormat.format(date)
        return strToTime(str)
    }

    @SuppressLint("SimpleDateFormat")
    fun showTimeSecond(time: Long): String {
        val date = Date(time)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        val timeZone =
            TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun showDateSecond(): String {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
        val timeZone =
            TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun showVideoTime(time: Long): String {
        val totalSeconds = time / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            Formatter().format("%02d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            Formatter().format("%02d:%02d", minutes, seconds).toString()
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun showVideoLongTime(time: Long): String {
        val totalSeconds = time / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return Formatter().format("%02d:%02d:%02d", hours, minutes, seconds).toString()
    }

    fun updateDateTime(file: File): Long {
        var currentTime: Long
        val strName = file.name
        currentTime = 0L
        try {
            currentTime =
                if (strName.contains("${CommUtils.getAppName()}_")) {
                    strName.substring(6, strName.lastIndexOf(".")).toLong()
                } else {
                    file.lastModified()
                }
        } catch (e: Exception) {
            Log.e("[ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]", "${e.message}")
        }
        return currentTime
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\ToastTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R

object ToastTools {
    var mPublicToast: Toast? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    fun showShort(
        @StringRes textStr: Int,
    ) {
        showShort(ContextProvider.getContext().getString(textStr))
    }

    fun showShort(textStr: String) {
        showShort(textStr, Toast.LENGTH_SHORT)
    }

    fun showShort(
        textStr: String,
        duration: Int,
    ) {
        mainHandler.post {
            val context = ContextProvider.getContext()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.toast_tip, null)
            val text = view.findViewById(R.id.toast_tip_text) as TextView
            text.text = textStr
            val screenHeight = context.resources.displayMetrics.heightPixels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mPublicToast = Toast.makeText(context, textStr, duration)
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, screenHeight / 8)
            } else {
                if (mPublicToast == null) {
                    mPublicToast = Toast(context)
                }
                mPublicToast?.duration = duration
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, screenHeight / 8)
                @Suppress("DEPRECATION")
                mPublicToast?.view = view
            }
            mPublicToast?.show()
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\UnitTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import com.mpdc4gsr.libunified.app.common.SharedManager
import java.util.*

object UnitTools {
    @JvmStatic
    fun showC(float: Float): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                "${String.format(Locale.ENGLISH, "%.1f", float)}Â°C"
            } else {
                "${String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))}Â°F"
            }
        return str
    }

    @JvmStatic
    fun showC(
        float: Float,
        isC: Boolean,
    ): String {
        val str =
            if (isC) {
                "${String.format(Locale.ENGLISH, "%.1f", float)}Â°C"
            } else {
                "${String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))}Â°F"
            }
        return str
    }

    @JvmStatic
    fun showIntervalC(
        min: Int,
        max: Int,
    ): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                "$min~$maxÂ°C"
            } else {
                val maxT: Int = (max * 1.8000 + 32.00).toInt()
                val minT: Int = (min * 1.8000 + 32.00).toInt()
                "$minT~$maxTÂ°F"
            }
        return str
    }

    @JvmStatic
    fun showConfigC(
        min: Int,
        max: Int,
    ): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                "($min~$maxÂ°C)"
            } else {
                val maxT: Int = (max * 1.8000 + 32.00).toInt()
                val minT: Int = (min * 1.8000 + 32.00).toInt()
                "($minT~$maxTÂ°F)"
            }
        return str
    }

    @JvmStatic
    fun showUnit(): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                "Â°C"
            } else {
                "Â°F"
            }
        return str
    }

    @JvmStatic
    fun showUnitValue(value: Float): Float {
        val str =
            if (SharedManager.getTemperature() == 1) {
                value
            } else {
                toF(value)
            }
        return str.toFloat()
    }

    @JvmStatic
    fun showUnitValue(
        value: Float,
        showC: Boolean,
    ): Float {
        if (value == Float.MAX_VALUE || value == Float.MIN_VALUE) {
            return value
        }
        val str =
            if (showC) {
                value
            } else {
                toF(value)
            }
        return str.toFloat()
    }

    @JvmStatic
    fun showToCValue(
        value: Float,
        isShowC: Boolean,
    ): Float {
        val str =
            if (isShowC) {
                value
            } else {
                toC(value)
            }
        return str.toFloat()
    }

    @JvmStatic
    fun showToCValue(value: Float): Float {
        val str =
            if (SharedManager.getTemperature() == 1) {
                value
            } else {
                toC(value)
            }
        return str.toFloat()
    }

    fun toF(value: Float): Float {
        return value * 1.8000f + 32.00f
    }

    fun toC(value: Float): Float {
        return (value - 32.0f) / 1.8000f
    }

    @JvmStatic
    fun showNoUnit(float: Float): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                String.format(Locale.ENGLISH, "%.1f", float)
            } else {
                String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))
            }
        return if (str.endsWith(".0")) str.substring(0, str.length - 2) else str
    }

    @JvmStatic
    fun showWithUnit(float: Float): String {
        val str =
            if (SharedManager.getTemperature() == 1) {
                String.format(Locale.ENGLISH, "%.1f", float)
            } else {
                String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))
            }
        return (if (str.endsWith(".0")) str.substring(0, str.length - 2) else str) + showUnit()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\VersionTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import com.elvishew.xlog.XLog
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
            XLog.e("[ph][ph][ph][ph][ph][ph][ph][ph]: ${e.message}")
            return false
        }
    }

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
            XLog.e("[ph][ph][ph][ph][ph][ph]: ${e.message}, remoteStr: $remoteStr, localStr: $localStr")
            return false
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\tools\VideoTools.kt =====

package com.mpdc4gsr.libunified.app.tools

import android.media.MediaMetadataRetriever

object VideoTools {
    fun getLocalVideoDuration(videoPath: String): Long {
        return if (videoPath.uppercase().endsWith(".MP4") || videoPath.uppercase()
                .endsWith(".AVI")
        ) {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(videoPath)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }
}