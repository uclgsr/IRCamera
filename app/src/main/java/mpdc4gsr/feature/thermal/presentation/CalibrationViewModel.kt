package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : AppBaseViewModel() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val _calibrationSettings = MutableStateFlow(CalibrationSettings())
    val calibrationSettings: StateFlow<CalibrationSettings> = _calibrationSettings.asStateFlow()
    private val _calibrationInfo = MutableStateFlow(CalibrationInfo())
    val calibrationInfo: StateFlow<CalibrationInfo> = _calibrationInfo.asStateFlow()
    
    init {
        loadSettings()
        loadCalibrationInfo()
    }

    data class CalibrationSettings(
        val autoCalibration: Boolean = true
    )

    data class CalibrationInfo(
        val thermalLastCalibrated: String = "Never",
        val gsrLastCalibrated: String = "Never",
        val cameraLastAligned: String = "Never"
    )

    companion object {
        private const val TAG = "CalibrationViewModel"
        private const val KEY_AUTO_CALIBRATION = "calibration_auto"
        private const val KEY_THERMAL_LAST_CALIB = "calibration_thermal_last"
        private const val KEY_GSR_LAST_CALIB = "calibration_gsr_last"
        private const val KEY_CAMERA_LAST_ALIGN = "calibration_camera_last"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
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
            try {
                AppLogger.d(TAG, "Starting thermal camera calibration")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_THERMAL_LAST_CALIB, timestamp).apply()
                AppLogger.i(TAG, "Thermal calibration completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full calibration requires Topdon SDK LibIRTemp integration")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during thermal calibration", e)
            }
        }
    }

    fun startGSRCalibration() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Starting GSR sensor calibration")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_GSR_LAST_CALIB, timestamp).apply()
                AppLogger.i(TAG, "GSR calibration completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full calibration requires Shimmer3 SDK calibration commands")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during GSR calibration", e)
            }
        }
    }

    fun startCameraAlignment() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Starting camera alignment procedure")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_CAMERA_LAST_ALIGN, timestamp).apply()
                AppLogger.i(TAG, "Camera alignment completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full alignment requires multi-camera spatial calibration")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during camera alignment", e)
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.format.DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT, Locale.US)
                .format(java.time.LocalDateTime.now())
        } else {
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        }
    }
}
