package mpdc4gsr.core.ui.utils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow


@Composable
fun <T> LiveData<T>.observeAsComposeState(initial: T): State<T> {
    return this.observeAsState(initial)
}

@Composable
fun <T> StateFlow<T>.collectAsComposeState(): State<T> {
    return this.collectAsState()
}

data class ComposeThermalData(
    val centerTemp: Float,
    val maxTemp: Float,
    val minTemp: Float,
    val maxTempLocation: Pair<Int, Int>? = null,
    val minTempLocation: Pair<Int, Int>? = null,
    val isRecording: Boolean = false,
    val connectionState: String = "Disconnected"
) {
    companion object {
        
        fun fromExistingData(
            center: Float,
            max: Float,
            min: Float,
            recording: Boolean = false
        ): ComposeThermalData {
            return ComposeThermalData(
                centerTemp = center,
                maxTemp = max,
                minTemp = min,
                isRecording = recording
            )
        }
    }
}

data class ComposeGSRData(
    val currentValue: Float,
    val batteryLevel: Int,
    val connectionState: String,
    val sampleRate: String = "51.2Hz",
    val recentReadings: List<Float> = emptyList()
) {
    companion object {
        fun fromShimmerData(
            value: Float,
            battery: Int,
            connected: Boolean
        ): ComposeGSRData {
            return ComposeGSRData(
                currentValue = value,
                batteryLevel = battery,
                connectionState = if (connected) "Connected" else "Disconnected"
            )
        }
    }
}

data class ComposeConnectionStates(
    val thermalCamera: mpdc4gsr.core.ui.ConnectionState,
    val gsrSensor: mpdc4gsr.core.ui.ConnectionState,
    val bleConnection: mpdc4gsr.core.ui.ConnectionState
)

object ViewModelStateBridge {

    @Composable
    fun createThermalDataState(
        centerTempLiveData: LiveData<Float>,
        maxTempLiveData: LiveData<Float>,
        minTempLiveData: LiveData<Float>,
        isRecordingLiveData: LiveData<Boolean>
    ): State<ComposeThermalData?> {
        val centerTemp = centerTempLiveData.observeAsState(0f)
        val maxTemp = maxTempLiveData.observeAsState(0f)
        val minTemp = minTempLiveData.observeAsState(0f)
        val isRecording = isRecordingLiveData.observeAsState(false)
        return androidx.compose.runtime.derivedStateOf {
            ComposeThermalData.fromExistingData(
                center = centerTemp.value,
                max = maxTemp.value,
                min = minTemp.value,
                recording = isRecording.value
            )
        }
    }

    @Composable
    fun createGSRDataState(
        currentValueFlow: StateFlow<Float>,
        batteryLevelFlow: StateFlow<Int>,
        connectionStateFlow: StateFlow<Boolean>
    ): State<ComposeGSRData> {
        val currentValue = currentValueFlow.collectAsState()
        val batteryLevel = batteryLevelFlow.collectAsState()
        val isConnected = connectionStateFlow.collectAsState()
        return androidx.compose.runtime.derivedStateOf {
            ComposeGSRData.fromShimmerData(
                value = currentValue.value,
                battery = batteryLevel.value,
                connected = isConnected.value
            )
        }
    }
}

object ComposeEventBridge {

    @Composable
    fun handleThermalEvents(onEvent: (String) -> Unit) {
        // Integration with existing EventBus patterns
        // Can be extended to convert EventBus events to Compose actions
    }

    @Composable
    fun handleConnectionEvents(onConnectionChange: (Boolean) -> Unit) {
        // Integration with existing connection event handling
        // Preserves existing EventBus integration while enabling Compose UI updates
    }
}