// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\config' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\config\libunified_src_main_java_com_mpdc4gsr_libunified_app_config_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\config' subtree
// Files: 7; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\AppConfig.kt =====

package com.mpdc4gsr.libunified.app.config

object AppConfig {
    const val GOOGLE_APK_URL =
        "https://play.google.com/store/apps/details?id=com.mpdc4gsr.MPDC4GSR"
    const val GOOGLE_APK_MARKET_URL = "market://details?id=com.mpdc4gsr.MPDC4GSR"
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\DeviceConfig.kt =====

package com.mpdc4gsr.libunified.app.config

import android.hardware.usb.UsbDevice

object DeviceConfig {
    const val IR_VENDOR_ID = 0x0BDA
    const val IR_PRODUCT_ID = 0x5840
    const val TS004_NAME_START = "TS004_"
    const val TS004_PASSWORD = "TS004001"
    const val TC007_NAME_START = "TC007_"
    const val TC007_PASSWORD = "12345678"
    const val TOPDON_VENDOR_ID = 0x0BDA
    const val TOPDON_PRODUCT_ID = 0x5830
    const val TCLITE_VENDOR_ID = 13428
    const val TCLITE_PRODUCT_ID = 17185
    const val HIK_VENDOR_ID = 11231
    const val HIK_PRODUCT_ID = 258
    fun UsbDevice.isTcTsDevice(): Boolean {
        return (productId == TOPDON_PRODUCT_ID && vendorId == TOPDON_VENDOR_ID) ||
                (productId == IR_PRODUCT_ID && vendorId == IR_VENDOR_ID) ||
                (productId == TCLITE_PRODUCT_ID && vendorId == TCLITE_VENDOR_ID) ||
                (productId == HIK_PRODUCT_ID && vendorId == HIK_VENDOR_ID)
    }

    fun UsbDevice.isTcLiteDevice(): Boolean {
        return (productId == TCLITE_PRODUCT_ID && vendorId == TCLITE_VENDOR_ID)
    }

    fun UsbDevice.isHik256(): Boolean = productId == HIK_PRODUCT_ID && vendorId == HIK_VENDOR_ID
    const val SKU = "TDTC001A11"
    const val SN = "TC001A11000001"
    const val ROTATE_ANGLE = 0
    const val IS_PORTRAIT = false
    const val S_ROTATE_ANGLE = 270
    const val S_IS_PORTRAIT = true
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\ExtraKeyConfig.kt =====

package com.mpdc4gsr.libunified.app.config

object ExtraKeyConfig {
    const val IS_PICK_REPORT_IMG = "IS_PICK_REPORT_IMG"
    const val IS_VIDEO = "IS_VIDEO"
    const val HAS_BACK_ICON = "HAS_BACK_ICON"
    const val CAN_SWITCH_DIR = "CAN_SWITCH_DIR"
    const val IS_TC007 = "IS_TC007"
    const val IS_PICK_INSPECTOR = "IS_PICK_INSPECTOR"
    const val IS_REPORT = "IS_REPORT"
    const val DIR_TYPE = "CUR_DIR_TYPE"
    const val CURRENT_ITEM = "CURRENT_ITEM"
    const val DETECT_ID = "DETECT_ID"
    const val DIR_ID = "DIR_ID"
    const val LONG_ID = "LONG_ID"
    const val URL = "URL"
    const val FILE_ABSOLUTE_PATH = "FILE_ABSOLUTE_PATH"
    const val ITEM_NAME = "ITEM_NAME"
    const val RESULT_INPUT_TEXT = "RESULT_INPUT_TEXT"
    const val RESULT_IMAGE_PATH = "RESULT_IMAGE_PATH"
    const val RESULT_PATH_WHITE = "RESULT_PATH_WHITE"
    const val RESULT_PATH_BLACK = "RESULT_PATH_BLACK"
    const val IMAGE_PATH_LIST = "IMAGE_PATH_LIST"
    const val IMAGE_TEMP_BEAN = "IMAGE_TEMP_BEAN"
    const val REPORT_BEAN = "REPORT_BEAN"
    const val REPORT_INFO = "REPORT_INFO"
    const val REPORT_CONDITION = "REPORT_CONDITION"
    const val REPORT_IR_LIST = "REPORT_IR_LIST"
    const val CUSTOM_PSEUDO_BEAN = "CUSTOM_PSEUDO_BEAN"
    const val TIME_MILLIS = "TIME_MILLIS"
    const val MONITOR_TYPE = "MONITOR_TYPE"
    const val IR_PATH = "ir_path"
    const val TEMP_HIGH = "temp_high"
    const val TEMP_LOW = "temp_low"
    const val IS_CAR_DETECT_ENTER = "IS_CAR_DETECT_ENTER"
    const val reportId = "REPORT_ID"
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\FileConfig.kt =====

package com.mpdc4gsr.libunified.app.config

import android.content.Context
import android.os.Build
import android.os.Environment
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.libunified.app.utils.CommUtils
import java.io.File

object FileConfig {
    fun getDetectImageDir(
        context: Context,
        child: String,
    ): File {
        val externalDir = context.getExternalFilesDir("detect")
        return if (externalDir == null) {
            val fileDir = File(context.filesDir, "detect")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            File(fileDir, child)
        } else {
            File(externalDir, child)
        }
    }

    fun getSignImageDir(
        context: Context,
        child: String,
    ): File {
        val externalDir = context.getExternalFilesDir("sign")
        return if (externalDir == null) {
            val fileDir = File(context.filesDir, "sign")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            File(fileDir, child)
        } else {
            File(externalDir, child)
        }
    }

    fun getFirmwareFile(filename: String): File =
        File(ContextProvider.getContext().getExternalFilesDir("firmware"), filename)

    @JvmStatic
    fun getPdfDir(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            val path = dir + File.separator + CommUtils.getAppName() + File.separator + "pdf"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            path
        } else {
            Environment.DIRECTORY_DOCUMENTS + "/${CommUtils.getAppName()}/pdf"
        }
    }

    @JvmStatic
    val excelDir: String
        get() {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val dir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                val path = dir + File.separator + CommUtils.getAppName() + File.separator + "excel"
                val file = File(path)
                if (!file.exists()) {
                    file.mkdirs()
                }
                path
            } else {
                Environment.DIRECTORY_DOCUMENTS + "/${CommUtils.getAppName()}/excel"
            }
        }

    @JvmStatic
    val gallerySourDir: String
        get() {
            val dir = ContextProvider.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return ""
            val result = dir.absolutePath + File.separator + "MPDC4GSR"
            val file = File(result)
            if (!file.exists()) {
                file.mkdirs()
            }
            return result
        }

    @JvmStatic
    val oldTc001GalleryDir: String
        get() {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            val path = dir + File.separator + "TC001"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    fun getGalleryDirByType(currentDirType: DirType): String =
        when (currentDirType) {
            DirType.LINE -> lineGalleryDir
            DirType.TC007 -> tc007GalleryDir
            else -> ts004GalleryDir
        }

    @JvmStatic
    val lineGalleryDir: String
        get() {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            val path = dir + File.separator + CommUtils.getAppName()
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    @JvmStatic
    val ts004GalleryDir: String
        get() {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            val path = dir + File.separator + "TS004"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    @JvmStatic
    val tc007GalleryDir: String
        get() {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            val path = dir + File.separator + "TC007"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    @JvmStatic
    val lineIrGalleryDir: String
        get() {
            val dcimDir = ContextProvider.getContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)
                ?: return ""
            val dir = dcimDir.absolutePath
            val path = dir + File.separator + "${CommUtils.getAppName()}-ir"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    @JvmStatic
    val tc007IrGalleryDir: String
        get() {
            val dcimDir = ContextProvider.getContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)
                ?: return ""
            val dir = dcimDir.absolutePath
            val path = dir + File.separator + "TC007-ir"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return path
        }

    @JvmStatic
    val documentsDir: String
        get() {
            return if (Build.VERSION.SDK_INT < 29) {
                val dir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                val path = dir + File.separator + CommUtils.getAppName() + "/house"
                val file = File(path)
                if (!file.exists()) {
                    file.mkdirs()
                }
                path
            } else {
                Environment.DIRECTORY_DOCUMENTS + File.separator + CommUtils.getAppName() + "/house/"
            }
        }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\HttpConfig.kt =====

package com.mpdc4gsr.libunified.app.config

object HttpConfig {
    const val HOST = "https://api.topdon.com"
    const val AUTH_SECRET =
        "vG8XVT/yWcJiqSVlIC2zRRhBmoSTIiRU2520KGIjop4ISKwDjUWXZEADpvFEMH3DT8OgEOsnOs5Auts0WKpxbhE5AGla3YZiVJCHugkSr5UvHDSbs5Ft74wO21Lwj4cDvQw8+hewpmwZS54cpSnSgXLO+2GEcR767dKwwgXSpqx1S8j51uFoxlWwr5CFSJdXinxwQyg26EzjbaqKXa8ViaqUFgi+17Qd9A5lY0p6fsEAtOeoqspQmD5ugKkwUmoy7/HzBrQXfYRGPCXwkBUq7S0DwmM1O918wdqGIQcSm9W8xUgBqyXDVQ=="

    @Volatile
    var hasNewVersion = false
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router\DegradeServiceImpl.kt =====

package com.mpdc4gsr.libunified.app.config.router

import android.content.Context
import android.widget.Toast
import com.elvishew.xlog.XLog

class DegradeServiceImpl {
    fun init(context: Context?) {
    }

    fun onLost(
        context: Context?,
        path: String?,
    ) {
        if (context != null) {
            Toast.makeText(context, "Navigation failed: $path", Toast.LENGTH_SHORT).show()
            XLog.e("Navigation failed to path: $path")
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\RouterConfig.kt =====

package com.mpdc4gsr.libunified.app.config

object RouterConfig {
    private const val GROUP_APP = "app"
    private const val GROUP_IR = "ir"
    private const val GROUP_HIK_IR = "irHik"
    private const val GROUP_USER = "user"
    private const val GROUP_REPORT = "report"
    private const val GROUP_THERMAL04 = "ts004"
    private const val GROUP_THERMAL07 = "tc007"
    private const val GROUP_CALIBRATE = "calibrate"
    private const val GROUP_GSR = "gsr"
    const val MAIN = "/$GROUP_APP/main"
    const val CLAUSE = "/$GROUP_APP/clause"
    const val POLICY = "/$GROUP_APP/policy"
    const val VERSION = "/$GROUP_APP/version"
    const val PDF = "/$GROUP_APP/app/pdf"
    const val IR_MORE_HELP = "/$GROUP_APP/app/more_help"
    const val IR_GALLERY_EDIT = "/$GROUP_APP/gallery/edit"
    const val WEB_VIEW = "/$GROUP_APP/WebViewActivity"
    const val IR_HIK_MAIN = "/$GROUP_HIK_IR/irHikMain"
    const val IR_HIK_CORRECT_THREE = "/$GROUP_HIK_IR/correction3"
    const val IR_HIK_MONITOR_CAPTURE1 = "/$GROUP_HIK_IR/monitorCap1"
    const val IR_HIK_IMG_PICK = "/$GROUP_HIK_IR/ImagePick"
    const val IR_MAIN = "/$GROUP_IR/irMain"
    const val IR_FRAME = "/$GROUP_IR/frame"
    const val IR_SETTING = "/$GROUP_IR/setting"
    const val IR_THERMAL_MONITOR = "/$GROUP_IR/monitor"
    const val IR_MONITOR_CHART = "/$GROUP_IR/monitor/chart"
    const val IR_THERMAL_LOG_MP_CHART = "/$GROUP_IR/log/mp/chart"
    const val IR_GALLERY_HOME = "/$GROUP_IR/gallery/home"
    const val IR_GALLERY_DETAIL_01 = "/$GROUP_IR/gallery/detail01"
    const val IR_GALLERY_DETAIL_04 = "/$GROUP_IR/gallery/detail04"
    const val IR_VIDEO_GSY = "/$GROUP_IR/video/gsy"
    const val IR_CAMERA_SETTING = "/$GROUP_IR/camera/setting"
    const val IR_CORRECTION = "/$GROUP_IR/correction"
    const val IR_CORRECTION_TWO = "/$GROUP_IR/correction2"
    const val IR_CORRECTION_THREE = "/$GROUP_IR/correction3"
    const val IR_CORRECTION_FOUR = "/$GROUP_IR/correction4"
    const val IR_IMG_PICK = "/$GROUP_IR/ImagePickIRActivity"
    const val IR_IMG_PICK_PLUS = "/$GROUP_IR/ImagePickIRPlushActivity"
    const val IR_MODEL = "/$GROUP_IR/model"
    const val IR_DUAL = "/$GROUP_IR/dual"
    const val IR_THERMAL = "/$GROUP_IR/thermal"
    const val IR_GALLERY_3D = "/menu/Image3DActivity"
    const val IR_MONOCULAR = "/$GROUP_THERMAL04/IRMonocularActivity"
    const val IR_DEVICE_ADD = "/$GROUP_THERMAL04/DeviceAddActivity"
    const val IR_CONNECT_TIPS = "/$GROUP_THERMAL04/ConnectTipsActivity"
    const val IR_THERMAL_07 = "/$GROUP_THERMAL07/IRThermal07Activity"
    const val IR_MONITOR_CAPTURE_07 = "/$GROUP_THERMAL07/MonitorCapture1"
    const val IR_CORRECTION_07 = "/$GROUP_THERMAL07/IR07CorrectionThreeActivity"
    const val IR_IMG_PICK_07 = "/$GROUP_THERMAL07/ImagePickTC007Activity"
    const val REPORT_CREATE_FIRST = "/$GROUP_REPORT/create/first"
    const val REPORT_CREATE_SECOND = "/$GROUP_REPORT/create/second"
    const val REPORT_PREVIEW_FIRST = "/$GROUP_REPORT/preview/first"
    const val REPORT_PREVIEW_SECOND = "/$GROUP_REPORT/preview/second"
    const val REPORT_DETAIL = "/$GROUP_REPORT/detail"
    const val REPORT_LIST = "/$GROUP_REPORT/list"
    const val REPORT_PICK_IMG = "/$GROUP_REPORT/pick/img"
    const val REPORT_PREVIEW = "/$GROUP_REPORT/preview"
    const val QUESTION = "/$GROUP_USER/question"
    const val QUESTION_DETAILS = "/$GROUP_USER/question/details"
    const val UNIT = "/$GROUP_USER/unit"
    const val TS004_MORE = "/$GROUP_USER/ts004More"
    const val TC_MORE = "/$GROUP_USER/tcMore"
    const val DEVICE_INFORMATION = "/$GROUP_USER/device_information"
    const val TISR = "/$GROUP_USER/tisr"
    const val ELECTRONIC_MANUAL = "/$GROUP_USER/electronic_manual"
    const val STORAGE_SPACE = "/$GROUP_USER/storage_space"
    const val AUTO_SAVE = "/$GROUP_USER/auto_save"
    const val MANUAL_START = "/$GROUP_CALIBRATE/manual/first"
    const val IR_FRAME_PLUSH = "/$GROUP_IR/frame/plush"
    const val IR_TCLITE = "/lite/tcLite"
    const val IR_THERMAL_MONITOR_LITE = "/lite/monitor"
    const val IR_IMG_PICK_LITE = "/lite/ImagePickIRLiteActivity"
    const val IR_MONITOR_CHART_LITE = "/lite/monitor/chart"
    const val IR_CORRECTION_THREE_LITE = "/lite/correction3"
    const val IR_CORRECTION_FOUR_LITE = "/lite/correction4"
    const val GSR_MULTI_MODAL = "/$GROUP_GSR/multimodal"
    const val GSR_DEMO = "/$GROUP_GSR/demo"
    const val GALLERY = "/thermal/gallery"
    const val THERMAL_MONITOR = "/thermal/monitor"
    const val CONNECT = "/thermal/connect"
    const val VIDEO = "/thermal/video"
    const val MONITOR_CHART = "/thermal/monitor/chart"
    const val LOG_MP_CHART = "/thermal/log/mp/chart"

    // Settings routes
    const val GENERAL_SETTINGS = "/$GROUP_USER/settings/general"
    const val THERMAL_SETTINGS = "/$GROUP_USER/settings/thermal"
    const val NETWORK_SETTINGS = "/$GROUP_USER/settings/network"
    const val STORAGE_SETTINGS = "/$GROUP_USER/settings/storage"
    const val ABOUT = "/$GROUP_USER/about"

    // Help and support routes
    const val USER_GUIDE = "/$GROUP_USER/help/guide"
    const val FAQ = "/$GROUP_USER/help/faq"
    const val TROUBLESHOOTING = "/$GROUP_USER/help/troubleshooting"
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router\libunified_src_main_java_com_mpdc4gsr_libunified_app_config_router_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router\DegradeServiceImpl.kt =====

package com.mpdc4gsr.libunified.app.config.router

import android.content.Context
import android.widget.Toast
import com.elvishew.xlog.XLog

class DegradeServiceImpl {
    fun init(context: Context?) {
    }

    fun onLost(
        context: Context?,
        path: String?,
    ) {
        if (context != null) {
            Toast.makeText(context, "Navigation failed: $path", Toast.LENGTH_SHORT).show()
            XLog.e("Navigation failed to path: $path")
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\config\RouterConfig.kt =====

package com.mpdc4gsr.libunified.app.config

object RouterConfig {
    private const val GROUP_APP = "app"
    private const val GROUP_IR = "ir"
    private const val GROUP_HIK_IR = "irHik"
    private const val GROUP_USER = "user"
    private const val GROUP_REPORT = "report"
    private const val GROUP_THERMAL04 = "ts004"
    private const val GROUP_THERMAL07 = "tc007"
    private const val GROUP_CALIBRATE = "calibrate"
    private const val GROUP_GSR = "gsr"
    const val MAIN = "/$GROUP_APP/main"
    const val CLAUSE = "/$GROUP_APP/clause"
    const val POLICY = "/$GROUP_APP/policy"
    const val VERSION = "/$GROUP_APP/version"
    const val PDF = "/$GROUP_APP/app/pdf"
    const val IR_MORE_HELP = "/$GROUP_APP/app/more_help"
    const val IR_GALLERY_EDIT = "/$GROUP_APP/gallery/edit"
    const val WEB_VIEW = "/$GROUP_APP/WebViewActivity"
    const val IR_HIK_MAIN = "/$GROUP_HIK_IR/irHikMain"
    const val IR_HIK_CORRECT_THREE = "/$GROUP_HIK_IR/correction3"
    const val IR_HIK_MONITOR_CAPTURE1 = "/$GROUP_HIK_IR/monitorCap1"
    const val IR_HIK_IMG_PICK = "/$GROUP_HIK_IR/ImagePick"
    const val IR_MAIN = "/$GROUP_IR/irMain"
    const val IR_FRAME = "/$GROUP_IR/frame"
    const val IR_SETTING = "/$GROUP_IR/setting"
    const val IR_THERMAL_MONITOR = "/$GROUP_IR/monitor"
    const val IR_MONITOR_CHART = "/$GROUP_IR/monitor/chart"
    const val IR_THERMAL_LOG_MP_CHART = "/$GROUP_IR/log/mp/chart"
    const val IR_GALLERY_HOME = "/$GROUP_IR/gallery/home"
    const val IR_GALLERY_DETAIL_01 = "/$GROUP_IR/gallery/detail01"
    const val IR_GALLERY_DETAIL_04 = "/$GROUP_IR/gallery/detail04"
    const val IR_VIDEO_GSY = "/$GROUP_IR/video/gsy"
    const val IR_CAMERA_SETTING = "/$GROUP_IR/camera/setting"
    const val IR_CORRECTION = "/$GROUP_IR/correction"
    const val IR_CORRECTION_TWO = "/$GROUP_IR/correction2"
    const val IR_CORRECTION_THREE = "/$GROUP_IR/correction3"
    const val IR_CORRECTION_FOUR = "/$GROUP_IR/correction4"
    const val IR_IMG_PICK = "/$GROUP_IR/ImagePickIRActivity"
    const val IR_IMG_PICK_PLUS = "/$GROUP_IR/ImagePickIRPlushActivity"
    const val IR_MODEL = "/$GROUP_IR/model"
    const val IR_DUAL = "/$GROUP_IR/dual"
    const val IR_THERMAL = "/$GROUP_IR/thermal"
    const val IR_GALLERY_3D = "/menu/Image3DActivity"
    const val IR_MONOCULAR = "/$GROUP_THERMAL04/IRMonocularActivity"
    const val IR_DEVICE_ADD = "/$GROUP_THERMAL04/DeviceAddActivity"
    const val IR_CONNECT_TIPS = "/$GROUP_THERMAL04/ConnectTipsActivity"
    const val IR_THERMAL_07 = "/$GROUP_THERMAL07/IRThermal07Activity"
    const val IR_MONITOR_CAPTURE_07 = "/$GROUP_THERMAL07/MonitorCapture1"
    const val IR_CORRECTION_07 = "/$GROUP_THERMAL07/IR07CorrectionThreeActivity"
    const val IR_IMG_PICK_07 = "/$GROUP_THERMAL07/ImagePickTC007Activity"
    const val REPORT_CREATE_FIRST = "/$GROUP_REPORT/create/first"
    const val REPORT_CREATE_SECOND = "/$GROUP_REPORT/create/second"
    const val REPORT_PREVIEW_FIRST = "/$GROUP_REPORT/preview/first"
    const val REPORT_PREVIEW_SECOND = "/$GROUP_REPORT/preview/second"
    const val REPORT_DETAIL = "/$GROUP_REPORT/detail"
    const val REPORT_LIST = "/$GROUP_REPORT/list"
    const val REPORT_PICK_IMG = "/$GROUP_REPORT/pick/img"
    const val REPORT_PREVIEW = "/$GROUP_REPORT/preview"
    const val QUESTION = "/$GROUP_USER/question"
    const val QUESTION_DETAILS = "/$GROUP_USER/question/details"
    const val UNIT = "/$GROUP_USER/unit"
    const val TS004_MORE = "/$GROUP_USER/ts004More"
    const val TC_MORE = "/$GROUP_USER/tcMore"
    const val DEVICE_INFORMATION = "/$GROUP_USER/device_information"
    const val TISR = "/$GROUP_USER/tisr"
    const val ELECTRONIC_MANUAL = "/$GROUP_USER/electronic_manual"
    const val STORAGE_SPACE = "/$GROUP_USER/storage_space"
    const val AUTO_SAVE = "/$GROUP_USER/auto_save"
    const val MANUAL_START = "/$GROUP_CALIBRATE/manual/first"
    const val IR_FRAME_PLUSH = "/$GROUP_IR/frame/plush"
    const val IR_TCLITE = "/lite/tcLite"
    const val IR_THERMAL_MONITOR_LITE = "/lite/monitor"
    const val IR_IMG_PICK_LITE = "/lite/ImagePickIRLiteActivity"
    const val IR_MONITOR_CHART_LITE = "/lite/monitor/chart"
    const val IR_CORRECTION_THREE_LITE = "/lite/correction3"
    const val IR_CORRECTION_FOUR_LITE = "/lite/correction4"
    const val GSR_MULTI_MODAL = "/$GROUP_GSR/multimodal"
    const val GSR_DEMO = "/$GROUP_GSR/demo"
    const val GALLERY = "/thermal/gallery"
    const val THERMAL_MONITOR = "/thermal/monitor"
    const val CONNECT = "/thermal/connect"
    const val VIDEO = "/thermal/video"
    const val MONITOR_CHART = "/thermal/monitor/chart"
    const val LOG_MP_CHART = "/thermal/log/mp/chart"

    // Settings routes
    const val GENERAL_SETTINGS = "/$GROUP_USER/settings/general"
    const val THERMAL_SETTINGS = "/$GROUP_USER/settings/thermal"
    const val NETWORK_SETTINGS = "/$GROUP_USER/settings/network"
    const val STORAGE_SETTINGS = "/$GROUP_USER/settings/storage"
    const val ABOUT = "/$GROUP_USER/about"

    // Help and support routes
    const val USER_GUIDE = "/$GROUP_USER/help/guide"
    const val FAQ = "/$GROUP_USER/help/faq"
    const val TROUBLESHOOTING = "/$GROUP_USER/help/troubleshooting"
}