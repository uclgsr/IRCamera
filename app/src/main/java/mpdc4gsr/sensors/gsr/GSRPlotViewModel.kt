package mpdc4gsr.sensors.gsr

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * GSR Plot ViewModel - Modern StateFlow Implementation
 * Manages GSR data plotting and visualization with reactive patterns
 */
class GSRPlotViewModel : BaseViewModel() {

    // StateFlow for plot state
    private val _plotUiState = MutableStateFlow(PlotUiState())
    val plotUiState: StateFlow<PlotUiState> = _plotUiState.asStateFlow()

    // SharedFlow for plot events
    private val _plotEvents = MutableSharedFlow<PlotEvent>()
    val plotEvents: SharedFlow<PlotEvent> = _plotEvents.asSharedFlow()

    // Data StateFlows
    private val _gsrData = MutableStateFlow<List<GSRDataPoint>>(emptyList())
    val gsrData: StateFlow<List<GSRDataPoint>> = _gsrData.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    data class PlotUiState(
        val isLoading: Boolean = false,
        val isPlotVisible: Boolean = true,
        val dataPointCount: Int = 0,
        val timeRange: String = "1 minute",
        val plotType: PlotType = PlotType.LINE_CHART
    )

    sealed class PlotEvent {
        data class ShowError(val message: String) : PlotEvent()
        data class ExportComplete(val fileName: String) : PlotEvent()
        data class ZoomChanged(val level: Float) : PlotEvent()
    }

    enum class PlotType { LINE_CHART, BAR_CHART, SCATTER_PLOT }

    data class GSRDataPoint(
        val timestamp: Long,
        val value: Float,
        val quality: DataQuality = DataQuality.GOOD
    )

    enum class DataQuality { GOOD, FAIR, POOR, INVALID }

    fun loadGSRData(sessionId: String) {
        launchWithErrorHandling {
            _plotUiState.value = _plotUiState.value.copy(isLoading = true)

            // Simulate data loading
            val sampleData = generateSampleGSRData()
            _gsrData.value = sampleData

            _plotUiState.value = _plotUiState.value.copy(
                isLoading = false,
                dataPointCount = sampleData.size
            )
        }
    }

    fun setZoomLevel(level: Float) {
        _zoomLevel.value = level.coerceIn(0.1f, 10.0f)
        viewModelScope.launch {
            _plotEvents.emit(PlotEvent.ZoomChanged(level))
        }
    }

    fun exportPlot(format: String) {
        launchWithErrorHandling {
            _plotUiState.value = _plotUiState.value.copy(isLoading = true)

            // Simulate export process
            kotlinx.coroutines.delay(2000)

            val fileName = "gsr_plot_${System.currentTimeMillis()}.$format"
            _plotEvents.emit(PlotEvent.ExportComplete(fileName))

            _plotUiState.value = _plotUiState.value.copy(isLoading = false)
        }
    }

    private fun generateSampleGSRData(): List<GSRDataPoint> {
        val currentTime = System.currentTimeMillis()
        return (0..100).map { i ->
            GSRDataPoint(
                timestamp = currentTime - (100 - i) * 1000,
                value = (Math.sin(i * 0.1) * 50 + 100 + Math.random() * 20).toFloat(),
                quality = if (i % 10 == 0) DataQuality.FAIR else DataQuality.GOOD
            )
        }
    }
}