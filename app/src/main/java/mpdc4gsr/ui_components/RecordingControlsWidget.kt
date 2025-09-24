package mpdc4gsr.ui_components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Recording Controls Widget with Local and Remote Trigger Support
 * Implements requirements for:
 * - Local start/stop controls integration
 * - Session state display with remote trigger indication
 * - Unified control logic for both local and remote triggers
 */
class RecordingControlsWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RecordingControlsWidget"

        // State colors
        private const val COLOR_IDLE = Color.GRAY
        private const val COLOR_STARTING = Color.YELLOW
        private const val COLOR_RECORDING = Color.RED
        private const val COLOR_STOPPING = 0xFFFFA500.toInt() // Orange color
        private const val COLOR_ERROR = Color.RED
    }

    // UI Elements
    private lateinit var recordButton: Button
    private lateinit var statusIndicator: ImageView
    private lateinit var statusText: TextView
    private lateinit var sessionTimer: TextView
    private lateinit var triggerSourceText: TextView

    // State management
    private var currentState = SessionState.IDLE
    private var isRemoteTriggered = false
    private var sessionStartTime = 0L
    private var sessionId: String? = null
    private var timerJob: Job? = null

    // Callbacks
    var onLocalStartClicked: (() -> Unit)? = null
    var onLocalStopClicked: (() -> Unit)? = null

    enum class SessionState {
        IDLE,
        STARTING,
        RECORDING,
        STOPPING,
        ERROR
    }

    init {
        orientation = VERTICAL
        setPadding(16, 16, 16, 16)
        setupUI()
    }

    private fun setupUI() {
        // Status indicator section
        val statusSection = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        statusIndicator = ImageView(context).apply {
            layoutParams = LayoutParams(24, 24).apply {
                marginEnd = 8
            }
            setImageResource(android.R.drawable.ic_media_pause)
            setColorFilter(COLOR_IDLE)
        }
        statusSection.addView(statusIndicator)

        statusText = TextView(context).apply {
            text = "Ready to Record"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        statusSection.addView(statusText)

        addView(statusSection)

        // Session timer
        sessionTimer = TextView(context).apply {
            text = "00:00"
            textSize = 20f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
        addView(sessionTimer)

        // Trigger source indicator
        triggerSourceText = TextView(context).apply {
            text = ""
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_dark))
            setPadding(0, 0, 0, 8)
        }
        addView(triggerSourceText)

        // Record button
        recordButton = Button(context).apply {
            text = "START RECORDING"
            textSize = 16f
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            setTextColor(Color.WHITE)
        }
        addView(recordButton)

        // Setup listeners
        recordButton.setOnClickListener {
            when (currentState) {
                SessionState.IDLE -> {
                    onLocalStartClicked?.invoke()
                }
                SessionState.RECORDING -> {
                    onLocalStopClicked?.invoke()
                }
                else -> {
                    // Button disabled during transitions
                }
            }
        }
    }

    /**
     * Update session state - handles both local and remote triggers
     */
    fun updateSessionState(
        state: SessionState,
        sessionId: String? = null,
        isRemoteTriggered: Boolean = false
    ) {
        this.currentState = state
        this.sessionId = sessionId
        this.isRemoteTriggered = isRemoteTriggered

        when (state) {
            SessionState.IDLE -> {
                statusIndicator.setImageResource(android.R.drawable.ic_media_pause)
                statusIndicator.setColorFilter(COLOR_IDLE)
                statusText.text = "Ready to Record"
                recordButton.text = "START RECORDING"
                recordButton.isEnabled = true
                recordButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                sessionTimer.text = "00:00"
                triggerSourceText.text = ""
                stopTimer()
            }

            SessionState.STARTING -> {
                statusIndicator.setImageResource(android.R.drawable.ic_media_play)
                statusIndicator.setColorFilter(COLOR_STARTING)
                statusText.text = "Starting..."
                recordButton.text = "STARTING..."
                recordButton.isEnabled = false
                recordButton.setBackgroundColor(COLOR_STARTING)
                updateTriggerSourceDisplay()
            }

            SessionState.RECORDING -> {
                statusIndicator.setImageResource(android.R.drawable.ic_media_play)
                statusIndicator.setColorFilter(COLOR_RECORDING)
                statusText.text = "Recording"
                recordButton.text = "STOP RECORDING"
                recordButton.isEnabled = true
                recordButton.setBackgroundColor(COLOR_RECORDING)
                sessionStartTime = System.currentTimeMillis()
                startTimer()
                updateTriggerSourceDisplay()
            }

            SessionState.STOPPING -> {
                statusIndicator.setImageResource(android.R.drawable.ic_media_pause)
                statusIndicator.setColorFilter(COLOR_STOPPING)
                statusText.text = "Stopping..."
                recordButton.text = "STOPPING..."
                recordButton.isEnabled = false
                recordButton.setBackgroundColor(COLOR_STOPPING)
                stopTimer()
            }

            SessionState.ERROR -> {
                statusIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
                statusIndicator.setColorFilter(COLOR_ERROR)
                statusText.text = "Error"
                recordButton.text = "START RECORDING"
                recordButton.isEnabled = true
                recordButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                sessionTimer.text = "ERROR"
                triggerSourceText.text = ""
                stopTimer()
            }
        }
    }

    private fun updateTriggerSourceDisplay() {
        triggerSourceText.text = when {
            isRemoteTriggered && currentState == SessionState.RECORDING -> 
                "Recording (Remote Control)"
            isRemoteTriggered && currentState == SessionState.STARTING -> 
                "Starting (Remote Control)"
            !isRemoteTriggered && currentState == SessionState.RECORDING -> 
                "Recording (Local Control)"
            !isRemoteTriggered && currentState == SessionState.STARTING -> 
                "Starting (Local Control)"
            else -> ""
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = (context as? LifecycleOwner)?.lifecycleScope?.launch {
            while (currentState == SessionState.RECORDING) {
                val elapsed = System.currentTimeMillis() - sessionStartTime
                sessionTimer.text = formatTime(elapsed)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Enable/disable local controls (useful when only remote control is desired)
     */
    fun setLocalControlsEnabled(enabled: Boolean) {
        recordButton.isEnabled = enabled && (currentState == SessionState.IDLE || currentState == SessionState.RECORDING)
        if (!enabled) {
            recordButton.alpha = 0.5f
        } else {
            recordButton.alpha = 1.0f
        }
    }

    /**
     * Get current recording duration in milliseconds
     */
    fun getCurrentDuration(): Long {
        return if (currentState == SessionState.RECORDING) {
            System.currentTimeMillis() - sessionStartTime
        } else {
            0L
        }
    }
}