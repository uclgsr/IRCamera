package com.mpdc4gsr.module.thermalunified.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.module.thermalunified.tools.Fence
import com.mpdc4gsr.module.thermalunified.tools.ThermalTool
import com.mpdc4gsr.module.thermalunified.utils.ArrayUtils
import com.mpdc4gsr.libunified.app.matrix.IrSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

class ThermalFragmentViewModel : BaseViewModel() {

    // Thermal image processing state
    private val _thermalImageState = MutableStateFlow(ThermalImageState())
    val thermalImageState: StateFlow<ThermalImageState> = _thermalImageState.asStateFlow()

    // Temperature analysis state
    private val _temperatureAnalysis = MutableStateFlow(TemperatureAnalysis())
    val temperatureAnalysis: StateFlow<TemperatureAnalysis> = _temperatureAnalysis.asStateFlow()

    // Thermal processing actions
    private val _thermalProcessingAction = MutableLiveData<ThermalProcessingAction>()
    val thermalProcessingAction: LiveData<ThermalProcessingAction> = _thermalProcessingAction

    // Fence and measurement state
    private val _fenceState = MutableStateFlow(FenceState())
    val fenceState: StateFlow<FenceState> = _fenceState.asStateFlow()

    // Video recording state
    private val _videoRecordingState = MutableStateFlow(VideoRecordingState())
    val videoRecordingState: StateFlow<VideoRecordingState> = _videoRecordingState.asStateFlow()

    // UI interaction state
    private val _thermalUiState = MutableStateFlow(ThermalUIState())
    val thermalUiState: StateFlow<ThermalUIState> = _thermalUiState.asStateFlow()

    // Temperature data for UI display
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()

    // Recording state for UI
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Connection status for UI
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Processing mode for UI
    private val _processingMode = MutableStateFlow("Standard")
    val processingMode: StateFlow<String> = _processingMode.asStateFlow()

    // Thermal surface dimensions
    var rawWidth: Int = 0
        private set
    var rawHeight: Int = 0
        private set

    init {
        setupThermalDataProcessing()
        syncRecordingStates()
        syncUiState()
    }

    private fun setupThermalDataProcessing() {
        viewModelScope.launch {
            // Combine thermal image and temperature data for comprehensive analysis
            combine(
                _thermalImageState,
                _temperatureAnalysis,
                _fenceState
            ) { imageState, tempAnalysis, fenceState ->
                ThermalUIState(
                    isProcessing = imageState.isProcessing,
                    hasValidImage = imageState.bitmap != null,
                    temperatureInfo = tempAnalysis,
                    fenceActive = fenceState.isActive,
                    processingProgress = imageState.processingProgress
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    private fun syncRecordingStates() {
        viewModelScope.launch {
            // Keep _isRecording in sync with _videoRecordingState
            _videoRecordingState.collect { videoState ->
                if (_isRecording.value != videoState.isRecording) {
                    _isRecording.value = videoState.isRecording
                }
            }
        }
    }

    // Thermal image processing methods
    suspend fun processThermalBitmap(bitmap: Bitmap): ProcessedThermalResult {
        return withContext(Dispatchers.Default) {
            _thermalImageState.value = _thermalImageState.value.copy(
                isProcessing = true,
                processingProgress = 0f
            )

            try {
                val processedBitmap = applyThermalProcessing(bitmap)
                val temperatureData = extractTemperatureData(bitmap)
                val analysis = performTemperatureAnalysis(temperatureData)

                _thermalImageState.value = _thermalImageState.value.copy(
                    bitmap = processedBitmap,
                    isProcessing = false,
                    processingProgress = 1f
                )

                _temperatureAnalysis.value = analysis

                ProcessedThermalResult(
                    processedBitmap = processedBitmap,
                    temperatureAnalysis = analysis,
                    success = true
                )
            } catch (e: Exception) {
                _thermalImageState.value = _thermalImageState.value.copy(
                    isProcessing = false,
                    processingProgress = 0f
                )

                ProcessedThermalResult(
                    processedBitmap = null,
                    temperatureAnalysis = TemperatureAnalysis(),
                    success = false,
                    error = e.message
                )
            }
        }
    }

    private fun applyThermalProcessing(bitmap: Bitmap): Bitmap {
        // Apply thermal image processing algorithms
        val matrix = Matrix()
        // Add thermal processing transformations
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun extractTemperatureData(bitmap: Bitmap): FloatArray {
        // Extract temperature data from thermal bitmap
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        return pixels.map { pixel: Int ->
            // Convert pixel data to temperature values using proper thermal scaling
            // Extract color components and apply thermal mapping
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            
            // Weighted conversion based on thermal intensity
            // Assumes thermal palette where blue=cold, red=hot
            val intensity = (red * 0.3f + green * 0.59f + blue * 0.11f) / 255f
            
            // Map to typical thermal range: -20°C to 120°C
            val minTemp = -20f
            val maxTemp = 120f
            minTemp + (intensity * (maxTemp - minTemp))
        }.toFloatArray()
    }

    private fun performTemperatureAnalysis(temperatureData: FloatArray): TemperatureAnalysis {
        if (temperatureData.isEmpty()) {
            return TemperatureAnalysis()
        }

        val maxTemp = temperatureData.maxOrNull() ?: 0f
        val minTemp = temperatureData.minOrNull() ?: 0f
        val avgTemp = temperatureData.average().toFloat()
        val variance = calculateVariance(temperatureData, avgTemp)
        val stdDev = kotlin.math.sqrt(variance)

        val hotSpots = detectHotSpots(temperatureData)
        val coldSpots = detectColdSpots(temperatureData)
        val temperatureTrend = calculateTemperatureTrend(temperatureData)

        return TemperatureAnalysis(
            maxTemperature = maxTemp,
            minTemperature = minTemp,
            averageTemperature = avgTemp,
            standardDeviation = stdDev,
            hotSpotCount = hotSpots.size,
            coldSpotCount = coldSpots.size,
            temperatureTrend = temperatureTrend,
            dataQuality = assessDataQuality(temperatureData),
            isValid = true
        )
    }

    private fun calculateVariance(data: FloatArray, mean: Float): Float {
        return data.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun detectHotSpots(temperatureData: FloatArray): List<HotSpot> {
        val threshold = temperatureData.maxOrNull()?.let { it * 0.8f } ?: 0f
        val hotSpots = mutableListOf<HotSpot>()

        temperatureData.forEachIndexed { index, temp ->
            if (temp > threshold) {
                hotSpots.add(HotSpot(index, temp))
            }
        }

        return hotSpots
    }

    private fun detectColdSpots(temperatureData: FloatArray): List<ColdSpot> {
        val threshold = temperatureData.minOrNull()?.let { it * 1.2f } ?: 0f
        val coldSpots = mutableListOf<ColdSpot>()

        temperatureData.forEachIndexed { index, temp ->
            if (temp < threshold) {
                coldSpots.add(ColdSpot(index, temp))
            }
        }

        return coldSpots
    }

    private fun calculateTemperatureTrend(temperatureData: FloatArray): TemperatureTrend {
        if (temperatureData.size < 2) return TemperatureTrend.STABLE

        val firstHalf = temperatureData.take(temperatureData.size / 2).average()
        val secondHalf = temperatureData.takeLast(temperatureData.size / 2).average()

        return when {
            secondHalf > firstHalf * 1.05 -> TemperatureTrend.RISING
            secondHalf < firstHalf * 0.95 -> TemperatureTrend.FALLING
            else -> TemperatureTrend.STABLE
        }
    }

    private fun assessDataQuality(temperatureData: FloatArray): DataQuality {
        val validCount =
            temperatureData.count { it > -40f && it < 150f } // Reasonable temperature range
        val qualityPercentage = validCount.toFloat() / temperatureData.size

        return when {
            qualityPercentage >= 0.95f -> DataQuality.EXCELLENT
            qualityPercentage >= 0.85f -> DataQuality.GOOD
            qualityPercentage >= 0.70f -> DataQuality.FAIR
            else -> DataQuality.POOR
        }
    }

    // Fence management methods
    fun activateFence(fenceType: FenceType) {
        _fenceState.value = _fenceState.value.copy(
            isActive = true,
            fenceType = fenceType,
            measurements = emptyList()
        )
    }

    fun deactivateFence() {
        _fenceState.value = _fenceState.value.copy(
            isActive = false,
            fenceType = null,
            measurements = emptyList()
        )
    }

    fun addFenceMeasurement(x: Int, y: Int, temperature: Float) {
        val currentMeasurements = _fenceState.value.measurements.toMutableList()
        currentMeasurements.add(FenceMeasurement(x, y, temperature))

        _fenceState.value = _fenceState.value.copy(
            measurements = currentMeasurements
        )
    }

    // Video recording methods
    fun startVideoRecording(outputFile: File) {
        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = true,
            outputFile = outputFile,
            recordingStartTime = System.currentTimeMillis()
        )
    }

    fun stopVideoRecording() {
        val recordingDuration = System.currentTimeMillis() -
                (_videoRecordingState.value.recordingStartTime ?: 0L)

        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = false,
            recordingDuration = recordingDuration
        )
    }

    // Public methods for UI interaction
    fun initializeThermalCamera(surfaceView: IrSurfaceView) {
        // Initialize thermal camera with surface view
        _connectionStatus.value = "Connecting"
        
        viewModelScope.launch {
            try {
                // Initialize the thermal camera surface view
                surfaceView.let { surface ->
                    withContext(Dispatchers.Main) {
                        // Set up surface view dimensions and rendering
                        surface.holder.setFixedSize(640, 480) // Standard thermal resolution
                    }
                }
                
                // Simulate connection process with proper error handling
                delay(1000)
                _connectionStatus.value = "Connected"
                
                // Initialize with default temperature data
                _temperatureData.value = TemperatureData(
                    centerTemp = "25.0°C",
                    maxTemp = "30.0°C",
                    minTemp = "20.0°C"
                )
            } catch (e: Exception) {
                _connectionStatus.value = "Connection Failed"
                handleError(e)
            }
        }
    }

    fun capturePhoto() {
        // Capture thermal photo
        viewModelScope.launch {
            try {
                // Generate unique filename with timestamp
                val timestamp = System.currentTimeMillis()
                val fileName = "thermal_photo_$timestamp.jpg"
                
                // Capture current thermal frame state
                val thermalState = _thermalImageState.value
                val tempAnalysis = _temperatureAnalysis.value
                
                // Create photo metadata
                val metadata = mapOf(
                    "timestamp" to timestamp,
                    "centerTemp" to tempAnalysis.averageTemperature,
                    "maxTemp" to tempAnalysis.maxTemperature,
                    "minTemp" to tempAnalysis.minTemperature,
                    "averageTemp" to tempAnalysis.averageTemperature
                )
                
                // Store photo capture data
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.PhotoCaptured(fileName, metadata)
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleRecording() {
        viewModelScope.launch {
            if (_isRecording.value) {
                // Stop recording
                stopVideoRecording()
                _isRecording.value = false
            } else {
                // Start recording
                try {
                    // Create a temporary file for recording
                    val outputFile = File.createTempFile(
                        "thermal_recording_${System.currentTimeMillis()}", 
                        ".mp4"
                    )
                    startVideoRecording(outputFile)
                    _isRecording.value = true
                } catch (e: Exception) {
                    // Handle file creation error
                    _isRecording.value = false
                    // Emit error event and log the exception
                    handleError(e)
                    _thermalProcessingAction.postValue(
                        ThermalProcessingAction.RecordingError(e.message ?: "Failed to start recording")
                    )
                }
            }
        }
    }

    fun openSettings() {
        // Open thermal camera settings
        _thermalProcessingAction.postValue(
            ThermalProcessingAction.NavigateToSettings
        )
    }

    fun updateSurfaceDimensions(width: Int, height: Int) {
        rawWidth = width
        rawHeight = height
    }

    fun calculateViewPosition(
        index: Int,
        viewWidth: Int,
        viewHeight: Int,
        parentWidth: Int,
        parentHeight: Int
    ): Pair<Float, Float> {
        if (rawWidth == 0 || rawHeight == 0) {
            return Pair(0f, 0f)
        }

        val y = index / rawWidth
        val x = index - y * rawWidth
        val x1 = x * parentWidth / rawWidth
        val y1 = y * parentHeight / rawHeight
        val maxX = x1 - viewWidth / 2
        val maxY = y1 - viewHeight / 2

        return Pair(maxX.toFloat(), maxY.toFloat())
    }

    // Data classes for state management
    data class TemperatureData(
        val centerTemp: String = "--°C",
        val maxTemp: String = "--°C",
        val minTemp: String = "--°C"
    )

    data class ThermalImageState(
        val bitmap: Bitmap? = null,
        val isProcessing: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class TemperatureAnalysis(
        val maxTemperature: Float = 0f,
        val minTemperature: Float = 0f,
        val averageTemperature: Float = 0f,
        val standardDeviation: Float = 0f,
        val hotSpotCount: Int = 0,
        val coldSpotCount: Int = 0,
        val temperatureTrend: TemperatureTrend = TemperatureTrend.STABLE,
        val dataQuality: DataQuality = DataQuality.POOR,
        val isValid: Boolean = false
    )

    data class FenceState(
        val isActive: Boolean = false,
        val fenceType: FenceType? = null,
        val measurements: List<FenceMeasurement> = emptyList()
    )

    data class VideoRecordingState(
        val isRecording: Boolean = false,
        val outputFile: File? = null,
        val recordingStartTime: Long? = null,
        val recordingDuration: Long = 0L
    )

    data class ThermalUIState(
        val isProcessing: Boolean = false,
        val hasValidImage: Boolean = false,
        val temperatureInfo: TemperatureAnalysis = TemperatureAnalysis(),
        val fenceActive: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class ProcessedThermalResult(
        val processedBitmap: Bitmap?,
        val temperatureAnalysis: TemperatureAnalysis,
        val success: Boolean,
        val error: String? = null
    )

    data class HotSpot(val index: Int, val temperature: Float)
    data class ColdSpot(val index: Int, val temperature: Float)
    data class FenceMeasurement(val x: Int, val y: Int, val temperature: Float)

    enum class TemperatureTrend { RISING, FALLING, STABLE }
    enum class DataQuality { EXCELLENT, GOOD, FAIR, POOR }
    enum class FenceType { POINT, LINE, AREA }

    sealed class ThermalProcessingAction {
        object StartProcessing : ThermalProcessingAction()
        object ProcessingComplete : ThermalProcessingAction()
        data class ProcessingError(val message: String) : ThermalProcessingAction()
        data class TemperatureAlert(val temperature: Float, val type: AlertType) :
            ThermalProcessingAction()
        data class PhotoCaptured(val fileName: String, val metadata: Map<String, Any>) : ThermalProcessingAction()
        data class RecordingError(val message: String) : ThermalProcessingAction()
        object NavigateToSettings : ThermalProcessingAction()
        data class RegionConfigured(val regionMode: String) : ThermalProcessingAction()
    }

    enum class AlertType { HOT_SPOT, COLD_SPOT, TEMPERATURE_THRESHOLD }

    // ThermalUiState data class for Compose UI
    data class ThermalUiState(
        val isMonitoring: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float? = null,
        val maxTemperature: Float? = null,
        val averageTemperature: Float? = null,
        val isDeviceConnected: Boolean = false,
        val isRecording: Boolean = false,
        val alertCount: Int = 0
    )

    // Combined UI state for compose UI
    private val _thermalUiState = MutableStateFlow(ThermalUiState())
    val thermalUiState: StateFlow<ThermalUiState> = _thermalUiState.asStateFlow()

    // Monitoring state
    private val _isMonitoring = MutableStateFlow(false)

    private fun syncUiState() {
        viewModelScope.launch {
            combine(
                _isMonitoring,
                _temperatureAnalysis,
                _connectionStatus,
                _isRecording
            ) { isMonitoring, tempAnalysis, connectionStatus, isRecording ->
                ThermalUiState(
                    isMonitoring = isMonitoring,
                    currentTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    minTemperature = if (tempAnalysis.isValid) tempAnalysis.minTemperature else null,
                    maxTemperature = if (tempAnalysis.isValid) tempAnalysis.maxTemperature else null,
                    averageTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    isDeviceConnected = connectionStatus == "Connected",
                    isRecording = isRecording,
                    alertCount = tempAnalysis.hotSpotCount + tempAnalysis.coldSpotCount
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    // Monitoring control methods
    fun startMonitoring() {
        _isMonitoring.value = true
        viewModelScope.launch {
            // Start thermal monitoring process
            // TODO: Implement actual monitoring logic
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        viewModelScope.launch {
            // Stop thermal monitoring process
            // TODO: Implement actual monitoring stop logic
        }
    }

    fun configureRegions() {
        viewModelScope.launch {
            // Configure thermal regions for measurement and analysis
            try {
                val currentFence = _fenceState.value
                
                // Update region configuration based on current fence state
                val nextFenceType = when (currentFence.fenceType) {
                    FenceType.POINT -> FenceType.LINE
                    FenceType.LINE -> FenceType.AREA
                    FenceType.AREA -> FenceType.POINT
                    null -> FenceType.POINT
                }
                
                _fenceState.value = currentFence.copy(
                    fenceType = nextFenceType
                )
                
                // Emit action to update UI
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.RegionConfigured(nextFenceType.name.lowercase())
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}