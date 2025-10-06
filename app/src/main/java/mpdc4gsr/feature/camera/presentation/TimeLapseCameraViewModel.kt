package mpdc4gsr.feature.camera.presentation
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

enum class TimeLapseMode(val displayName: String) {
    MANUAL("Manual Interval"),
    AUTO("Auto Optimize"),
    PRESET_FAST("Fast (1s)"),
    PRESET_MEDIUM("Medium (5s)"),
    PRESET_SLOW("Slow (10s)")
}

class TimeLapseCameraViewModel(
    @Suppress("UNUSED_PARAMETER") context: Context
) : AppBaseViewModel() {
    companion object {
        private const val DEFAULT_PLAYBACK_FPS = 30
    }
    data class TimeLapseState(
        val isRecording: Boolean = false,
        val capturedFrames: Int = 0,
        val intervalSeconds: Int = 5,
        val mode: TimeLapseMode = TimeLapseMode.PRESET_MEDIUM,
        val totalDuration: Int = 0,
        val estimatedVideoLength: Int = 0,
        val lastCaptureTime: Long = 0L,
        val error: String? = null,
        val resolution: String = "1920×1080",
        val quality: Int = 90
    )
    private val _timeLapseState = MutableStateFlow(TimeLapseState())
    val timeLapseState: StateFlow<TimeLapseState> = _timeLapseState.asStateFlow()
    fun startTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = true,
                capturedFrames = 0,
                totalDuration = 0,
                error = null
            )
        }
    }
    fun stopTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = false
            )
        }
    }
    fun updateInterval(seconds: Int) {
        _timeLapseState.value = _timeLapseState.value.copy(
            intervalSeconds = seconds.coerceIn(1, 60)
        )
    }
    fun setMode(mode: TimeLapseMode) {
        val interval = when (mode) {
            TimeLapseMode.PRESET_FAST -> 1
            TimeLapseMode.PRESET_MEDIUM -> 5
            TimeLapseMode.PRESET_SLOW -> 10
            else -> _timeLapseState.value.intervalSeconds
        }
        _timeLapseState.value = _timeLapseState.value.copy(
            mode = mode,
            intervalSeconds = interval
        )
    }
    fun captureFrame() {
        launchWithErrorHandling {
            val current = _timeLapseState.value
            _timeLapseState.value = current.copy(
                capturedFrames = current.capturedFrames + 1,
                lastCaptureTime = System.currentTimeMillis(),
                estimatedVideoLength = (current.capturedFrames + 1) / DEFAULT_PLAYBACK_FPS
            )
        }
    }
}

class TimeLapseCameraViewModelFactory(
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLapseCameraViewModel::class.java)) {
            return TimeLapseCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
