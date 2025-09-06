package com.topdon.tc001.camera.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.csl.irCamera.R

/**
 * Real-time status indicator for parallel multi-modal recording
 * Shows active sensors, recording duration, and session information
 */
class RecordingStatusIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusIcon: ImageView
    private val statusText: TextView
    private val durationText: TextView
    private val sensorsText: TextView
    
    private var isRecording = false
    private var startTime = 0L
    private var sessionId = ""
    private var activeSensors = emptySet<SensorSelectionDialog.SensorType>()

    init {
        orientation = VERTICAL
        setPadding(16, 8, 16, 8)
        gravity = Gravity.CENTER
        
        // Recording status icon
        statusIcon = ImageView(context).apply {
            layoutParams = LayoutParams(32, 32).apply {
                gravity = Gravity.CENTER
                bottomMargin = 4
            }
            // Would normally use a drawable, but creating programmatically
            setBackgroundColor(Color.LTGRAY)
        }
        addView(statusIcon)
        
        // Status text (Recording/Stopped)
        statusText = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            gravity = Gravity.CENTER
        }
        addView(statusText)
        
        // Duration counter
        durationText = TextView(context).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            gravity = Gravity.CENTER
        }
        addView(durationText)
        
        // Active sensors list
        sensorsText = TextView(context).apply {
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, android.R.color.tertiary_text_dark))
            gravity = Gravity.CENTER
        }
        addView(sensorsText)
        
        updateDisplay()
    }

    /**
     * Start recording indicator
     */
    fun startRecording(
        sessionId: String,
        sensors: Set<SensorSelectionDialog.SensorType>
    ) {
        this.sessionId = sessionId
        this.activeSensors = sensors
        this.startTime = System.currentTimeMillis()
        this.isRecording = true
        
        updateDisplay()
        
        // Start duration counter
        startDurationCounter()
    }

    /**
     * Stop recording indicator
     */
    fun stopRecording() {
        this.isRecording = false
        updateDisplay()
    }

    /**
     * Update sensor status
     */
    fun updateSensorStatus(sensor: SensorSelectionDialog.SensorType, status: String) {
        // For detailed status updates, we could show individual sensor states
        updateDisplay()
    }

    private fun updateDisplay() {
        if (isRecording) {
            statusIcon.setBackgroundColor(Color.RED)
            statusText.text = "🔴 RECORDING"
            statusText.setTextColor(Color.RED)
            
            sensorsText.text = activeSensors.joinToString(" • ") { 
                when (it) {
                    SensorSelectionDialog.SensorType.THERMAL -> "🌡️"
                    SensorSelectionDialog.SensorType.RGB -> "📸"
                    SensorSelectionDialog.SensorType.GSR -> "📊"
                }
            }
            
            visibility = VISIBLE
        } else {
            statusIcon.setBackgroundColor(Color.GRAY)
            statusText.text = "⏹️ STOPPED"
            statusText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            durationText.text = ""
            sensorsText.text = ""
            
            // Hide when not recording to save screen space
            visibility = GONE
        }
    }

    private fun startDurationCounter() {
        if (!isRecording) return
        
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        
        durationText.text = String.format("%02d:%02d", minutes, seconds)
        
        // Update every second
        postDelayed({ startDurationCounter() }, 1000)
    }

    /**
     * Show/hide the indicator
     */
    fun setVisible(visible: Boolean) {
        visibility = if (visible) VISIBLE else GONE
    }
}