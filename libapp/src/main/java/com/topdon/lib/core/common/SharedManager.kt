package com.topdon.lib.core.common

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.google.gson.Gson
import com.topdon.lib.core.bean.CarDetectChildBean
import com.topdon.lib.core.bean.ContinuousBean
import com.topdon.lib.core.bean.WatermarkBean
import com.topdon.lib.core.dialog.CarDetectDialog

/**
 * currentclass封装不受“savedsettings开关”影响的configuration项，
 *
 * [SaveSettingUtil] saved受“savedsettings开关”影响的configuration项.
 *
 * create by fylder on 2018/6/14
 **/
object SharedManager {
    /**
     * 是否已click过冬季特辑入口.
     */
    var hasClickWinter: Boolean
        get() = SPUtils.getInstance().getBoolean("hasClickWinter", false)
        set(value) = SPUtils.getInstance().put("hasClickWinter", value)

    /**
     * 是否需要Show/Displaythermal imaging-趋势图tip.
     */
    var isNeedShowTrendTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isNeedShowTrendTips", true)
        set(value) = SPUtils.getInstance().put("isNeedShowTrendTips", value)

    /**
     * 房屋检测 - 建筑area积单位 0-英亩 1-平方米 2-公顷
     */
    var houseSpaceUnit: Int
        get() = SPUtils.getInstance().getInt("houseSpaceUnit", 0)
        set(value) {
            SPUtils.getInstance().put("houseSpaceUnit", value)
        }

    /**
     * 房屋检测 - 检测费用单位，0-美元USD 1-欧元EUR 2-英镑GBP 3-澳元AUD 4-日元JPY 5-加元CAD 6-新西兰NZD 7-human民币RMB 8-港币HKD
     */
    var costUnit: Int
        get() = SPUtils.getInstance().getInt("costUnit", 0)
        set(value) {
            SPUtils.getInstance().put("costUnit", value)
        }

    /**
     * devicelist中是否有 TC 有linedevice，默认 false.
     */
    var hasTcLine: Boolean
        get() = SPUtils.getInstance().getBoolean("hasConnectTcLine", false)
        set(value) {
            SPUtils.getInstance().put("hasConnectTcLine", value)
        }

    /**
     * devicelist中是否有 TS004 device，默认 false.
     */
    var hasTS004: Boolean
        get() = SPUtils.getInstance().getBoolean("hasConnectTS004", false)
        set(value) {
            SPUtils.getInstance().put("hasConnectTS004", value)
        }

    /**
     * devicelist中是否有 TC007 device，默认 false.
     */
    var hasTC007: Boolean
        get() = SPUtils.getInstance().getBoolean("hasConnectTC007", false)
        set(value) {
            SPUtils.getInstance().put("hasConnectTC007", value)
        }

    /**
     * TC007 的temperature修正parameter，JSON 形式.
     */
    var irConfigJsonTC007: String
        get() = SPUtils.getInstance().getString("irConfigJsonTC007")
        set(value) {
            SPUtils.getInstance().put("irConfigJsonTC007", value)
        }

    /**
     * 首页操作指引要Show/Display的步骤 1-第1步 2-第2步 3-第3步 0-不Show/Display
     */
    var homeGuideStep: Int
        get() {
            val value = SPUtils.getInstance().getInt("homeGuideStep", 2)
            return if (value == 1) 2 else value
        }
        set(value) {
            SPUtils.getInstance().put("homeGuideStep", value)
        }

    /**
     * temperature修正操作指引要Show/Display的步骤 1-第1步 2-第2步 0-不Show/Display
     */
    var configGuideStep: Int
        get() = SPUtils.getInstance().getInt("configGuideStep", 1)
        set(value) = SPUtils.getInstance().put("configGuideStep", value)

    /**
     * 是否Show/Display过发射率tip
     */
    var isHideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isHideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("isHideEmissivityTips", value)
        }

    /**
     * tc007是否Show/Display过发射率tip
     */
    var is07HideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("is07HideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("is07HideEmissivityTips", value)
        }

    /**
     * TS004是否开启”超分“
     */
    var is04TISR: Boolean
        get() = SPUtils.getInstance().getBoolean("is04TISR", false)
        set(value) {
            SPUtils.getInstance().put("is04TISR", value)
        }

    /**
     * TS004 是否开启”自动saved到手机“
     */
    var is04AutoSync: Boolean
        get() = SPUtils.getInstance().getBoolean("is04AutoSync", false)
        set(value) {
            SPUtils.getInstance().put("is04AutoSync", value)
        }

    /**
     * dual light校正rotation angle，取值range [0, 2000]，对应 SeekBar 取值.id对应device的sid作为唯一标识区分
     */
    fun getManualAngle(sId: String): Int {
        return SPUtils.getInstance().getInt("manualAngle_$sId", 1000)
    }

    fun setManualAngle(
        sId: String,
        value: Int,
    ) {
        SPUtils.getInstance().put("manualAngle_$sId", value)
    }

    /**
     * dual light校正的实际data，长度必定为 24.
     */
    fun getManualData(sId: String): ByteArray {
        val strValue = SPUtils.getInstance().getString("manualData_$sId")
        return if (strValue.isNullOrEmpty()) {
            // 对应 1,0,0,0,1,0 6个 float，该值为默认值
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

    /**
     * connectiondevice后是否自动开启画area.
     */
    var isConnectAutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnectAutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnectAutoOpen", value)
        }

    /**
     * connection TC007 后是否自动开启画area.
     */
    var isConnect07AutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnect07AutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnect07AutoOpen", value)
        }

    /**
     * devicedisconnect时，是否需要弹出 OTG tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipOTG: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipOTG", true)
        set(value) {
            SPUtils.getInstance().put("isTipOTG", value)
        }

    /**
     * clickthermal imaging-自动快门时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipShutter: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipShutter", true)
        set(value) {
            SPUtils.getInstance().put("isTipShutter", value)
        }

    /**
     * clicktemperature-高温档时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipHighTemp: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipHighTemp", true)
        set(value) {
            SPUtils.getInstance().put("isTipHighTemp", value)
        }

    /**
     * clickthermal imaging-picture-in-picture（也就是dual light）时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipPinP: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipPinP", true)
        set(value) {
            SPUtils.getInstance().put("isTipPinP", value)
        }

    /**
     * clickthermal imaging-observation时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipCoordinate: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipCoordinate", true)
        set(value) {
            SPUtils.getInstance().put("isTipCoordinate", value)
        }

    /**
     * clickthermal imaging-AI追踪时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipAIRecognition: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipAIRecognition", true)
        set(value) {
            SPUtils.getInstance().put("isTipAIRecognition", value)
        }

    /**
     * clickthermal imaging-observationmode-capture踪时，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipObservePhoto: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipObservePhoto", true)
        set(value) {
            SPUtils.getInstance().put("isTipObservePhoto", value)
        }

    /**
     * continuouscapture相关configuration项，不受savedsettings开关影响.
     */
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

    /**
     * wificlass产品
     * watermark相关configuration想，不受savedsettings开关影响.
     */
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

    /**
     * watermark相关configuration想，不受savedsettings开关影响.
     */
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

    /**
     * clickTS004-是否switchdevice，是否需要弹出tip弹框.
     * true-弹出tip弹框 false-Userpoint过不再tip，不需要再弹出
     */
    var isTipChangeDevice: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipChangeDevice", true)
        set(value) {
            SPUtils.getInstance().put("isTipChangeDevice", value)
        }

    /**
     * deviceconnectionsuccess，是否switch.
     * true-switch false-不switch
     */
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
    private const val HAS_SHOW_CLAUSE = "hasShowClause" // 是否Show/Display过条款
    private const val TEMPERATURE_UNIT = "temperature" 
    private const val VERSION_CHECK_DATE = "version_check_date" 
    private const val DEVICE_SN = "deviceSn" 
    private const val DEVICE_VERSION = "deviceVersion" 
    private const val IR_CONFIG = "ir_config" // temperature修正parameter(json)
    private const val SP_CUSTOM_PSEUDO = "sp_custom_pseudo" // 自定义pseudo color条
    private const val SP_TARGET_POP = "sp_target_pop" 
    private const val SP_SETTING_IS_PUSH = "sp_setting_is_push" 
    private const val SP_SETTING_IS_RECOMMEND = "sp_setting_is_recommend"

    /************************TS004************************************/
    private const val SP_HOT_MODE = "sp_hot_mode" // white hot
    private const val SP_CHANGE_DEVICE = "sp_change_device" 
    private const val SP_TC007_CUSTOM_PSEUDO = "sp_tc007_custom_pseudo" // tc007自定义pseudo color条

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

    // 在Application上使用applicationContext会为空，需要传递context
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

    // 1: Celsius    0: Fahrenheit
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

    /**
     * Persists custompseudo to the data source.
     */
    fun saveCustomPseudo(json: String) {
        SPUtils.getInstance().put(SP_CUSTOM_PSEUDO, json)
    }

    fun getCustomPseudo(): String {
        return SPUtils.getInstance().getString(SP_CUSTOM_PSEUDO, "")
    }

    /**
     * Persists tc007custompseudo to the data source.
     */
    fun saveTC007CustomPseudo(json: String) {
        SPUtils.getInstance().put(SP_TC007_CUSTOM_PSEUDO, json)
    }

    fun getTC0007CustomPseudo(): String {
        return SPUtils.getInstance().getString(SP_TC007_CUSTOM_PSEUDO, "")
    }

    /**
     * target页area是否自动弹框
     */
    fun getTargetPop(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_TARGET_POP, false)
    }

    /**
     * Persists targetpop to the data source.
     */
    fun saveTargetPop(targetPop: Boolean) {
        SPUtils.getInstance().put(SP_TARGET_POP, targetPop)
    }

    private const val IR_DUAL_DISP = "ir_dual_disp" // dual lightregistration-水平
    private const val IR_DUAL_DISP_V = "ir_dual_disp_v" // dual lightregistration-垂直

    /**
     * Persists settingispush to the data source.
     */
    fun saveSettingIsPush(isPush: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_PUSH, isPush)
    }

    fun getSettingIsPush(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_PUSH, true)
    }

    /**
     * Persists settingisrecommend to the data source.
     */
    fun saveSettingIsRecommend(isRecommend: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_RECOMMEND, isRecommend)
    }

    fun getSettingIsRecommend(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_RECOMMEND, true)
    }

    /**
     * 国内版是否首页禁止Authorization了
     */
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
    /************************TS004************************************/

    /**
     * TS004主页area-black hot
     */
    fun getHotMode(): Int {
        return SPUtils.getInstance().getInt(SP_HOT_MODE, 1)
    }

    /**
     * Persists hotmode to the data source.
     */
    fun saveHotMode(hotMode: Int) {
        SPUtils.getInstance().put(SP_HOT_MODE, hotMode)
    }

    /**
     * TS004和TS001相互switch device 0:都无connection  1:TS001connection  2:TS004connection
     */
    fun getChangeDevice(): Int {
        return SPUtils.getInstance().getInt(SP_CHANGE_DEVICE, 0)
    }

    /**
     * Persists changedevice to the data source.
     */
    fun saveChangeDevice(device: Int) {
        SPUtils.getInstance().put(SP_CHANGE_DEVICE, device)
    }

    fun getCarDetectInfo(): CarDetectChildBean {
        var detectInfo = SPUtils.getInstance().getString(SP_CAR_DETECT, "")
        if (detectInfo.isEmpty()) {
            return CarDetectDialog.getDetectList()[0].detectChildBeans[0]
        }
        var detectChildBean = GsonUtils.fromJson(detectInfo, CarDetectChildBean::class.java)
        var type = detectChildBean.type
        var pos = detectChildBean.pos
        return CarDetectDialog.getDetectList()[type].detectChildBeans[pos]
    }

    /**
     * Persists cardetectinfo to the data source.
     */
    fun saveCarDetectInfo(bean: CarDetectChildBean) {
        SPUtils.getInstance().put(SP_CAR_DETECT, GsonUtils.toJson(bean))
    }
}
