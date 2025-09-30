package mpdc4gsr.sensors

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Unified Sensor ViewModel for managing multiple sensor types
 * Provides reactive state management for sensor monitoring dashboard
 */
class SensorViewModel : BaseViewModel() {

    data class SensorState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val sensors: List<SensorInfo> = emptyList(),
        val isMonitoring: Boolean = false
    )

    data class SensorInfo(
        val id: String,
        val name: String,
        val type: String,
        val description: String,
        val status: String,
        val currentValue: String,
        val lastUpdate: String,
        val sampleRate: String
    )

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    init {
        loadSensors()
    }

    fun loadSensors() {
        viewModelScope.launch {
            _sensorState.value = _sensorState.value.copy(isLoading = true)
            
            try {
                // Placeholder sensor data - will be replaced with actual sensor integration
                val sensors = listOf(
                    SensorInfo(
                        id = "gsr_1",
                        name = "GSR Sensor",
                        type = "GSR",
                        description = "Galvanic Skin Response",
                        status = "active",
                        currentValue = "2.45 µS",
                        lastUpdate = "Just now",
                        sampleRate = "128 Hz"
                    ),
                    SensorInfo(
                        id = "thermal_1",
                        name = "Thermal Camera",
                        type = "Thermal",
                        description = "IR Thermal Imaging",
                        status = "active",
                        currentValue = "36.8°C",
                        lastUpdate = "2s ago",
                        sampleRate = "30 FPS"
                    ),
                    SensorInfo(
                        id = "camera_1",
                        name = "RGB Camera",
                        type = "Camera",
                        description = "Visual Recording",
                        status = "inactive",
                        currentValue = "N/A",
                        lastUpdate = "5m ago",
                        sampleRate = "30 FPS"
                    )
                )
                
                _sensorState.value = _sensorState.value.copy(
                    isLoading = false,
                    sensors = sensors
                )
            } catch (e: Exception) {
                _sensorState.value = _sensorState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sensors"
                )
            }
        }
    }

    fun startMonitoring() {
        viewModelScope.launch {
            _sensorState.value = _sensorState.value.copy(isMonitoring = true)
        }
    }

    fun stopMonitoring() {
        viewModelScope.launch {
            _sensorState.value = _sensorState.value.copy(isMonitoring = false)
        }
    }

    fun configureSensor(sensorId: String) {
        // Placeholder for sensor configuration
        viewModelScope.launch {
            // TODO: Implement actual sensor configuration
        }
    }
}
