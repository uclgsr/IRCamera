package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Camera settings view for manual camera controls
 * MVP implementation with basic callback support
 */
class CameraSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Exposure controls
    var onExposureModeToggle: ((Boolean) -> Unit)? = null
    var onExposureCompensationChanged: ((Float) -> Unit)? = null
    var onAeLockToggle: ((Boolean) -> Unit)? = null

    // Focus controls
    var onFocusModeToggle: ((Boolean) -> Unit)? = null
    var onFocusDistanceChanged: ((Float) -> Unit)? = null
    var onAfLockToggle: ((Boolean) -> Unit)? = null

    // Basic camera controls
    var onCameraToggle: (() -> Unit)? = null
    var onRecordingToggle: ((Boolean) -> Unit)? = null
    var onFlashToggle: ((Boolean) -> Unit)? = null
    var onStage3ProcessingToggle: ((Boolean) -> Unit)? = null

    init {
        // Initialize view - minimal implementation for MVP
        orientation = VERTICAL
        // Add any basic UI elements here if needed
    }
}