package com.topdon.tc001.camera.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.topdon.tc001.camera.RGBCameraRecorder
import com.csl.irCamera.R

/**
 * Samsung-style camera settings UI component
 * Provides resolution selection, camera switching, and recording options
 */
class CameraSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // UI Components
    private lateinit var cameraToggleButton: ImageButton
    private lateinit var resolutionSpinner: Spinner
    private lateinit var flashToggleButton: ImageButton
    private lateinit var recordButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var stabilizationToggle: Switch
    private lateinit var audioToggle: Switch
    private lateinit var frameRateSpinner: Spinner
    private lateinit var qualitySeekBar: SeekBar
    private lateinit var settingsPanel: LinearLayout
    private lateinit var statusText: TextView

    // Data
    private var currentSettings = RGBCameraRecorder.RecordingSettings()
    private var isSettingsPanelVisible = false
    private var isRecording = false

    // Callbacks
    var onCameraToggle: (() -> Unit)? = null
    var onRecordingToggle: ((Boolean) -> Unit)? = null
    var onSettingsChanged: ((RGBCameraRecorder.RecordingSettings) -> Unit)? = null
    var onFlashToggle: ((Boolean) -> Unit)? = null

    init {
        initView()
        setupListeners()
        updateUI()
    }

    private fun initView() {
        // Create the layout programmatically since we don't have XML resources
        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(16, 16, 16, 16)

        // Main controls layout
        val mainControlsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Camera toggle button
        cameraToggleButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                marginEnd = 16
            }
            setImageResource(android.R.drawable.ic_menu_camera)
            background = context.getDrawable(android.R.drawable.btn_default)
            contentDescription = "Switch Camera"
        }
        mainControlsLayout.addView(cameraToggleButton)

        // Record button
        recordButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                marginEnd = 16
            }
            setImageResource(android.R.drawable.presence_video_online)
            background = context.getDrawable(android.R.drawable.btn_default)
            contentDescription = "Record"
            scaleType = ImageView.ScaleType.CENTER
        }
        mainControlsLayout.addView(recordButton)

        // Flash toggle button
        flashToggleButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                marginEnd = 16
            }
            setImageResource(android.R.drawable.ic_menu_gallery)
            background = context.getDrawable(android.R.drawable.btn_default)
            contentDescription = "Flash"
        }
        mainControlsLayout.addView(flashToggleButton)

        // Settings button
        settingsButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setImageResource(android.R.drawable.ic_menu_preferences)
            background = context.getDrawable(android.R.drawable.btn_default)
            contentDescription = "Settings"
        }
        mainControlsLayout.addView(settingsButton)

        addView(mainControlsLayout)

        // Status text
        statusText = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topToBottom = mainControlsLayout.id
                topMargin = 16
            }
            text = "Ready to record"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 8, 0, 8)
        }
        addView(statusText)

        // Settings panel (initially hidden)
        createSettingsPanel()
    }

    private fun createSettingsPanel() {
        settingsPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topToBottom = statusText.id
                topMargin = 16
            }
            setPadding(16, 16, 16, 16)
            background = context.getDrawable(android.R.drawable.dialog_holo_light_frame)
            visibility = View.GONE
        }

        // Resolution selection
        val resolutionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val resolutionLabel = TextView(context).apply {
            text = "Resolution:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        resolutionLayout.addView(resolutionLabel)

        resolutionSpinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val resolutions = RGBCameraRecorder.VideoResolution.values().map { it.displayName }
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, resolutions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        resolutionLayout.addView(resolutionSpinner)
        settingsPanel.addView(resolutionLayout)

        // Frame rate selection
        val frameRateLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val frameRateLabel = TextView(context).apply {
            text = "Frame Rate:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        frameRateLayout.addView(frameRateLabel)

        frameRateSpinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val frameRates = listOf("30 FPS", "60 FPS")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, frameRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        frameRateLayout.addView(frameRateSpinner)
        settingsPanel.addView(frameRateLayout)

        // Quality selection
        val qualityLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val qualityLabel = TextView(context).apply {
            text = "Video Quality:"
        }
        qualityLayout.addView(qualityLabel)

        qualitySeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            max = 100
            progress = 80 // Default to high quality
        }
        qualityLayout.addView(qualitySeekBar)
        settingsPanel.addView(qualityLayout)

        // Stabilization toggle
        val stabilizationLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val stabilizationLabel = TextView(context).apply {
            text = "Video Stabilization:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        stabilizationLayout.addView(stabilizationLabel)

        stabilizationToggle = Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            isChecked = true
        }
        stabilizationLayout.addView(stabilizationToggle)
        settingsPanel.addView(stabilizationLayout)

        // Audio toggle
        val audioLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val audioLabel = TextView(context).apply {
            text = "Record Audio:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        audioLayout.addView(audioLabel)

        audioToggle = Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            isChecked = true
        }
        audioLayout.addView(audioToggle)
        settingsPanel.addView(audioLayout)

        addView(settingsPanel)
    }

    private fun setupListeners() {
        cameraToggleButton.setOnClickListener {
            onCameraToggle?.invoke()
        }

        recordButton.setOnClickListener {
            isRecording = !isRecording
            onRecordingToggle?.invoke(isRecording)
            updateRecordingUI()
        }

        flashToggleButton.setOnClickListener {
            currentSettings = currentSettings.copy(enableFlash = !currentSettings.enableFlash)
            onFlashToggle?.invoke(currentSettings.enableFlash)
            updateFlashUI()
        }

        settingsButton.setOnClickListener {
            toggleSettingsPanel()
        }

        resolutionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val resolution = RGBCameraRecorder.VideoResolution.values()[position]
                currentSettings = currentSettings.copy(resolution = resolution)
                onSettingsChanged?.invoke(currentSettings)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        frameRateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val frameRate = if (position == 0) 30 else 60
                currentSettings = currentSettings.copy(frameRate = frameRate)
                onSettingsChanged?.invoke(currentSettings)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val bitRate = (progress / 100f * 15_000_000).toInt() + 1_000_000 // 1-16 Mbps
                    currentSettings = currentSettings.copy(bitRate = bitRate)
                    onSettingsChanged?.invoke(currentSettings)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        stabilizationToggle.setOnCheckedChangeListener { _, isChecked ->
            currentSettings = currentSettings.copy(enableStabilization = isChecked)
            onSettingsChanged?.invoke(currentSettings)
        }

        audioToggle.setOnCheckedChangeListener { _, isChecked ->
            currentSettings = currentSettings.copy(audioEnabled = isChecked)
            onSettingsChanged?.invoke(currentSettings)
        }
    }

    private fun updateUI() {
        updateRecordingUI()
        updateFlashUI()
    }

    private fun updateRecordingUI() {
        if (isRecording) {
            recordButton.setImageResource(android.R.drawable.ic_media_pause)
            statusText.text = "Recording RGB video..."
            statusText.setTextColor(context.getColor(android.R.color.holo_red_dark))
        } else {
            recordButton.setImageResource(android.R.drawable.presence_video_online)
            statusText.text = "Ready to record"
            statusText.setTextColor(context.getColor(android.R.color.primary_text_light))
        }
        
        // Disable settings during recording
        settingsButton.isEnabled = !isRecording
    }

    private fun updateFlashUI() {
        flashToggleButton.alpha = if (currentSettings.enableFlash) 1.0f else 0.5f
    }

    private fun toggleSettingsPanel() {
        isSettingsPanelVisible = !isSettingsPanelVisible
        settingsPanel.visibility = if (isSettingsPanelVisible) View.VISIBLE else View.GONE
        
        settingsButton.setImageResource(
            if (isSettingsPanelVisible) android.R.drawable.ic_menu_close_clear_cancel
            else android.R.drawable.ic_menu_preferences
        )
    }

    // Public methods for external control

    fun setRecordingState(recording: Boolean) {
        isRecording = recording
        updateRecordingUI()
    }

    fun setCameraFacing(facing: RGBCameraRecorder.CameraFacing) {
        // Update camera icon or text based on facing
        cameraToggleButton.contentDescription = facing.displayName
    }

    fun updateRecordingStatus(status: String) {
        statusText.text = status
    }

    fun setAvailableCameraFacing(facingOptions: List<RGBCameraRecorder.CameraFacing>) {
        // Enable/disable camera toggle based on available options
        cameraToggleButton.isEnabled = facingOptions.size > 1
    }

    fun getCurrentSettings(): RGBCameraRecorder.RecordingSettings {
        return currentSettings
    }

    fun updateSettings(settings: RGBCameraRecorder.RecordingSettings) {
        currentSettings = settings
        
        // Update UI to reflect new settings
        resolutionSpinner.setSelection(settings.resolution.ordinal)
        frameRateSpinner.setSelection(if (settings.frameRate == 30) 0 else 1)
        
        val qualityPercentage = ((settings.bitRate - 1_000_000) / 15_000_000f * 100).toInt()
        qualitySeekBar.progress = qualityPercentage.coerceIn(0, 100)
        
        stabilizationToggle.isChecked = settings.enableStabilization
        audioToggle.isChecked = settings.audioEnabled
        
        updateFlashUI()
    }
}