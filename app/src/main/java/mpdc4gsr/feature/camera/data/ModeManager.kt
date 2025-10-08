package mpdc4gsr.feature.camera.data


class ModeManager {
    companion object {
    }

    enum class CameraMode {
        RAW_50MP,
        VIDEO_4K,
        PREVIEW_ONLY,
    }

    enum class State {
        IDLE,
        SWITCHING,
        RAW_ACTIVE,
        VIDEO_ACTIVE,
        PREVIEW_ACTIVE,
    }

    private var currentMode = CameraMode.PREVIEW_ONLY
    private var currentState = State.IDLE
    private var deviceCaps: DeviceCaps? = null
    var onModeChanged: ((CameraMode, State) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    fun initialize(caps: DeviceCaps) {
        deviceCaps = caps
    }

    fun requestModeSwitch(targetMode: CameraMode): Boolean {
        if (currentState == State.SWITCHING) {
            return false
        }
        if (currentMode == targetMode) {
            return true
        }
        if (!isModeSupported(targetMode)) {
            val error = "Mode $targetMode not supported on this device"
            onError?.invoke(error)
            return false
        }
        currentState = State.SWITCHING
        val previousMode = currentMode
        currentMode = targetMode
        onModeChanged?.invoke(currentMode, currentState)
        return true
    }

    fun confirmModeSwitch() {
        if (currentState != State.SWITCHING) {
            return
        }
        currentState =
            when (currentMode) {
                CameraMode.RAW_50MP -> State.RAW_ACTIVE
                CameraMode.VIDEO_4K -> State.VIDEO_ACTIVE
                CameraMode.PREVIEW_ONLY -> State.PREVIEW_ACTIVE
            }
        onModeChanged?.invoke(currentMode, currentState)
    }

    fun reportModeSwitchFailed(error: String) {
        if (currentState != State.SWITCHING) {
            return
        }
        currentState = State.IDLE
        onError?.invoke("Mode switch failed: $error")
    }

    fun getCurrentMode(): CameraMode = currentMode
    fun getCurrentState(): State = currentState
    fun isModeSupported(mode: CameraMode): Boolean {
        val caps = deviceCaps ?: return false
        return when (mode) {
            CameraMode.RAW_50MP -> caps.supportsRaw && caps.rawSize.width > 0
            CameraMode.VIDEO_4K -> true
            CameraMode.PREVIEW_ONLY -> true
        }
    }

    fun getAvailableModes(): List<CameraMode> {
        val modes = mutableListOf(CameraMode.PREVIEW_ONLY, CameraMode.VIDEO_4K)
        if (isModeSupported(CameraMode.RAW_50MP)) {
            modes.add(CameraMode.RAW_50MP)
        }
        return modes
    }

    fun isSwitching(): Boolean = currentState == State.SWITCHING
    fun canSwitchMode(): Boolean {
        return currentState != State.SWITCHING
    }

    fun getRecommendedFrameRate(): Int {
        val caps = deviceCaps ?: return 30
        return when (currentMode) {
            CameraMode.RAW_50MP -> 15
            CameraMode.VIDEO_4K -> if (caps.supports4k60) 60 else 30
            CameraMode.PREVIEW_ONLY -> 30
        }
    }
}
