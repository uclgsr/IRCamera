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
import kotlinx.coroutines.Dispatchers
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
    private val _uiState = MutableStateFlow(ThermalUIState())
    val uiState: StateFlow<ThermalUIState> = _uiState.asStateFlow()

    // Thermal surface dimensions
    var rawWidth: Int = 0
        private set
    var rawHeight: Int = 0
        private set

    init {
        setupThermalDataProcessing()
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
                _uiState.value = newUiState
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
            // Convert pixel data to temperature values
            // TODO: Implement proper pixel to temperature conversion
            (pixel and 0xFF).toFloat() / 255f * 100f // Simple placeholder conversion
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
        val validCount = temperatureData.count { it > -40f && it < 150f } // Reasonable temperature range
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

    fun updateSurfaceDimensions(width: Int, height: Int) {
        rawWidth = width
        rawHeight = height
    }

    fun calculateViewPosition(index: Int, viewWidth: Int, viewHeight: Int, parentWidth: Int, parentHeight: Int): Pair<Float, Float> {
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
        data class TemperatureAlert(val temperature: Float, val type: AlertType) : ThermalProcessingAction()
    }

    enum class AlertType { HOT_SPOT, COLD_SPOT, TEMPERATURE_THRESHOLD }
}