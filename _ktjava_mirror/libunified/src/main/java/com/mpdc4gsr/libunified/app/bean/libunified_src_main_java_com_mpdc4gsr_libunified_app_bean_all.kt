// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean' subtree
// Files: 17; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\AlarmBean.kt =====

package com.mpdc4gsr.libunified.app.bean

import java.nio.ByteBuffer

data class AlarmBean(
    var isHighOpen: Boolean = false,
    var isLowOpen: Boolean = false,
    var highTemp: Float = Float.MAX_VALUE,
    var lowTemp: Float = Float.MIN_VALUE,
    var isMarkOpen: Boolean = true,
    var highColor: Int = 0xffff0000.toInt(),
    var lowColor: Int = 0xff0000ff.toInt(),
    var markType: Int = TYPE_ALARM_MARK_STROKE,
    var isRingtoneOpen: Boolean = false,
    var ringtoneType: Int = 0,
) {
    companion object {
        const val TYPE_ALARM_MARK_STROKE = 1
        const val TYPE_ALARM_MARK_MATRIX = 2
        fun loadFromArray(data: ByteArray): AlarmBean {
            val buffer = ByteBuffer.wrap(data)
            val isHighOpen = buffer.get() == 1.toByte()
            val isLowOpen = buffer.get() == 1.toByte()
            val highTemp = buffer.float
            val lowTemp = buffer.float
            val isMarkOpen = buffer.get() == 1.toByte()
            val highColor = buffer.int
            val lowColor = buffer.int
            val markType = buffer.int
            val isRingtoneOpen = buffer.get() == 1.toByte()
            val ringtoneType = buffer.int
            return AlarmBean(
                isHighOpen = isHighOpen,
                isLowOpen = isLowOpen,
                highTemp = highTemp,
                lowTemp = lowTemp,
                isMarkOpen = isMarkOpen,
                highColor = if (highColor == 0) 0xffff0000.toInt() else highColor,
                lowColor = if (lowColor == 0) 0xff0fa752.toInt() else lowColor,
                markType = if (markType == 0) 1 else markType,
                isRingtoneOpen = isRingtoneOpen,
                ringtoneType = ringtoneType,
            )
        }
    }

    fun toByteArray(): ByteArray =
        ByteBuffer.allocate(28)
            .put(if (isHighOpen) 1 else 0)
            .put(if (isLowOpen) 1 else 0)
            .putFloat(highTemp)
            .putFloat(lowTemp)
            .put(if (isMarkOpen) 1 else 0)
            .putInt(highColor)
            .putInt(lowColor)
            .putInt(markType)
            .put(if (isRingtoneOpen) 1 else 0)
            .putInt(ringtoneType)
            .array()

    fun isOpen(): Boolean = isHighOpen || isLowOpen
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\NoBodyEntity.kt =====

package com.mpdc4gsr.libunified.app.bean.base

class NoBodyEntity


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\Resp.kt =====

package com.mpdc4gsr.libunified.app.bean.base

import android.text.TextUtils

class Resp<T> {
    var code: String = ""
    var msg: String = ""
    var data: T? = null
    fun isSuccess(): Boolean {
        return TextUtils.equals(code, "0")
    }

    override fun toString(): String {
        return "Resp(code='$code', msg='$msg', data=$data)"
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\CameraIRConfig.kt =====

package com.mpdc4gsr.libunified.app.bean

data class ContinuousBean(
    var isOpen: Boolean = false,
    var continuaTime: Long = 1000,
    var count: Int = 3
)

class ObserveBean {
    companion object {
        //
        const val TYPE_NONE = -1 //
        const val TYPE_DYN_R = 0 //
        const val TYPE_TMP_H_S = 1 //
        const val TYPE_TMP_L_S = 2 //
        const val TYPE_MEASURE_PERSON = 10 //
        const val TYPE_MEASURE_SHEEP = 11 //
        const val TYPE_MEASURE_DOG = 12 //
        const val TYPE_MEASURE_BIRD = 13 //
        const val TYPE_TARGET_HORIZONTAL = 15 //
        const val TYPE_TARGET_VERTICAL = 16 //
        const val TYPE_TARGET_CIRCLE = 17 //
        const val TYPE_TARGET_COLOR_GREEN = 20 //
        const val TYPE_TARGET_COLOR_RED = 21 //
        const val TYPE_TARGET_COLOR_BLUE = 22 //
        const val TYPE_TARGET_COLOR_BLACK = 23 //
        const val TYPE_TARGET_COLOR_WHITE = 24 //
        const val TYPE_TARGET_AREA = 30
        const val TYPE_TARGET_LINE = 31
        const val TYPE_TARGET_SPOT = 32
    }

    var observeType: Int = TYPE_NONE
    var observeX: Float = 0f
    var observeY: Float = 0f
    var observeWidth: Float = 0f
    var observeHeight: Float = 0f
    var maxTemp: Float = 0f
    var minTemp: Float = 0f
    var avgTemp: Float = 0f
    var isSelect: Boolean = false
    var colorType: Int = TYPE_TARGET_COLOR_WHITE
}

data class CameraItemBean(
    var name: String = "",
    var type: Int = 0,
    var time: Int = DELAY_TIME_0,
    var isSel: Boolean = false,
) {
    fun changeDelayType() {
        if (type == TYPE_DELAY) {
            when (time) {
                DELAY_TIME_0 -> {
                    time = DELAY_TIME_3
                }

                DELAY_TIME_3 -> {
                    time = DELAY_TIME_6
                }

                DELAY_TIME_6 -> {
                    time = DELAY_TIME_0
                }
            }
        }
    }

    companion object {
        const val TYPE_DELAY = 0
        const val TYPE_ZDKM = 1
        const val TYPE_SDKM = 2
        const val TYPE_AUDIO = 3
        const val TYPE_SETTING = 4
        const val DELAY_TIME_0 = 0//3
        const val DELAY_TIME_3 = 3//3
        const val DELAY_TIME_6 = 6//6

        //
        const val TYPE_TMP_ZD = -1 //
        const val TYPE_TMP_C = 1 // 
        const val TYPE_TMP_H = 0 //
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\CustomPseudoBean.kt =====

package com.mpdc4gsr.libunified.app.bean

import android.os.Parcelable
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.common.SharedManager
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomPseudoBean(
    var selectIndex: Int = 0,
    var colors: IntArray? = null,
    var zAltitudes: IntArray? = null,
    var places: FloatArray? = null,
    var isUseCustomPseudo: Boolean = false,
    var maxTemp: Float = 50f,
    var minTemp: Float = 0f,
    var isColorCustom: Boolean = true,
    var customMinColor: Int = 0xff0000FF.toInt(),
    var customMiddleColor: Int = 0xFFFF0000.toInt(),
    var customMaxColor: Int = 0xFFFFFF00.toInt(),
    var customRecommendIndex: Int = 0,
    var isUseGray: Boolean = true,
) : Parcelable {
    companion object {
        fun loadFromShared(isTC007: Boolean = false): CustomPseudoBean {
            // TC007 functionality removed - always use default
            val json = if (!isTC007) SharedManager.getCustomPseudo() else ""
            return if (json.isNotEmpty()) {
                try {
                    Gson().fromJson(json, CustomPseudoBean::class.java)
                } catch (e: Exception) {
                    CustomPseudoBean()
                }
            } else {
                CustomPseudoBean()
            }
        }

        fun toCustomPseudoBean(byteArray: ByteArray): CustomPseudoBean {
            // Stub implementation - return default bean
            return CustomPseudoBean()
        }
    }

    fun saveToShared(isTC007: Boolean = false) {
        // TC007 functionality removed - only save for non-TC007 devices
        if (!isTC007) {
            SharedManager.saveCustomPseudo(Gson().toJson(this))
        }
        // TC007 save functionality disabled
    }

    fun getColorList(isTC007: Boolean = false): IntArray? {
        // Return null to indicate no custom colors (use defaults)
        return if (isUseCustomPseudo) null else null
    }

    fun getPlaceList(): FloatArray? {
        // Return null to indicate no custom places (use defaults)
        return if (isUseCustomPseudo) null else null
    }

    fun getCustomColors(): IntArray {
        return colors ?: intArrayOf(customMinColor, customMiddleColor, customMaxColor)
    }

    fun getCustomZAltitudes(): IntArray {
        return zAltitudes ?: intArrayOf(0, 50, 100)
    }

    fun getCustomPlaces(): FloatArray {
        return places ?: floatArrayOf(0f, 0.5f, 1f)
    }

    fun toByteArray(): ByteArray {
        // Return minimal byte array
        return ByteArray(92)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CustomPseudoBean
        if (selectIndex != other.selectIndex) return false
        if (colors != null) {
            if (other.colors == null) return false
            if (!colors.contentEquals(other.colors)) return false
        } else if (other.colors != null) return false
        if (zAltitudes != null) {
            if (other.zAltitudes == null) return false
            if (!zAltitudes.contentEquals(other.zAltitudes)) return false
        } else if (other.zAltitudes != null) return false
        if (places != null) {
            if (other.places == null) return false
            if (!places.contentEquals(other.places)) return false
        } else if (other.places != null) return false
        if (isUseCustomPseudo != other.isUseCustomPseudo) return false
        if (maxTemp != other.maxTemp) return false
        if (minTemp != other.minTemp) return false
        if (isColorCustom != other.isColorCustom) return false
        if (customMinColor != other.customMinColor) return false
        if (customMiddleColor != other.customMiddleColor) return false
        if (customMaxColor != other.customMaxColor) return false
        if (customRecommendIndex != other.customRecommendIndex) return false
        if (isUseGray != other.isUseGray) return false
        return true
    }

    override fun hashCode(): Int {
        var result = selectIndex
        result = 31 * result + (colors?.contentHashCode() ?: 0)
        result = 31 * result + (zAltitudes?.contentHashCode() ?: 0)
        result = 31 * result + (places?.contentHashCode() ?: 0)
        result = 31 * result + isUseCustomPseudo.hashCode()
        result = 31 * result + maxTemp.hashCode()
        result = 31 * result + minTemp.hashCode()
        result = 31 * result + isColorCustom.hashCode()
        result = 31 * result + customMinColor
        result = 31 * result + customMiddleColor
        result = 31 * result + customMaxColor
        result = 31 * result + customRecommendIndex
        result = 31 * result + isUseGray.hashCode()
        return result
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\GalleryDelEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class GalleryDelEvent


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\PDFEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class PDFEvent


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\ReportCreateEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

public data class ReportCreateEvent(val name: String = "")


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\TS004ResetEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested"
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\VersionUpData.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class VersionUpData(
    val versionNo: String,
    val isForcedUpgrade: Boolean,
    val description: String,
    val downPageUrl: String,
    val sizeStr: String,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\GalleryBean.kt =====

package com.mpdc4gsr.libunified.app.bean

import android.os.Parcel
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.repository.TS004FileBean
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.libunified.app.tools.VideoTools
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.*

@Parcelize
open class GalleryBean(
    val id: Int,
    val path: String,
    val thumb: String,
    val name: String,
    val duration: Long,
    val timeMillis: Long,
    var hasDownload: Boolean,
) : Parcelable {
    constructor(file: File) : this(
        id = 0,
        path = file.absolutePath,
        thumb = file.absolutePath,
        name = file.name,
        duration = VideoTools.getLocalVideoDuration(file.absolutePath),
        timeMillis = TimeTools.updateDateTime(file),
        hasDownload = true,
    )

    constructor(isVideo: Boolean, fileBean: TS004FileBean) : this(
        id = fileBean.id,
        path = "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        thumb = if (isVideo) "http://192.168.40.1:8080/DCIM/${fileBean.thumb}" else "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        name = fileBean.name,
        duration = fileBean.duration * 1000L,
        timeMillis = fileBean.time * 1000 - TimeZone.getDefault().getOffset(fileBean.time * 1000),
        hasDownload = File(FileConfig.ts004GalleryDir, fileBean.name).exists(),
    )
}

class GalleryTitle(timeMillis: Long) : GalleryBean(
    id = 0,
    path = "",
    thumb = "",
    name = "",
    duration = 0L,
    timeMillis = timeMillis,
    hasDownload = true,
) {
    companion object CREATOR : Parcelable.Creator<GalleryTitle> {
        override fun createFromParcel(parcel: Parcel): GalleryTitle {
            return GalleryTitle(parcel.readLong())
        }

        override fun newArray(size: Int): Array<GalleryTitle?> {
            return arrayOfNulls(size)
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\json\CheckVersionJson.kt =====

package com.mpdc4gsr.libunified.app.bean.json

data class CheckVersionJson(
    val downloadPackageUrl: String,
    val downloadPageUrl: String,
    val forcedUpgradeFlag: String?,
    val googleVerCode: Int,
    val softConfigOtherTypeVOList: List<SoftConfigOtherTypeVO>,
    val versionCode: Int,
    val versionNo: String?,
    val notUnZipSize: Double,
)

data class SoftConfigOtherTypeVO(
    val descType: Int,
    val descTypeName: String,
    val fileUrl: Any,
    val textDescription: String,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\json\StatementJson.kt =====

package com.mpdc4gsr.libunified.app.bean.json

data class StatementJson(
    val content: Any,
    val createTime: String,
    val createUserName: Any,
    val current: Int,
    val htmlContent: String,
    val id: Any,
    val language: Any,
    val languageId: Any,
    val languageIds: Any,
    val revieweContent: Any,
    val size: Int,
    val softCode: Any,
    val status: Any,
    val statusList: Any,
    val type: Any,
    val versionNum: Any,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\response\ResponseUserInfo.kt =====

package com.mpdc4gsr.libunified.app.bean.response

data class ResponseUserInfo(
    val topdonId: String,
    val userName: String,
    val email: String,
    val url: String,
    val pwd: String,
    val remark: String,
    val createTime: Long,
    val updateTime: Long,
    val profilePicture: String,
    val lastVisitTime: String,
    val phone: String?,
    val avatar: String?,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\TargetColorBean.kt =====

package com.mpdc4gsr.libunified.app.bean

data class TargetColorBean(
    val res: Int,
    val name: String,
    val code: Int,
    var isSelect: Boolean = false,
    var n_res: Int = 0,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\tools\ThermalBean.kt =====

package com.mpdc4gsr.libunified.app.bean.tools

class ThermalBean {
    var maxTemp = 0f
    var minTemp = 0f
    var centerTemp = 0f
    var type = 1
    var createTime = 0L
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\WatermarkBean.kt =====

package com.mpdc4gsr.libunified.app.bean

import com.mpdc4gsr.libunified.app.utils.ByteUtils
import com.mpdc4gsr.libunified.app.utils.CommUtils

data class WatermarkBean(
    var isOpen: Boolean = false,
    var title: String = CommUtils.getAppName(),
    var address: String = "",
    var isAddTime: Boolean = false,
) {
    companion object {
        fun loadFromArray(data: ByteArray): WatermarkBean {
            val titleLen = ByteUtils.bigBytesToInt(data[1], data[2], data[3], data[4])
            val titleBytes = ByteArray(titleLen)
            System.arraycopy(data, 5, titleBytes, 0, titleBytes.size)
            val addressLen = ByteUtils.bigBytesToInt(data[125], data[126], data[127], data[128])
            val addressBytes = ByteArray(addressLen)
            System.arraycopy(data, 129, addressBytes, 0, addressBytes.size)
            return WatermarkBean(
                isOpen = data[0].toInt() == 1,
                title = if (titleLen == 0) "" else String(titleBytes),
                address = if (addressLen == 0) "" else String(addressBytes),
                isAddTime = data[449].toInt() == 1,
            )
        }
    }

    fun toByteArray(): ByteArray {
        val result = ByteArray(450)
        val titleByteArray = title.toByteArray()
        val addressByteArray = address.toByteArray()
        result[0] = if (isOpen) 1 else 0
        result[1] = (titleByteArray.size ushr 24).toByte()
        result[2] = (titleByteArray.size ushr 16).toByte()
        result[3] = (titleByteArray.size ushr 8).toByte()
        result[4] = titleByteArray.size.toByte()
        System.arraycopy(titleByteArray, 0, result, 5, titleByteArray.size)
        result[125] = (addressByteArray.size ushr 24).toByte()
        result[126] = (addressByteArray.size ushr 16).toByte()
        result[127] = (addressByteArray.size ushr 8).toByte()
        result[128] = addressByteArray.size.toByte()
        System.arraycopy(addressByteArray, 0, result, 129, addressByteArray.size)
        result[449] = if (isAddTime) 1 else 0
        return result
    }
}


