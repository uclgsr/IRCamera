package mpdc4gsr.feature.connectivity.data

interface SimpleRecordingInterface {
    val isRecording: Boolean
    fun startRecording(): Boolean
    fun stopRecording(): Boolean
    fun getStatus(): Map<String, Any>
}

class MockRecordingController : SimpleRecordingInterface {
    private var _isRecording = false
    private var sessionStartTime: Long? = null
    override val isRecording: Boolean
        get() = _isRecording

    override fun startRecording(): Boolean {
        if (_isRecording) {
            return false // Already recording
        }
        _isRecording = true
        sessionStartTime = System.currentTimeMillis()
        return true
    }

    override fun stopRecording(): Boolean {
        if (!_isRecording) {
            return false // Not recording
        }
        _isRecording = false
        sessionStartTime = null
        return true
    }

    override fun getStatus(): Map<String, Any> {
        val status = mutableMapOf<String, Any>()
        status["recording"] = _isRecording
        status["timestamp"] = System.currentTimeMillis()
        if (_isRecording && sessionStartTime != null) {
            status["session_duration"] = System.currentTimeMillis() - sessionStartTime!!
            status["sensors"] = listOf("RGB", "Thermal", "GSR")
        }
        return status
    }
}
