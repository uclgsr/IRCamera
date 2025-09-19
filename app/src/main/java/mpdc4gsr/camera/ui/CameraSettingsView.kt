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

    var onCameraToggle: (() -> Unit)? = null
    var onRecordingToggle: ((Boolean) -> Unit)? = null
    var onSettingsChanged: ((RGBCameraRecorder.RecordingSettings) -> Unit)? = null
    var onFlashToggle: ((Boolean) -> Unit)? = null
    var onStage3ProcessingToggle: ((Boolean) -> Unit)? = null

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
     * Set the state of Samsung Stage3/Level3 processing toggle
     */
    fun setStage3ProcessingEnabled(enabled: Boolean) {
        stage3ProcessingToggle.isChecked = enabled
    }

    /**
     * Show or hide Stage3/Level3 processing toggle based on device capabilities
     */
    fun setStage3ProcessingVisible(visible: Boolean) {
        stage3Layout.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
