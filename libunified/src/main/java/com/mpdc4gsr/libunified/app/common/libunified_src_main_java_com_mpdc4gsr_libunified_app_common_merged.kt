// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\common' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\common\ProductType.kt =====

package com.mpdc4gsr.libunified.app.common

object ProductType {
    const val PRODUCT_NAME_TC = "TC001"
    const val PRODUCT_NAME_TS = "TS001"
    const val PRODUCT_NAME_TCP = "TC_PLUS"

    // PRODUCT_NAME_TC007 removed
    // PRODUCT_NAME_TS004 removed
    const val PRODUCT_NAME_TC007 = "TC007"  // Re-added for compatibility
    const val PRODUCT_NAME_TC001LITE = "TCLite"
    const val PRODUCT_NAME_TC002C_DUO = "TC002C_DUO"
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\common\SaveSettingUtils.kt =====

package com.mpdc4gsr.libunified.app.common

import android.util.TypedValue
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.compat.SPUtils

object SaveSettingUtils {
    private const val SP_NAME = "SaveSettingUtils"
    const val FusionTypeLPYFusion = 4
    const val FusionTypeMeanFusion = 2
    const val FusionTypeIROnly = 1
    const val FusionTypeVLOnly = 0

    // FusionTypeTC007Fusion constant removed - TC007 device support discontinued
    const val FusionTypeHSLFusion = 3
    const val FusionTypeScreenFusion = 5
    const val FusionTypeIROnlyNoFusion = 6
    fun reset() {
        isMeasureTempMode = true
        isVideoMode = false
        isAutoShutter = true
        isRecordAudio = false
        isOpenMirror = false
        delayCaptureSecond = 0
        contrastValue = 128
        pseudoColorMode = 3
        rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        isOpenPseudoBar = true
        isOpenTwoLight = false
        twoLightAlpha = 50
        ddeConfig = 2
        tempTextColor = 0xffffffff.toInt()
        temperatureMode = CameraItemBean.TYPE_TMP_C
        alarmBean = AlarmBean()
        isOpenCompass = false
        isOpenHighPoint = false
        isOpenLowPoint = false
        aiTraceType = ObserveBean.TYPE_NONE
        isOpenTarget = false
        targetMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        targetType = ObserveBean.TYPE_TARGET_HORIZONTAL
        targetColorType = ObserveBean.TYPE_TARGET_COLOR_GREEN
        reportAuthorName = CommUtils.getAppName()
        reportWatermarkText = CommUtils.getAppName()
        reportHumidity = 500
        fusionType = FusionTypeLPYFusion
        isOpenAmplify = false
    }

    var isSaveSetting: Boolean
        get() = SPUtils.getInstance(SP_NAME).getBoolean("isSaveSetting", true)
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isMeasureTempMode", true) else true
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isMeasureTempMode", value)
            }
        }
    var isOpenAmplify: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isOpenAmplify", false) else false
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isOpenAmplify", value)
        }
    var isVideoMode: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isVideoMode", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isAutoShutter", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isRecordAudio", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isRecordAudio", value)
            }
        }
    var delayCaptureSecond: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("delayCaptureSecond", 0)
            } else {
                0
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("delayCaptureSecond", value)
            }
        }
    var fusionType: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getInt("fusionType", FusionTypeLPYFusion) else FusionTypeLPYFusion
        set(value) {
            SPUtils.getInstance(SP_NAME).put("fusionType", value)
        }
    var isOpenTwoLight: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isOpenTwoLight", false) else false
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("twoLightAlpha", 50) else 50
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("twoLightAlpha", value)
            }
        }
    var pseudoColorMode: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("pseudoColorMode", 3) else 3
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("pseudoColorMode", value)
            }
        }
    var isOpenPseudoBar: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenPseudoBar", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenPseudoBar", value)
            }
        }
    var contrastValue: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("contrastValue", 128)
            } else {
                128
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("contrastValue", value)
            }
        }
    var ddeConfig: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("ddeConfig", 2) else 2
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("ddeConfig", value)
            }
        }
    var alarmBean: AlarmBean
        get() =
            if (isSaveSetting) {
                val json = SPUtils.getInstance(SP_NAME).getString("alarmBean", "")
                if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(
                    json,
                    AlarmBean::class.java
                )
            } else {
                AlarmBean()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("alarmBean", Gson().toJson(value))
            }
        }
    var rotateAngle: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("rotateAngle", DeviceConfig.S_ROTATE_ANGLE)
            } else {
                DeviceConfig.S_ROTATE_ANGLE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("rotateAngle", value)
            }
        }
    var isOpenMirror: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenMirror", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenMirror", value)
            }
        }
    var isOpenCompass: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenCompass", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenCompass", value)
            }
        }
    var tempTextColor: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextColor", 0xffffffff.toInt())
            } else {
                0xffffffff.toInt()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextColor", value)
            }
        }
    var tempTextSize: Int
        get() {
            val context = ContextProvider.getContext()
            val defaultSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14f,
                context.resources.displayMetrics
            ).toInt()
            return if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextSize", defaultSize)
            } else {
                defaultSize
            }
        }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextSize", value)
            }
        }
    var temperatureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("temperatureMode", CameraItemBean.TYPE_TMP_C)
            } else {
                CameraItemBean.TYPE_TMP_C
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("temperatureMode", value)
            }
        }
    var isOpenHighPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenHighPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenLowPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("aiTraceType", ObserveBean.TYPE_NONE)
            } else {
                ObserveBean.TYPE_NONE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTarget", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetMeasureMode",
                    ObserveBean.TYPE_MEASURE_PERSON,
                )
            } else {
                ObserveBean.TYPE_MEASURE_PERSON
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetMeasureMode", value)
            }
        }
    var targetType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetType",
                    ObserveBean.TYPE_TARGET_HORIZONTAL,
                )
            } else {
                ObserveBean.TYPE_TARGET_HORIZONTAL
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetType", value)
            }
        }
    var targetColorType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetColorType",
                    ObserveBean.TYPE_TARGET_COLOR_GREEN,
                )
            } else {
                ObserveBean.TYPE_TARGET_COLOR_GREEN
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetColorType", value)
            }
        }
    var reportAuthorName: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportAuthorName", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportWatermarkText", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("reportHumidity", 500)
            } else {
                500
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportHumidity", value)
            }
        }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\common\SharedManager.kt =====

package com.mpdc4gsr.libunified.app.common

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.CarDetectChildBean
import com.mpdc4gsr.libunified.app.bean.ContinuousBean
import com.mpdc4gsr.libunified.app.bean.WatermarkBean
import com.mpdc4gsr.libunified.app.utils.CarDetectData
import com.mpdc4gsr.libunified.compat.SPUtils

object SharedManager {
    var hasClickWinter: Boolean
        get() = SPUtils.getInstance().getBoolean("hasClickWinter", false)
        set(value) = SPUtils.getInstance().put("hasClickWinter", value)
    var isNeedShowTrendTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isNeedShowTrendTips", true)
        set(value) = SPUtils.getInstance().put("isNeedShowTrendTips", value)
    var hasShownStoragePermissionTip: Boolean
        get() = SPUtils.getInstance().getBoolean("hasShownStoragePermissionTip", false)
        set(value) = SPUtils.getInstance().put("hasShownStoragePermissionTip", value)
    var houseSpaceUnit: Int
        get() = SPUtils.getInstance().getInt("houseSpaceUnit", 0)
        set(value) {
            SPUtils.getInstance().put("houseSpaceUnit", value)
        }
    var costUnit: Int
        get() = SPUtils.getInstance().getInt("costUnit", 0)
        set(value) {
            SPUtils.getInstance().put("costUnit", value)
        }
    var hasTcLine: Boolean
        get() = SPUtils.getInstance().getBoolean("hasConnectTcLine", false)
        set(value) {
            SPUtils.getInstance().put("hasConnectTcLine", value)
        }

    // hasTS004 and hasTC007 properties removed - TS004/TC007 device support discontinued
    // hasTC007 property removed - TC007 device support discontinued
    // irConfigJsonTC007 property removed - TC007 device support discontinued
    var homeGuideStep: Int
        get() {
            val value = SPUtils.getInstance().getInt("homeGuideStep", 2)
            return if (value == 1) 2 else value
        }
        set(value) {
            SPUtils.getInstance().put("homeGuideStep", value)
        }
    var configGuideStep: Int
        get() = SPUtils.getInstance().getInt("configGuideStep", 1)
        set(value) = SPUtils.getInstance().put("configGuideStep", value)
    var isHideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isHideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("isHideEmissivityTips", value)
        }
    var is07HideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("is07HideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("is07HideEmissivityTips", value)
        }
    var is04TISR: Boolean
        get() = SPUtils.getInstance().getBoolean("is04TISR", false)
        set(value) {
            SPUtils.getInstance().put("is04TISR", value)
        }
    var is04AutoSync: Boolean
        get() = SPUtils.getInstance().getBoolean("is04AutoSync", false)
        set(value) {
            SPUtils.getInstance().put("is04AutoSync", value)
        }

    fun getManualAngle(sId: String): Int {
        return SPUtils.getInstance().getInt("manualAngle_$sId", 1000)
    }

    fun setManualAngle(
        sId: String,
        value: Int,
    ) {
        SPUtils.getInstance().put("manualAngle_$sId", value)
    }

    fun getManualData(sId: String): ByteArray {
        val strValue = SPUtils.getInstance().getString("manualData_$sId")
        return if (strValue.isNullOrEmpty()) {
            byteArrayOf(
                0,
                0,
                -128,
                63,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                -128,
                63,
                0,
                0,
                0,
                0,
            )
        } else {
            Base64.decode(strValue.toByteArray(), Base64.DEFAULT)
        }
    }

    fun setManualData(
        sId: String,
        value: ByteArray,
    ) {
        if (value.size == 24) {
            SPUtils.getInstance()
                .put("manualData_$sId", String(Base64.encode(value, Base64.DEFAULT)))
        }
    }

    var isConnectAutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnectAutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnectAutoOpen", value)
        }
    var isConnect07AutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnect07AutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnect07AutoOpen", value)
        }
    var isTipOTG: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipOTG", true)
        set(value) {
            SPUtils.getInstance().put("isTipOTG", value)
        }
    var isTipShutter: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipShutter", true)
        set(value) {
            SPUtils.getInstance().put("isTipShutter", value)
        }
    var isTipHighTemp: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipHighTemp", true)
        set(value) {
            SPUtils.getInstance().put("isTipHighTemp", value)
        }
    var isTipPinP: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipPinP", true)
        set(value) {
            SPUtils.getInstance().put("isTipPinP", value)
        }
    var isTipCoordinate: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipCoordinate", true)
        set(value) {
            SPUtils.getInstance().put("isTipCoordinate", value)
        }
    var isTipAIRecognition: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipAIRecognition", true)
        set(value) {
            SPUtils.getInstance().put("isTipAIRecognition", value)
        }
    var isTipObservePhoto: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipObservePhoto", true)
        set(value) {
            SPUtils.getInstance().put("isTipObservePhoto", value)
        }
    var continuousBean: ContinuousBean
        get() {
            val json = SPUtils.getInstance().getString("continuousBean", "")
            return if (json.isNullOrEmpty()) {
                ContinuousBean()
            } else {
                Gson().fromJson(
                    json,
                    ContinuousBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("continuousBean", Gson().toJson(value))
        }
    var wifiWatermarkBean: WatermarkBean
        get() {
            val json = SPUtils.getInstance().getString("wifiWatermarkBean", "")
            return if (json.isNullOrEmpty()) {
                WatermarkBean()
            } else {
                Gson().fromJson(
                    json,
                    WatermarkBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("watermarkBean", Gson().toJson(value))
        }
    var watermarkBean: WatermarkBean
        get() {
            val json = SPUtils.getInstance().getString("watermarkBean", "")
            return if (json.isNullOrEmpty()) {
                WatermarkBean()
            } else {
                Gson().fromJson(
                    json,
                    WatermarkBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("watermarkBean", Gson().toJson(value))
        }
    var isTipChangeDevice: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipChangeDevice", true)
        set(value) {
            SPUtils.getInstance().put("isTipChangeDevice", value)
        }
    var isChangeDevice: Boolean
        get() = SPUtils.getInstance().getBoolean("isChangeDevice", false)
        set(value) {
            SPUtils.getInstance().put("isChangeDevice", value)
        }
    private const val TOKEN: String = "token"
    private const val USER_ID: String = "user_id"
    private const val USERNAME: String = "username"
    private const val NICKNAME: String = "nickname"
    private const val HEAD_ICON: String = "head_icon"
    private const val BASE_HOST: String = "base_host"
    private const val LANGUAGE = "language"
    private const val HAS_SHOW_CLAUSE = "hasShowClause"
    private const val TEMPERATURE_UNIT = "temperature"
    private const val VERSION_CHECK_DATE = "version_check_date"
    private const val DEVICE_SN = "deviceSn"
    private const val DEVICE_VERSION = "deviceVersion"
    private const val IR_CONFIG = "ir_config"
    private const val SP_CUSTOM_PSEUDO = "sp_custom_pseudo"
    private const val SP_TARGET_POP = "sp_target_pop"
    private const val SP_SETTING_IS_PUSH = "sp_setting_is_push"
    private const val SP_SETTING_IS_RECOMMEND = "sp_setting_is_recommend"
    private const val SP_HOT_MODE = "sp_hot_mode"
    private const val SP_CHANGE_DEVICE = "sp_change_device"
    private const val SP_TC007_CUSTOM_PSEUDO = "sp_tc007_custom_pseudo"
    private const val SP_CAR_DETECT = "sp_car_detect"
    fun setToken(token: String) {
        SPUtils.getInstance().put(TOKEN, token)
    }

    fun getToken(): String {
        return SPUtils.getInstance().getString(TOKEN, "")
    }

    fun setUserId(token: String) {
        SPUtils.getInstance().put(USER_ID, token)
    }

    fun getUserId(): String {
        return SPUtils.getInstance().getString(USER_ID, "0")
    }

    fun setUsername(username: String) {
        SPUtils.getInstance().put(USERNAME, username)
    }

    fun getUsername(): String {
        return SPUtils.getInstance().getString(USERNAME, "")
    }

    fun setNickname(nickname: String) {
        SPUtils.getInstance().put(NICKNAME, nickname)
    }

    fun getNickname(): String {
        return SPUtils.getInstance().getString(NICKNAME, "")
    }

    fun setHeadIcon(headIcon: String) {
        SPUtils.getInstance().put(HEAD_ICON, headIcon)
    }

    fun getHeadIcon(): String {
        return SPUtils.getInstance().getString(HEAD_ICON, "")
    }

    fun setBaseHost(value: String) {
        return SPUtils.getInstance().put(BASE_HOST, value)
    }

    fun getBaseHost(): String {
        return SPUtils.getInstance().getString(BASE_HOST, "")
    }

    fun setLanguage(
        context: Context,
        language: String,
    ) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(LANGUAGE, language).apply()
    }

    fun getLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(LANGUAGE, "")!!
    }

    fun setHasShowClause(hasShowClause: Boolean) {
        return SPUtils.getInstance().put(HAS_SHOW_CLAUSE, hasShowClause)
    }

    fun getHasShowClause(): Boolean {
        return SPUtils.getInstance().getBoolean(HAS_SHOW_CLAUSE, false)
    }

    fun setIRConfig(value: String) {
        return SPUtils.getInstance().put(IR_CONFIG, value)
    }

    fun getIRConfig(): String {
        return SPUtils.getInstance().getString(IR_CONFIG, "")
    }

    fun setTemperature(value: Int) {
        return SPUtils.getInstance().put(TEMPERATURE_UNIT, value)
    }

    fun getTemperature(): Int {
        return SPUtils.getInstance().getInt(TEMPERATURE_UNIT, 1)
    }

    fun setVersionCheckDate(value: Long) {
        return SPUtils.getInstance().put(VERSION_CHECK_DATE, value)
    }

    fun getVersionCheckDate(): Long {
        return SPUtils.getInstance().getLong(VERSION_CHECK_DATE, 0)
    }

    fun setDeviceSn(value: String) {
        return SPUtils.getInstance().put(DEVICE_SN, value)
    }

    fun getDeviceSn(): String {
        return SPUtils.getInstance().getString(DEVICE_SN, "")
    }

    fun setDeviceVersion(value: String) {
        return SPUtils.getInstance().put(DEVICE_VERSION, value)
    }

    fun getDeviceVersion(): String {
        return SPUtils.getInstance().getString(DEVICE_VERSION, "")
    }

    fun saveCustomPseudo(json: String) {
        SPUtils.getInstance().put(SP_CUSTOM_PSEUDO, json)
    }

    fun getCustomPseudo(): String {
        return SPUtils.getInstance().getString(SP_CUSTOM_PSEUDO, "")
    }

    // saveTC007CustomPseudo and getTC0007CustomPseudo methods removed - TC007 device support discontinued
    fun getTargetPop(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_TARGET_POP, false)
    }

    fun saveTargetPop(targetPop: Boolean) {
        SPUtils.getInstance().put(SP_TARGET_POP, targetPop)
    }

    private const val IR_DUAL_DISP = "ir_dual_disp"
    private const val IR_DUAL_DISP_V = "ir_dual_disp_v"
    fun saveSettingIsPush(isPush: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_PUSH, isPush)
    }

    fun getSettingIsPush(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_PUSH, true)
    }

    fun saveSettingIsRecommend(isRecommend: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_RECOMMEND, isRecommend)
    }

    fun getSettingIsRecommend(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_RECOMMEND, true)
    }

    fun getMainPermissionsState(): Boolean {
        return SPUtils.getInstance().getBoolean("main_permissions_state", false)
    }

    fun setMainPermissionsState(value: Boolean) {
        return SPUtils.getInstance().put("main_permissions_state", value)
    }

    fun getImagePermissionsState(): Boolean {
        return SPUtils.getInstance().getBoolean("storage_permissions_state", false)
    }

    fun setImagePermissionsState(value: Boolean) {
        return SPUtils.getInstance().put("storage_permissions_state", value)
    }

    fun getHotMode(): Int {
        return SPUtils.getInstance().getInt(SP_HOT_MODE, 1)
    }

    fun saveHotMode(hotMode: Int) {
        SPUtils.getInstance().put(SP_HOT_MODE, hotMode)
    }

    fun getChangeDevice(): Int {
        return SPUtils.getInstance().getInt(SP_CHANGE_DEVICE, 0)
    }

    fun saveChangeDevice(device: Int) {
        SPUtils.getInstance().put(SP_CHANGE_DEVICE, device)
    }

    fun getCarDetectInfo(): CarDetectChildBean {
        val detectInfo = SPUtils.getInstance().getString(SP_CAR_DETECT, "")
        if (detectInfo.isEmpty()) {
            return CarDetectData.getDetectList()[0].detectChildBeans[0]
        }
        val detectChildBean = Gson().fromJson(detectInfo, CarDetectChildBean::class.java)
        val type = detectChildBean.type
        val pos = detectChildBean.pos
        return CarDetectData.getDetectList()[type].detectChildBeans[pos]
    }

    fun saveCarDetectInfo(bean: CarDetectChildBean) {
        SPUtils.getInstance().put(SP_CAR_DETECT, Gson().toJson(bean))
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\common\UserInfoManager.kt =====

package com.mpdc4gsr.libunified.app.common

import android.text.TextUtils

class UserInfoManager {
    companion object {
        @Volatile
        var manager: UserInfoManager? = null
        fun getInstance(): UserInfoManager {
            if (manager == null) {
                synchronized(UserInfoManager::class) {
                    if (manager == null) {
                        manager = UserInfoManager()
                    }
                }
            }
            return manager!!
        }
    }

    fun isLogin(): Boolean {
        val token = SharedManager.getToken()
        return if (TextUtils.equals("-1", token)) {
            false
        } else {
            !TextUtils.isEmpty(token)
        }
    }

    fun login(
        token: String,
        userId: String,
        phone: String?,
        email: String,
        nickname: String,
        headUrl: String?,
    ) {
        SharedManager.setUserId(userId)
        SharedManager.setUsername(
            if (getMaskPhone(phone)?.isNotEmpty() == true) getMaskPhone(phone) ?: "" else email
        )
        SharedManager.setNickname(nickname)
        SharedManager.setHeadIcon(headUrl ?: "12345")
        SharedManager.setToken(token)
    }

    fun logout() {
        SharedManager.setToken("")
        SharedManager.setUserId("0")
        SharedManager.setNickname("")
        SharedManager.setHeadIcon("")
    }

    private fun getMaskPhone(phone: String?): String? {
        return phone?.replace("(\\d{3})\\d{4}(\\d{4})".toRegex(), "$1****$2")
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\common\WifiSaveSettingUtils.kt =====

package com.mpdc4gsr.libunified.app.common

import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils.FusionTypeIROnly
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils.FusionTypeLPYFusion
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.compat.SPUtils

object WifiSaveSettingUtils {
    private const val SP_NAME = "WifiSaveSettingUtils"
    const val TYPE_PLUG = 0
    const val TYPE_WIFI = 1
    fun reset() {
        isMeasureTempMode = true
        isVideoMode = false
        isAutoShutter = true
        isRecordAudio = false
        isOpenMirror = false
        delayCaptureSecond = 0
        contrastValue = 128
        pseudoColorMode = 3
        rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        isOpenPseudoBar = true
        isOpenTwoLight = false
        twoLightAlpha = 50
        ddeConfig = 2
        tempTextColor = 0xffffffff.toInt()
        temperatureMode = CameraItemBean.TYPE_TMP_C
        alarmBean = AlarmBean()
        isOpenCompass = false
        isOpenHighPoint = false
        isOpenLowPoint = false
        aiTraceType = ObserveBean.TYPE_NONE
        isOpenTarget = false
        targetMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        targetType = ObserveBean.TYPE_TARGET_HORIZONTAL
        targetColorType = ObserveBean.TYPE_TARGET_COLOR_GREEN
        reportAuthorName = CommUtils.getAppName()
        reportWatermarkText = CommUtils.getAppName()
        reportHumidity = 500
        fusionType = FusionTypeLPYFusion
        registrationX = 0
        registrationY = 0
    }

    var registrationX: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("registrationX", 0) else 0
        set(value) {
            SPUtils.getInstance(SP_NAME).put("registrationX", value)
        }
    var registrationY: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("registrationY", 0) else 0
        set(value) {
            SPUtils.getInstance(SP_NAME).put("registrationY", value)
        }
    var fusionType: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getInt("fusionType", FusionTypeIROnly) else FusionTypeIROnly
        set(value) {
            SPUtils.getInstance(SP_NAME).put("fusionType", value)
        }
    var isSaveSetting: Boolean
        get() = SPUtils.getInstance(SP_NAME).getBoolean("isSaveSetting", true)
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isMeasureTempMode", true) else true
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isMeasureTempMode", value)
            }
        }
    var isVideoMode: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isVideoMode", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isAutoShutter", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isRecordAudio", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isRecordAudio", value)
            }
        }
    var isOpenMirror: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenMirror", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenMirror", value)
            }
        }
    var delayCaptureSecond: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("delayCaptureSecond", 0)
            } else {
                0
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("delayCaptureSecond", value)
            }
        }
    var contrastValue: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("contrastValue", 128)
            } else {
                128
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("contrastValue", value)
            }
        }
    var pseudoColorMode: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("pseudoColorMode", 3) else 3
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("pseudoColorMode", value)
            }
        }
    var rotateAngle: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("rotateAngle", DeviceConfig.S_ROTATE_ANGLE)
            } else {
                DeviceConfig.S_ROTATE_ANGLE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("rotateAngle", value)
            }
        }
    var isOpenPseudoBar: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenPseudoBar", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenPseudoBar", value)
            }
        }
    var isOpenTwoLight: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTwoLight", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("twoLightAlpha", 50) else 50
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("twoLightAlpha", value)
            }
        }
    var ddeConfig: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("ddeConfig", 2) else 2
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("ddeConfig", value)
            }
        }
    var tempTextColor: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextColor", 0xffffffff.toInt())
            } else {
                0xffffffff.toInt()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextColor", value)
            }
        }
    var tempTextSize: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextSize", 14)
            } else {
                14
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextSize", value)
            }
        }
    var temperatureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("temperatureMode", CameraItemBean.TYPE_TMP_C)
            } else {
                CameraItemBean.TYPE_TMP_C
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("temperatureMode", value)
            }
        }
    var alarmBean: AlarmBean
        get() =
            if (isSaveSetting) {
                val json = SPUtils.getInstance(SP_NAME).getString("alarmBean", "")
                if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(
                    json,
                    AlarmBean::class.java
                )
            } else {
                AlarmBean()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("alarmBean", Gson().toJson(value))
            }
        }
    var isOpenCompass: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenCompass", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenCompass", value)
            }
        }
    var isOpenHighPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenHighPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenLowPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("aiTraceType", ObserveBean.TYPE_NONE)
            } else {
                ObserveBean.TYPE_NONE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTarget", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetMeasureMode",
                    ObserveBean.TYPE_MEASURE_PERSON,
                )
            } else {
                ObserveBean.TYPE_MEASURE_PERSON
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetMeasureMode", value)
            }
        }
    var targetType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetType",
                    ObserveBean.TYPE_TARGET_HORIZONTAL,
                )
            } else {
                ObserveBean.TYPE_TARGET_HORIZONTAL
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetType", value)
            }
        }
    var targetColorType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetColorType",
                    ObserveBean.TYPE_TARGET_COLOR_GREEN,
                )
            } else {
                ObserveBean.TYPE_TARGET_COLOR_GREEN
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetColorType", value)
            }
        }
    var reportAuthorName: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportAuthorName", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportWatermarkText", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("reportHumidity", 500)
            } else {
                500
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportHumidity", value)
            }
        }
}