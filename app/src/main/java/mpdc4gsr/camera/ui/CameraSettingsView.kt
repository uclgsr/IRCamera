package mpdc4gsr.camera.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import mpdc4gsr.camera.RGBCameraRecorder

class CameraSettingsView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var cameraToggleButton: ImageButton
    private lateinit var resolutionSpinner: Spinner
    private lateinit var flashToggleButton: ImageButton
    private lateinit var recordButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var stabilizationToggle: Switch
    private lateinit var audioToggle: Switch
    private lateinit var stage3ProcessingToggle: Switch
    private lateinit var stage3Layout: LinearLayout
    private lateinit var frameRateSpinner: Spinner
    private lateinit var qualitySeekBar: SeekBar
    private lateinit var settingsPanel: LinearLayout
    private lateinit var statusText: TextView

    private var currentSettings = RGBCameraRecorder.RecordingSettings()
    private var isSettingsPanelVisible = false
    private var isRecording = false

    // Manual camera control elements
    private lateinit var exposureLockButton: ImageButton
    private lateinit var focusLockButton: ImageButton
    private lateinit var exposureCompensationSeekBar: SeekBar
    private lateinit var exposureCompensationText: TextView
    private lateinit var resetControlsButton: ImageButton
    private lateinit var manualControlsPanel: LinearLayout

    var onCameraToggle: (() -> Unit)? = null
    var onRecordingToggle: ((Boolean) -> Unit)? = null
    var onSettingsChanged: ((RGBCameraRecorder.RecordingSettings) -> Unit)? = null
    var onFlashToggle: ((Boolean) -> Unit)? = null
    var onStage3ProcessingToggle: ((Boolean) -> Unit)? = null
    
    // Manual camera control callbacks
    var onExposureLockToggle: ((Boolean) -> Unit)? = null
    var onFocusLockToggle: ((Boolean) -> Unit)? = null  
    var onExposureCompensationChanged: ((Float) -> Unit)? = null
    var onResetCameraControls: (() -> Unit)? = null

    init {
        initView()
        setupListeners()
        updateUI()
    }

    private fun initView() {

        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(16, 16, 16, 16)

        val mainControlsLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

        cameraToggleButton =
            ImageButton(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(120, 120).apply {
                        marginEnd = 16
                    }
                setImageResource(android.R.drawable.ic_menu_camera)
                background = context.getDrawable(android.R.drawable.btn_default)
                contentDescription = "Switch Camera"
            }
        mainControlsLayout.addView(cameraToggleButton)

        recordButton =
            ImageButton(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(160, 160).apply {
                        marginEnd = 16
                    }
                setImageResource(android.R.drawable.presence_video_online)
                background = context.getDrawable(android.R.drawable.btn_default)
                contentDescription = "Record"
                scaleType = ImageView.ScaleType.CENTER
            }
        mainControlsLayout.addView(recordButton)

        flashToggleButton =
            ImageButton(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(120, 120).apply {
                        marginEnd = 16
                    }
                setImageResource(android.R.drawable.ic_menu_gallery)
                background = context.getDrawable(android.R.drawable.btn_default)
                contentDescription = "Flash"
            }
        mainControlsLayout.addView(flashToggleButton)

        settingsButton =
            ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120)
                setImageResource(android.R.drawable.ic_menu_preferences)
                background = context.getDrawable(android.R.drawable.btn_default)
                contentDescription = "Settings"
            }
        mainControlsLayout.addView(settingsButton)

        addView(mainControlsLayout)

        statusText =
            TextView(context).apply {
                layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        topToBottom = mainControlsLayout.id
                        topMargin = 16
                    }
                text = "Ready to record"
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(0, 8, 0, 8)
            }
        addView(statusText)

        createSettingsPanel()
    }

    private fun createSettingsPanel() {
        settingsPanel =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        topToBottom = statusText.id
                        topMargin = 16
                    }
                setPadding(16, 16, 16, 16)
                background = context.getDrawable(android.R.drawable.dialog_holo_light_frame)
                visibility = View.GONE
            }

        val resolutionLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        val resolutionLabel =
            TextView(context).apply {
                text = "Resolution:"
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        resolutionLayout.addView(resolutionLabel)

        resolutionSpinner =
            Spinner(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val resolutions = RGBCameraRecorder.VideoResolution.values().map { it.displayName }
                adapter =
                    ArrayAdapter(context, android.R.layout.simple_spinner_item, resolutions).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
            }
        resolutionLayout.addView(resolutionSpinner)
        settingsPanel.addView(resolutionLayout)

        val frameRateLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        val frameRateLabel =
            TextView(context).apply {
                text = "Frame Rate:"
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        frameRateLayout.addView(frameRateLabel)

        frameRateSpinner =
            Spinner(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val frameRates = listOf("30 FPS", "60 FPS")
                adapter =
                    ArrayAdapter(context, android.R.layout.simple_spinner_item, frameRates).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
            }
        frameRateLayout.addView(frameRateSpinner)
        settingsPanel.addView(frameRateLayout)

        val qualityLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        val qualityLabel =
            TextView(context).apply {
                text = "Video Quality:"
            }
        qualityLayout.addView(qualityLabel)

        qualitySeekBar =
            SeekBar(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                max = 100
                progress = 80
            }
        qualityLayout.addView(qualitySeekBar)
        settingsPanel.addView(qualityLayout)

        val stabilizationLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        val stabilizationLabel =
            TextView(context).apply {
                text = "Video Stabilization:"
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        stabilizationLayout.addView(stabilizationLabel)

        stabilizationToggle =
            Switch(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isChecked = true
            }
        stabilizationLayout.addView(stabilizationToggle)
        settingsPanel.addView(stabilizationLayout)

        val audioLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

        val audioLabel =
            TextView(context).apply {
                text = "Record Audio:"
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        audioLayout.addView(audioLabel)

        audioToggle =
            Switch(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isChecked = true
            }
        audioLayout.addView(audioToggle)
        settingsPanel.addView(audioLayout)

        // Stage3/Level3 Processing toggle
        stage3Layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val stage3Label = TextView(context).apply {
            text = "Samsung Stage3/Level3 RAW:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        stage3Layout.addView(stage3Label)

        stage3ProcessingToggle = Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = true // Enable Stage3/Level3 by default for Samsung devices
        }
        stage3Layout.addView(stage3ProcessingToggle)
        settingsPanel.addView(stage3Layout)

        // Manual Camera Controls Panel
        manualControlsPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 16, 8, 8)
        }

        // Manual controls title
        val manualControlsTitle = TextView(context).apply {
            text = "Manual Camera Controls"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        manualControlsPanel.addView(manualControlsTitle)

        // Exposure Lock
        val exposureLockLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val exposureLockLabel = TextView(context).apply {
            text = "Exposure Lock:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        exposureLockLayout.addView(exposureLockLabel)

        exposureLockButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(android.R.drawable.ic_lock_idle_low_battery) // Using unlocked icon initially
            contentDescription = "Lock/Unlock Exposure"
            background = null
        }
        exposureLockLayout.addView(exposureLockButton)
        manualControlsPanel.addView(exposureLockLayout)

        // Focus Lock
        val focusLockLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val focusLockLabel = TextView(context).apply {
            text = "Focus Lock:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        focusLockLayout.addView(focusLockLabel)

        focusLockButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(android.R.drawable.ic_search_category_default) // Using focus icon
            contentDescription = "Lock/Unlock Focus"
            background = null
        }
        focusLockLayout.addView(focusLockButton)
        manualControlsPanel.addView(focusLockLayout)

        // Exposure Compensation
        val exposureCompLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        exposureCompensationText = TextView(context).apply {
            text = "Exposure Compensation: 0.0 EV"
            setPadding(0, 8, 0, 4)
        }
        exposureCompLayout.addView(exposureCompensationText)

        exposureCompensationSeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            max = 80 // -4.0 to +4.0 EV in 0.1 steps (80 steps total, centered at 40)
            progress = 40 // 0.0 EV
        }
        exposureCompLayout.addView(exposureCompensationSeekBar)
        manualControlsPanel.addView(exposureCompLayout)

        // Reset Controls Button
        resetControlsButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = 8
            }
            setImageResource(android.R.drawable.ic_menu_revert)
            contentDescription = "Reset to Auto Controls"
            background = null
        }
        manualControlsPanel.addView(resetControlsButton)

        settingsPanel.addView(manualControlsPanel)

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

        resolutionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val resolution = RGBCameraRecorder.VideoResolution.values()[position]
                    currentSettings = currentSettings.copy(resolution = resolution)
                    onSettingsChanged?.invoke(currentSettings)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        frameRateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val frameRate = if (position == 0) 30 else 60
                    currentSettings = currentSettings.copy(frameRate = frameRate)
                    onSettingsChanged?.invoke(currentSettings)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        qualitySeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        val bitRate =
                            (progress / 100f * 15_000_000).toInt() + 1_000_000
                        currentSettings = currentSettings.copy(bitRate = bitRate)
                        onSettingsChanged?.invoke(currentSettings)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        stabilizationToggle.setOnCheckedChangeListener { _, isChecked ->
            currentSettings = currentSettings.copy(enableStabilization = isChecked)
            onSettingsChanged?.invoke(currentSettings)
        }

        audioToggle.setOnCheckedChangeListener { _, isChecked ->
            currentSettings = currentSettings.copy(audioEnabled = isChecked)
            onSettingsChanged?.invoke(currentSettings)
        }

        stage3ProcessingToggle.setOnCheckedChangeListener { _, isChecked ->
            onStage3ProcessingToggle?.invoke(isChecked)
        }

        // Manual camera control listeners
        exposureLockButton.setOnClickListener {
            val isLocked = exposureLockButton.tag as? Boolean ?: false
            val newLockState = !isLocked
            exposureLockButton.tag = newLockState
            exposureLockButton.setImageResource(
                if (newLockState) android.R.drawable.ic_lock_lock
                else android.R.drawable.ic_lock_idle_low_battery
            )
            onExposureLockToggle?.invoke(newLockState)
        }

        focusLockButton.setOnClickListener {
            val isLocked = focusLockButton.tag as? Boolean ?: false
            val newLockState = !isLocked
            focusLockButton.tag = newLockState
            focusLockButton.setImageResource(
                if (newLockState) android.R.drawable.ic_menu_mylocation
                else android.R.drawable.ic_search_category_default
            )
            onFocusLockToggle?.invoke(newLockState)
        }

        exposureCompensationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Convert progress (0-80) to exposure compensation (-4.0 to +4.0 EV)
                    val exposureComp = (progress - 40) * 0.1f
                    exposureCompensationText.text = "Exposure Compensation: ${String.format("%.1f", exposureComp)} EV"
                    onExposureCompensationChanged?.invoke(exposureComp)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        resetControlsButton.setOnClickListener {
            resetManualControls()
            onResetCameraControls?.invoke()
        }
    }

    private fun resetManualControls() {
        // Reset exposure lock
        exposureLockButton.tag = false
        exposureLockButton.setImageResource(android.R.drawable.ic_lock_idle_low_battery)
        
        // Reset focus lock
        focusLockButton.tag = false
        focusLockButton.setImageResource(android.R.drawable.ic_search_category_default)
        
        // Reset exposure compensation
        exposureCompensationSeekBar.progress = 40
        exposureCompensationText.text = "Exposure Compensation: 0.0 EV"
    }

    fun updateExposureLockState(locked: Boolean) {
        exposureLockButton.tag = locked
        exposureLockButton.setImageResource(
            if (locked) android.R.drawable.ic_lock_lock
            else android.R.drawable.ic_lock_idle_low_battery
        )
    }

    fun updateFocusLockState(locked: Boolean) {
        focusLockButton.tag = locked
        focusLockButton.setImageResource(
            if (locked) android.R.drawable.ic_menu_mylocation
            else android.R.drawable.ic_search_category_default
        )
    }

    fun updateExposureCompensation(compensation: Float) {
        val progress = ((compensation * 10) + 40).toInt().coerceIn(0, 80)
        exposureCompensationSeekBar.progress = progress
        exposureCompensationText.text = "Exposure Compensation: ${String.format("%.1f", compensation)} EV"
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

        settingsButton.isEnabled = !isRecording
    }

    private fun updateFlashUI() {
        flashToggleButton.alpha = if (currentSettings.enableFlash) 1.0f else 0.5f
    }

    private fun toggleSettingsPanel() {
        isSettingsPanelVisible = !isSettingsPanelVisible
        settingsPanel.visibility = if (isSettingsPanelVisible) View.VISIBLE else View.GONE

        settingsButton.setImageResource(
            if (isSettingsPanelVisible) {
                android.R.drawable.ic_menu_close_clear_cancel
            } else {
                android.R.drawable.ic_menu_preferences
            },
        )
    }


    fun setRecordingState(recording: Boolean) {
        isRecording = recording
        updateRecordingUI()
    }

    fun setCameraFacing(facing: RGBCameraRecorder.CameraFacing) {

        cameraToggleButton.contentDescription = facing.displayName
    }

    fun updateRecordingStatus(status: String) {
        statusText.text = status
    }

    fun setAvailableCameraFacing(facingOptions: List<RGBCameraRecorder.CameraFacing>) {

        cameraToggleButton.isEnabled = facingOptions.size > 1
    }

    fun getCurrentSettings(): RGBCameraRecorder.RecordingSettings {
        return currentSettings
    }

    fun updateSettings(settings: RGBCameraRecorder.RecordingSettings) {
        currentSettings = settings

        resolutionSpinner.setSelection(settings.resolution.ordinal)
        frameRateSpinner.setSelection(if (settings.frameRate == 30) 0 else 1)

        val qualityPercentage = ((settings.bitRate - 1_000_000) / 15_000_000f * 100).toInt()
        qualitySeekBar.progress = qualityPercentage.coerceIn(0, 100)

        stabilizationToggle.isChecked = settings.enableStabilization
        audioToggle.isChecked = settings.audioEnabled

        updateFlashUI()
    }

    /**
     * TODO:  the state of Samsung Stage3/Level3 processing toggle
     */
    fun setStage3ProcessingEnabled(enabled: Boolean) {
        stage3ProcessingToggle.isChecked = enabled
    }

    /**
     * TODO: Show or hide Stage3/Level3 processing toggle based on device capabilities
     */
    fun setStage3ProcessingVisible(visible: Boolean) {
        stage3Layout.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
