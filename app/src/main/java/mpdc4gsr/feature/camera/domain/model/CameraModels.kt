package mpdc4gsr.feature.camera.domain.model

enum class FocusMode(val displayName: String) {
    AUTO("Auto"),
    MANUAL("Manual"),
    CONTINUOUS("Continuous");

    fun getNext(): FocusMode {
        return when (this) {
            AUTO -> MANUAL
            MANUAL -> CONTINUOUS
            CONTINUOUS -> AUTO
        }
    }
}

enum class WhiteBalance(val displayName: String) {
    AUTO("Auto"),
    DAYLIGHT("Daylight"),
    CLOUDY("Cloudy"),
    TUNGSTEN("Tungsten");

    fun getNext(): WhiteBalance {
        return when (this) {
            AUTO -> DAYLIGHT
            DAYLIGHT -> CLOUDY
            CLOUDY -> TUNGSTEN
            TUNGSTEN -> AUTO
        }
    }
}

data class CameraState(
    val isRecording: Boolean = false,
    val isPreviewing: Boolean = false,
    val focusMode: FocusMode = FocusMode.AUTO,
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
    val exposureCompensation: Int = 0,
    val zoomLevel: Float = 1.0f
)
