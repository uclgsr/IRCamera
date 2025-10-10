package mpdc4gsr.feature.capture.thermal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalSettingsRepository(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _thermalSettings = MutableStateFlow(loadSettings())
    val thermalSettings: StateFlow<ThermalSettings> = _thermalSettings.asStateFlow()

    data class ThermalSettings(
        val frameRate: Int = 25,
        val saveRawImages: Boolean = false,
        val palette: String = "Iron",
        val temperatureUnit: String = "Celsius",
        val emissivity: Float = 0.95f,
        val autoScale: Boolean = true,
        val showCrosshair: Boolean = true,
        val temperatureRange: String = "Auto"
    )

    companion object {
        private const val KEY_FRAME_RATE = "thermal_frame_rate"
        private const val KEY_SAVE_RAW_IMAGES = "thermal_save_raw_images"
        private const val KEY_PALETTE = "thermal_palette"
        private const val KEY_TEMP_UNIT = "thermal_temp_unit"
        private const val KEY_EMISSIVITY = "thermal_emissivity"
        private const val KEY_AUTO_SCALE = "thermal_auto_scale"
        private const val KEY_SHOW_CROSSHAIR = "thermal_show_crosshair"
        private const val KEY_TEMP_RANGE = "thermal_temp_range"
        private const val BITRATE_LOW = 800_000
        private const val BITRATE_MEDIUM = 1_500_000
        private const val BITRATE_HIGH = 2_000_000

        @Volatile
        private var instance: ThermalSettingsRepository? = null
        fun getInstance(context: Context): ThermalSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: ThermalSettingsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private fun loadSettings(): ThermalSettings {
        return ThermalSettings(
            frameRate = prefs.getInt(KEY_FRAME_RATE, 25),
            saveRawImages = prefs.getBoolean(KEY_SAVE_RAW_IMAGES, false),
            palette = prefs.getString(KEY_PALETTE, "Iron") ?: "Iron",
            temperatureUnit = prefs.getString(KEY_TEMP_UNIT, "Celsius") ?: "Celsius",
            emissivity = prefs.getFloat(KEY_EMISSIVITY, 0.95f),
            autoScale = prefs.getBoolean(KEY_AUTO_SCALE, true),
            showCrosshair = prefs.getBoolean(KEY_SHOW_CROSSHAIR, true),
            temperatureRange = prefs.getString(KEY_TEMP_RANGE, "Auto") ?: "Auto"
        )
    }

    fun getSettings(): ThermalSettings {
        return _thermalSettings.value
    }

    fun updateFrameRate(frameRate: Int) {
        prefs.edit().putInt(KEY_FRAME_RATE, frameRate).apply()
        _thermalSettings.value = _thermalSettings.value.copy(frameRate = frameRate)
    }

    fun updateSaveRawImages(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_RAW_IMAGES, enabled).apply()
        _thermalSettings.value = _thermalSettings.value.copy(saveRawImages = enabled)
    }

    fun updatePalette(palette: String) {
        prefs.edit().putString(KEY_PALETTE, palette).apply()
        _thermalSettings.value = _thermalSettings.value.copy(palette = palette)
    }

    fun updateTemperatureUnit(unit: String) {
        prefs.edit().putString(KEY_TEMP_UNIT, unit).apply()
        _thermalSettings.value = _thermalSettings.value.copy(temperatureUnit = unit)
    }

    fun updateEmissivity(emissivity: Float) {
        prefs.edit().putFloat(KEY_EMISSIVITY, emissivity).apply()
        _thermalSettings.value = _thermalSettings.value.copy(emissivity = emissivity)
    }

    fun updateAutoScale(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCALE, enabled).apply()
        _thermalSettings.value = _thermalSettings.value.copy(autoScale = enabled)
    }

    fun updateShowCrosshair(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CROSSHAIR, enabled).apply()
        _thermalSettings.value = _thermalSettings.value.copy(showCrosshair = enabled)
    }

    fun updateTemperatureRange(range: String) {
        prefs.edit().putString(KEY_TEMP_RANGE, range).apply()
        _thermalSettings.value = _thermalSettings.value.copy(temperatureRange = range)
    }

    fun getThermalVideoConfig(): ThermalVideoConfig {
        val settings = getSettings()
        val frameRate = settings.frameRate.coerceIn(10, 30)
        val bitrate = when {
            frameRate <= 15 -> BITRATE_LOW
            frameRate <= 25 -> BITRATE_MEDIUM
            else -> BITRATE_HIGH
        }
        return ThermalVideoConfig(
            frameRate = frameRate,
            bitrate = bitrate
        )
    }

    data class ThermalVideoConfig(
        val frameRate: Int,
        val bitrate: Int
    )
}

