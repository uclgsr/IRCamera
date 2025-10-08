package mpdc4gsr.presentation.screens.thermal

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
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
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    init {
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
            val timestamp = getCurrentTimestamp()
            prefs.edit().putString(KEY_THERMAL_LAST_CALIB, timestamp).apply()
            loadCalibrationInfo()
        }
    }

    fun startGSRCalibration() {
        viewModelScope.launch {
            val timestamp = getCurrentTimestamp()
            prefs.edit().putString(KEY_GSR_LAST_CALIB, timestamp).apply()
            loadCalibrationInfo()
        }
    }

    fun startCameraAlignment() {
        viewModelScope.launch {
            val timestamp = getCurrentTimestamp()
            prefs.edit().putString(KEY_CAMERA_LAST_ALIGN, timestamp).apply()
            loadCalibrationInfo()
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
