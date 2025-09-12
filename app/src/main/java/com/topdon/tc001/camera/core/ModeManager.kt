package com.topdon.tc001.camera.core

import android.util.Log

/**
 * Deterministic state machine for camera mode management
 * Two exclusive modes: RAW mode (50MP DNG stream) OR Video mode (4K60 if exposed, else 4K30)
 */
class ModeManager {
    
    companion object {
        private const val TAG = "ModeManager"
    }
    
    enum class CameraMode {
        RAW_50MP,
        VIDEO_4K,
        PREVIEW_ONLY
    }
    
    enum class State {
        IDLE,
        SWITCHING,
        RAW_ACTIVE,
        VIDEO_ACTIVE,
        PREVIEW_ACTIVE
    }
    
    private var currentMode = CameraMode.PREVIEW_ONLY
    private var currentState = State.IDLE
    private var deviceCaps: DeviceCaps? = null
    
    // State change callbacks
    var onModeChanged: ((CameraMode, State) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Initialize with device capabilities
     */
    fun initialize(caps: DeviceCaps) {
        deviceCaps = caps
        Log.i(TAG, "Mode manager initialized with device capabilities")
    }
    
    /**
     * Request mode switch with validation
     */
    fun requestModeSwitch(targetMode: CameraMode): Boolean {
        if (currentState == State.SWITCHING) {
            Log.w(TAG, "Mode switch already in progress")
            return false
        }
        
        if (currentMode == targetMode) {
            Log.i(TAG, "Already in target mode: $targetMode")
            return true
        }
        
        // Validate mode is supported
        if (!isModeSupported(targetMode)) {
            val error = "Mode $targetMode not supported on this device"
            Log.e(TAG, error)
            onError?.invoke(error)
            return false
        }
        
        // Start switching
        currentState = State.SWITCHING
        val previousMode = currentMode
        currentMode = targetMode
        
        Log.i(TAG, "Mode switch: $previousMode -> $targetMode")
        onModeChanged?.invoke(currentMode, currentState)
        
        return true
    }
    
    /**
     * Confirm mode switch completed successfully
     */
    fun confirmModeSwitch() {
        if (currentState != State.SWITCHING) {
            Log.w(TAG, "No mode switch in progress to confirm")
            return
        }
        
        currentState = when (currentMode) {
            CameraMode.RAW_50MP -> State.RAW_ACTIVE
            CameraMode.VIDEO_4K -> State.VIDEO_ACTIVE
            CameraMode.PREVIEW_ONLY -> State.PREVIEW_ACTIVE
        }
        
        Log.i(TAG, "Mode switch confirmed: $currentMode active")
        onModeChanged?.invoke(currentMode, currentState)
    }
    
    /**
     * Report mode switch failed
     */
    fun reportModeSwitchFailed(error: String) {
        if (currentState != State.SWITCHING) {
            Log.w(TAG, "No mode switch in progress to fail")
            return
        }
        
        Log.e(TAG, "Mode switch failed: $error")
        
        // Revert to previous state
        currentState = State.IDLE
        onError?.invoke("Mode switch failed: $error")
    }
    
    /**
     * Get current mode
     */
    fun getCurrentMode(): CameraMode = currentMode
    
    /**
     * Get current state
     */
    fun getCurrentState(): State = currentState
    
    /**
     * Check if mode is supported by device
     */
    fun isModeSupported(mode: CameraMode): Boolean {
        val caps = deviceCaps ?: return false
        
        return when (mode) {
            CameraMode.RAW_50MP -> caps.supportsRaw && caps.rawSize.width > 0
            CameraMode.VIDEO_4K -> true // Basic video always supported
            CameraMode.PREVIEW_ONLY -> true // Always supported
        }
    }
    
    /**
     * Get available modes for this device
     */
    fun getAvailableModes(): List<CameraMode> {
        val modes = mutableListOf(CameraMode.PREVIEW_ONLY, CameraMode.VIDEO_4K)
        
        if (isModeSupported(CameraMode.RAW_50MP)) {
            modes.add(CameraMode.RAW_50MP)
        }
        
        return modes
    }
    
    /**
     * Check if currently switching modes
     */
    fun isSwitching(): Boolean = currentState == State.SWITCHING
    
    /**
     * Check if a mode switch is allowed from current state
     */
    fun canSwitchMode(): Boolean {
        return currentState != State.SWITCHING
    }
    
    /**
     * Get recommended frame rate for current mode
     */
    fun getRecommendedFrameRate(): Int {
        val caps = deviceCaps ?: return 30
        
        return when (currentMode) {
            CameraMode.RAW_50MP -> 15 // Conservative for Samsung RAW
            CameraMode.VIDEO_4K -> if (caps.supports4k60) 60 else 30
            CameraMode.PREVIEW_ONLY -> 30
        }
    }
}