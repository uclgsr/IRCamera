package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.BaseViewModel

/**
 * Calibration ViewModel - MVVM Integration
 * Manages system calibration settings and calibration procedures
 */
class CalibrationViewModel : BaseViewModel() {

    private lateinit var prefs: SharedPreferences

    private val _calibrationSettings = MutableStateFlow(CalibrationSettings())
    val calibrationSettings: StateFlow<CalibrationSettings> = _calibrationSettings.asStateFlow()

    private val _calibrationInfo = MutableStateFlow(CalibrationInfo())
    val calibrationInfo: StateFlow<CalibrationInfo> = _calibrationInfo.asStateFlow()

    data class CalibrationSettings(
        val autoCalibration: Boolean = true
    )

    data class CalibrationInfo(
        val thermalLastCalibrated: String = "Never",
        val gsrLastCalibrated: String = "Never",
        val cameraLastAligned: String = "Never"
    )

    companion object {
        private const val KEY_AUTO_CALIBRATION = "calibration_auto"
        private const val KEY_THERMAL_LAST_CALIB = "calibration_thermal_last"
        private const val KEY_GSR_LAST_CALIB = "calibration_gsr_last"
        private const val KEY_CAMERA_LAST_ALIGN = "calibration_camera_last"
    }

    fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        loadSettings()
        loadCalibrationInfo()
    }

    private fun loadSettings() {
        _calibrationSettings.value = CalibrationSettings(
            autoCalibration = prefs.getBoolean(KEY_AUTO_CALIBRATION, true)
        )
    }

    private fun loadCalibrationInfo() {
        _calibrationInfo.value = CalibrationInfo(
            thermalLastCalibrated = prefs.getString(KEY_THERMAL_LAST_CALIB, "Never") ?: "Never",
            gsrLastCalibrated = prefs.getString(KEY_GSR_LAST_CALIB, "Never") ?: "Never",
            cameraLastAligned = prefs.getString(KEY_CAMERA_LAST_ALIGN, "Never") ?: "Never"
        )
    }

    fun updateAutoCalibration(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_CALIBRATION, enabled).apply()
            _calibrationSettings.value = _calibrationSettings.value.copy(autoCalibration = enabled)
        }
    }

    fun startThermalCalibration() {
        viewModelScope.launch {
            // TODO: Integrate with thermal camera SDK for actual calibration
            val timestamp = System.currentTimeMillis().toString()
            prefs.edit().putString(KEY_THERMAL_LAST_CALIB, "Just now").apply()
            loadCalibrationInfo()
        }
    }

    fun startGSRCalibration() {
        viewModelScope.launch {
            // TODO: Integrate with Shimmer SDK for actual GSR calibration
            val timestamp = System.currentTimeMillis().toString()
            prefs.edit().putString(KEY_GSR_LAST_CALIB, "Just now").apply()
            loadCalibrationInfo()
        }
    }

    fun startCameraAlignment() {
        viewModelScope.launch {
            // TODO: Integrate with camera alignment procedure
            val timestamp = System.currentTimeMillis().toString()
            prefs.edit().putString(KEY_CAMERA_LAST_ALIGN, "Just now").apply()
            loadCalibrationInfo()
        }
    }
}
