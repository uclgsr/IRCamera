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