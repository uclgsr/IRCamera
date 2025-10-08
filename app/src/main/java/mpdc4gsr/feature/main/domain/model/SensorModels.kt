package mpdc4gsr.feature.main.domain.model

enum class SessionState { 
    IDLE, STARTING, RECORDING, PAUSED, STOPPING, ERROR 
}

enum class SensorStatus { 
    DISCONNECTED, CONNECTING, CONNECTED, STREAMING, ERROR, SIMULATION 
}

data class SensorState(
    val status: SensorStatus = SensorStatus.DISCONNECTED,
    val message: String? = null,
    val isRecording: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class GsrDataState(
    val currentValue: Float = 0f,
    val batteryLevel: Int = 0,
    val recentReadings: List<Float> = emptyList(),
    val averageValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 0f
)
