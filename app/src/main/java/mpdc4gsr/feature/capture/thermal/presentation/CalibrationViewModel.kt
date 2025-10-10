package mpdc4gsr.feature.capture.thermal.presentation

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalCoreUseCases
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val thermalUseCases: ThermalCoreUseCases,
    ) : ViewModel() {
        private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        private val _calibrationSettings = MutableStateFlow(CalibrationSettings())
        val calibrationSettings: StateFlow<CalibrationSettings> = _calibrationSettings.asStateFlow()
        private val _calibrationInfo = MutableStateFlow(CalibrationInfo())
        val calibrationInfo: StateFlow<CalibrationInfo> = _calibrationInfo.asStateFlow()
        private val _calibrationProgress = MutableStateFlow(CalibrationProgress())
        val calibrationProgress: StateFlow<CalibrationProgress> = _calibrationProgress.asStateFlow()

        data class CalibrationSettings(
            val autoCalibration: Boolean = true,
        )

        data class CalibrationInfo(
            val thermalLastCalibrated: String = "Never",
            val gsrLastCalibrated: String = "Never",
            val cameraLastAligned: String = "Never",
            val thermalLastDirectory: String? = null,
        )

        data class CalibrationProgress(
            val isRunning: Boolean = false,
            val captured: Int = 0,
            val target: Int = 0,
            val lastSavedPath: String? = null,
            val errorMessage: String? = null,
        )

        companion object {
            private const val KEY_AUTO_CALIBRATION = "calibration_auto"
            private const val KEY_THERMAL_LAST_CALIB = "calibration_thermal_last"
            private const val KEY_THERMAL_LAST_DIR = "calibration_thermal_last_dir"
            private const val KEY_GSR_LAST_CALIB = "calibration_gsr_last"
            private const val KEY_CAMERA_LAST_ALIGN = "calibration_camera_last"
            private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
            private const val THERMAL_CAPTURE_COUNT = 10
            private const val THERMAL_CAPTURE_INTERVAL_MS = 200L
            private const val THERMAL_CALIBRATION_ROOT = "calibration/thermal"
        }

        init {
            loadSettings()
            loadCalibrationInfo()
        }

        private fun loadSettings() {
            _calibrationSettings.value =
                CalibrationSettings(
                    autoCalibration = prefs.getBoolean(KEY_AUTO_CALIBRATION, true),
                )
        }

        private fun loadCalibrationInfo() {
            _calibrationInfo.value =
                CalibrationInfo(
                    thermalLastCalibrated = prefs.getString(KEY_THERMAL_LAST_CALIB, "Never") ?: "Never",
                    gsrLastCalibrated = prefs.getString(KEY_GSR_LAST_CALIB, "Never") ?: "Never",
                    cameraLastAligned = prefs.getString(KEY_CAMERA_LAST_ALIGN, "Never") ?: "Never",
                    thermalLastDirectory = prefs.getString(KEY_THERMAL_LAST_DIR, null),
                )
        }

        fun updateAutoCalibration(enabled: Boolean) {
            viewModelScope.launch {
                prefs.edit().putBoolean(KEY_AUTO_CALIBRATION, enabled).apply()
                _calibrationSettings.value = _calibrationSettings.value.copy(autoCalibration = enabled)
            }
        }

        fun startThermalCalibration() {
            if (_calibrationProgress.value.isRunning) {
                return
            }
            viewModelScope.launch {
                _calibrationProgress.value =
                    CalibrationProgress(
                        isRunning = true,
                        target = THERMAL_CAPTURE_COUNT,
                        captured = 0,
                    )

                if (!thermalUseCases.checkConnection()) {
                    val connectResult = thermalUseCases.connectCamera()
                    if (connectResult.isFailure) {
                        _calibrationProgress.value =
                            CalibrationProgress(
                                isRunning = false,
                                errorMessage = connectResult.exceptionOrNull()?.message
                                    ?: "Unable to connect to thermal camera",
                            )
                        return@launch
                    }
                }

                val captureTimestamp = System.currentTimeMillis()
                val captureDir =
                    withContext(Dispatchers.IO) {
                        File(context.filesDir, "$THERMAL_CALIBRATION_ROOT/$captureTimestamp").apply {
                            mkdirs()
                        }
                    }
                val gson = GsonBuilder().setPrettyPrinting().create()
                val captures = mutableListOf<Map<String, Any>>()

                for (index in 1..THERMAL_CAPTURE_COUNT) {
                    val snapshotResult =
                        withContext(Dispatchers.IO) {
                            thermalUseCases.captureSnapshot()
                        }
                    val snapshot =
                        snapshotResult.getOrElse { error ->
                            _calibrationProgress.value =
                                CalibrationProgress(
                                    isRunning = false,
                                    captured = index - 1,
                                    target = THERMAL_CAPTURE_COUNT,
                                    errorMessage = error.message
                                        ?: "Failed to capture thermal snapshot",
                                )
                            return@launch
                        }

                    val imageFile = File(captureDir, "thermal_${index}.png")
                    val matrixFile = File(captureDir, "thermal_${index}.json")
                    withContext(Dispatchers.IO) {
                        saveSnapshotAssets(snapshot, imageFile, matrixFile, gson)
                    }
                    captures +=
                        mapOf(
                            "index" to index,
                            "timestamp" to snapshot.timestamp,
                            "min_temp" to snapshot.minTemp,
                            "max_temp" to snapshot.maxTemp,
                            "image" to imageFile.name,
                            "matrix" to matrixFile.name,
                        )
                    _calibrationProgress.update {
                        it.copy(
                            isRunning = true,
                            captured = index,
                            target = THERMAL_CAPTURE_COUNT,
                            lastSavedPath = imageFile.absolutePath,
                            errorMessage = null,
                        )
                    }
                    delay(THERMAL_CAPTURE_INTERVAL_MS)
                }

                withContext(Dispatchers.IO) {
                    File(captureDir, "manifest.json").writer().use { writer ->
                        val manifest =
                            mapOf(
                                "timestamp" to captureTimestamp,
                                "capture_count" to THERMAL_CAPTURE_COUNT,
                                "captures" to captures,
                            )
                        gson.toJson(manifest, writer)
                    }
                }

                val formattedTimestamp = getCurrentTimestamp(captureTimestamp)
                prefs
                    .edit()
                    .putString(KEY_THERMAL_LAST_CALIB, formattedTimestamp)
                    .putString(KEY_THERMAL_LAST_DIR, captureDir.absolutePath)
                    .apply()
                loadCalibrationInfo()

                _calibrationProgress.value =
                    CalibrationProgress(
                        isRunning = false,
                        captured = THERMAL_CAPTURE_COUNT,
                        target = THERMAL_CAPTURE_COUNT,
                        lastSavedPath = captureDir.absolutePath,
                    )
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

        private fun saveSnapshotAssets(
            snapshot: ThermalSnapshot,
            imageFile: File,
            matrixFile: File,
            gson: com.google.gson.Gson,
        ) {
            imageFile.outputStream().use { output ->
                snapshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            matrixFile.writer().use { writer ->
                val matrixPayload = snapshot.temperatureMatrix.map { row -> row.toList() }
                gson.toJson(matrixPayload, writer)
            }
        }

        private fun getCurrentTimestamp(baseTimestampMs: Long = System.currentTimeMillis()): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter =
                    java.time.format.DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT, Locale.US)
                val localDateTime =
                    java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(baseTimestampMs),
                        java.time.ZoneId.systemDefault(),
                    )
                formatter.format(localDateTime)
            } else {
                SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date(baseTimestampMs))
            }


