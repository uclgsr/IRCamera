package mpdc4gsr.camera.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.csl.irCamera.R
import com.kotlinx.coroutines.CoroutineScope
import com.kotlinx.coroutines.Dispatchers
import com.kotlinx.coroutines.SupervisorJob
import com.kotlinx.coroutines.cancel
import com.kotlinx.coroutines.launch
import com.mpdc4gsr.camera.RGBCameraRecorder

class CameraModeSelector
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var modeSegmentedControl: RadioGroup
    private lateinit var rawModeButton: RadioButton
    private lateinit var videoModeButton: RadioButton
    private lateinit var previewModeButton: RadioButton
    private lateinit var modeInfoText: TextView
    private lateinit var performanceWarning: TextView
    private lateinit var switchingProgressBar: ProgressBar

    private var cameraRecorder: RGBCameraRecorder? = null
    private var onModeChangeListener: ((RGBCameraRecorder.CameraMode) -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.camera_mode_selector, this, true)

        modeSegmentedControl = findViewById(R.id.mode_segmented_control)
        rawModeButton = findViewById(R.id.raw_mode_button)
        videoModeButton = findViewById(R.id.video_mode_button)
        previewModeButton = findViewById(R.id.preview_mode_button)
        modeInfoText = findViewById(R.id.mode_info_text)
        performanceWarning = findViewById(R.id.performance_warning)
        switchingProgressBar = findViewById(R.id.switching_progress_bar)

        setupModeButtons()
        setupModeChangeListener()
    }

    private fun setupModeButtons() {
        rawModeButton.apply {
            text = "RAW 50MP"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_camera_raw, 0, 0, 0)
        }

        videoModeButton.apply {
            text = "4K Video"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_videocam, 0, 0, 0)
        }

        previewModeButton.apply {
            text = "Preview"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_preview, 0, 0, 0)
        }

        previewModeButton.isChecked = true
        updateModeInfo(RGBCameraRecorder.CameraMode.PREVIEW_ONLY)
    }

    private fun setupModeChangeListener() {
        modeSegmentedControl.setOnCheckedChangeListener { _, checkedId ->
            if (switchingProgressBar.visibility == VISIBLE) {

                return@setOnCheckedChangeListener
            }

            val selectedMode =
                when (checkedId) {
                    R.id.raw_mode_button -> RGBCameraRecorder.CameraMode.RAW_50MP
                    R.id.video_mode_button -> RGBCameraRecorder.CameraMode.VIDEO_4K
                    R.id.preview_mode_button -> RGBCameraRecorder.CameraMode.PREVIEW_ONLY
                    else -> RGBCameraRecorder.CameraMode.PREVIEW_ONLY
                }

            switchToMode(selectedMode)
        }
    }

    fun setCameraRecorder(recorder: RGBCameraRecorder) {
        this.cameraRecorder = recorder
        updateAvailableModes()
    }

    fun setOnModeChangeListener(listener: (RGBCameraRecorder.CameraMode) -> Unit) {
        this.onModeChangeListener = listener
    }

    private fun switchToMode(mode: RGBCameraRecorder.CameraMode) {
        val recorder = cameraRecorder ?: return

        if (recorder.getCurrentMode() == mode) {
            updateModeInfo(mode)
            return
        }

        if (recorder.isRecording()) {
            showError("Cannot switch modes while recording. Stop recording first.")
            revertToCurrentMode()
            return
        }

        if (!recorder.isModeSupported(mode)) {
            showError("${mode.displayName} is not supported on this device.")
            revertToCurrentMode()
            return
        }

        showSwitchingProgress(true)

        coroutineScope.launch {
            try {
                val success = recorder.switchMode(mode)

                if (success) {
                    updateModeInfo(mode)
                    onModeChangeListener?.invoke(mode)
                    showModeChangeSuccess(mode)
                } else {
                    showError("Failed to switch to ${mode.displayName}")
                    revertToCurrentMode()
                }
            } catch (e: Exception) {
                showError("Mode switch error: ${e.message}")
                revertToCurrentMode()
            } finally {
                showSwitchingProgress(false)
            }
        }
    }

    private fun updateAvailableModes() {
        val recorder = cameraRecorder ?: return
        val availableModes = recorder.getAvailableModes()

        rawModeButton.apply {
            isEnabled = availableModes.contains(RGBCameraRecorder.CameraMode.RAW_50MP)
            alpha = if (isEnabled) 1.0f else 0.5f
        }

        videoModeButton.apply {
            isEnabled = availableModes.contains(RGBCameraRecorder.CameraMode.VIDEO_4K)
            alpha = if (isEnabled) 1.0f else 0.5f
        }

        val rawSupported = recorder.supportsRawCapture()
        val videoSupported = recorder.supportsVideoRecording()
        val highSpeed60 = recorder.supportsHighSpeed60fps()

        val capabilityInfo =
            buildString {
                if (rawSupported) {
                    val maxRes = recorder.getMaxRawResolution()
                    append("✓ RAW: ${maxRes?.width}×${maxRes?.height}\n")
                }
                if (videoSupported) {
                    val videoRes = recorder.getCurrentVideoResolution()
                    append("✓ Video: ${videoRes.width}×${videoRes.height}")
                    if (highSpeed60) append(" @60fps") else append(" @30fps")
                }
            }

        if (capabilityInfo.isNotEmpty()) {
            modeInfoText.text = capabilityInfo
        }
    }

    private fun updateModeInfo(mode: RGBCameraRecorder.CameraMode) {
        when (mode) {
            RGBCameraRecorder.CameraMode.RAW_50MP -> {
                modeInfoText.text = "High-resolution RAW capture\n~15fps streaming, DNG format"
                showPerformanceWarning("RAW mode uses significant memory and storage")
            }

            RGBCameraRecorder.CameraMode.VIDEO_4K -> {
                val fps =
                    if (cameraRecorder?.supportsHighSpeed60fps() == true) "30-60fps" else "30fps"
                modeInfoText.text = "4K video recording\n$fps, H.264 encoding"
                showPerformanceWarning("4K recording may cause device heating")
            }

            RGBCameraRecorder.CameraMode.PREVIEW_ONLY -> {
                modeInfoText.text = "Preview mode only\nLow power consumption"
                hidePerformanceWarning()
            }
        }
    }

    private fun showSwitchingProgress(show: Boolean) {
        switchingProgressBar.visibility = if (show) VISIBLE else GONE
        modeSegmentedControl.isEnabled = !show
    }

    private fun showPerformanceWarning(message: String) {
        performanceWarning.text = message
        performanceWarning.visibility = VISIBLE
        performanceWarning.setTextColor(
            ContextCompat.getColor(
                context,
                android.R.color.holo_orange_dark
            )
        )
    }

    private fun hidePerformanceWarning() {
        performanceWarning.visibility = GONE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showModeChangeSuccess(mode: RGBCameraRecorder.CameraMode) {
        Toast.makeText(context, "Switched to ${mode.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun revertToCurrentMode() {
        val currentMode =
            cameraRecorder?.getCurrentMode() ?: RGBCameraRecorder.CameraMode.PREVIEW_ONLY

        when (currentMode) {
            RGBCameraRecorder.CameraMode.RAW_50MP -> rawModeButton.isChecked = true
            RGBCameraRecorder.CameraMode.VIDEO_4K -> videoModeButton.isChecked = true
            RGBCameraRecorder.CameraMode.PREVIEW_ONLY -> previewModeButton.isChecked = true
        }

        updateModeInfo(currentMode)
    }

    fun getSelectedMode(): RGBCameraRecorder.CameraMode {
        return when (modeSegmentedControl.checkedRadioButtonId) {
            R.id.raw_mode_button -> RGBCameraRecorder.CameraMode.RAW_50MP
            R.id.video_mode_button -> RGBCameraRecorder.CameraMode.VIDEO_4K
            else -> RGBCameraRecorder.CameraMode.PREVIEW_ONLY
        }
    }

    fun setMode(mode: RGBCameraRecorder.CameraMode) {
        when (mode) {
            RGBCameraRecorder.CameraMode.RAW_50MP -> rawModeButton.isChecked = true
            RGBCameraRecorder.CameraMode.VIDEO_4K -> videoModeButton.isChecked = true
            RGBCameraRecorder.CameraMode.PREVIEW_ONLY -> previewModeButton.isChecked = true
        }
        updateModeInfo(mode)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.cancel()
    }
}
