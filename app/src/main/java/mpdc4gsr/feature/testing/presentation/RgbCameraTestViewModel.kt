package mpdc4gsr.feature.testing.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel

class RgbCameraTestViewModel : AppBaseViewModel() {
    companion object {
    }

    data class TestCase(
        val id: String,
        val name: String,
        val description: String,
        val status: TestStatus = TestStatus.PENDING,
        val result: String? = null
    )

    enum class TestStatus {
        PENDING, RUNNING, PASSED, FAILED
    }

    private val _testResults = MutableStateFlow<List<TestCase>>(emptyList())
    val testResults: StateFlow<List<TestCase>> = _testResults.asStateFlow()
    private val _isTestRunning = MutableStateFlow(false)
    val isTestRunning: StateFlow<Boolean> = _isTestRunning.asStateFlow()
    private val _cameraCapabilities = MutableStateFlow<Map<String, Any>>(emptyMap())
    val cameraCapabilities: StateFlow<Map<String, Any>> = _cameraCapabilities.asStateFlow()
    private val _recordingStatus = MutableStateFlow("Ready")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()
    private var cameraRecorder: RgbCameraRecorder? = null
    fun initializeTestCases() {
        _testResults.value = listOf(
            TestCase(
                id = "permissions",
                name = "Camera Permissions",
                description = "Verify camera and storage permissions"
            ),
            TestCase(
                id = "capability",
                name = "Camera Capabilities",
                description = "Test camera features and resolutions"
            ),
            TestCase(
                id = "4k_recording",
                name = "4K Recording Test",
                description = "Test 4K video recording capability"
            ),
            TestCase(
                id = "tap_focus",
                name = "Tap-to-Focus",
                description = "Test tap-to-focus functionality"
            ),
            TestCase(
                id = "manual_controls",
                name = "Manual Controls",
                description = "Test manual exposure and focus controls"
            ),
            TestCase(
                id = "raw_capture",
                name = "RAW Capture",
                description = "Test RAW image capture capability"
            )
        )
    }

    fun initializeCameraRecorder(context: Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
                cameraRecorder = RgbCameraRecorder(context, lifecycleOwner)
                _recordingStatus.value = "Camera Initialized"
                _recordingStatus.value = "Initialization Failed"
            }
        }
    }

    fun updateTestResult(testId: String, status: TestStatus, result: String? = null) {
        _testResults.value = _testResults.value.map { test ->
            if (test.id == testId) {
                test.copy(status = status, result = result)
            } else {
                test
            }
        }
    }

    fun setTestRunning(running: Boolean) {
        _isTestRunning.value = running
    }

    fun updateRecordingStatus(status: String) {
        _recordingStatus.value = status
    }

    fun getCameraRecorder(): RgbCameraRecorder? = cameraRecorder
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
                cameraRecorder?.cleanup()
            }
        }
    }
}
