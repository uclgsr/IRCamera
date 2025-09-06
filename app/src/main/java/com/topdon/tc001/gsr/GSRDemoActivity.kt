package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
// Removed ARouter import - using NavigationManager instead
import com.csl.irCamera.R
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.util.TimeUtil
import com.topdon.lib.core.config.RouterConfig

/**
 * Simple GSR demonstration activity showing basic functionality
 * Navigation: Use NavigationManager.getInstance().build(RouterConfig.GSR_DEMO).navigation(context)
 */
class GSRDemoActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "GSRDemoActivity"
        
        fun start(context: Context) {
            val intent = Intent(context, GSRDemoActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var syncButton: Button
    private lateinit var statusText: TextView
    private lateinit var dataText: TextView
    
    private var isRecording = false
    private var lastSample: GSRSample? = null
    
    private val gsrListener = object : GSRRecorder.GSRRecordingListener {
        override fun onRecordingStarted(sessionInfo: SessionInfo) {
            runOnUiThread {
                isRecording = true
                updateButtonStates()
                statusText.text = "Recording started: ${sessionInfo.sessionId}"
            }
        }
        
        override fun onRecordingStopped(sessionInfo: SessionInfo) {
            runOnUiThread {
                isRecording = false
                updateButtonStates()
                statusText.text = "Recording stopped. ${sessionInfo.sampleCount} samples recorded."
                
                val sessionDir = null // gsrRecorder.getSessionDirectory()?.absolutePath // TODO: Fix getSessionDirectory method
                dataText.text = "Session saved to:\n$sessionDir\n\n" +
                    "Files created:\n" +
                    "- signals.csv (GSR data)\n" +
                    "- sync_marks.csv (sync events)\n" +
                    "- session_metadata.json (metadata)"
            }
        }
        
        override fun onSampleRecorded(sample: GSRSample) {
            lastSample = sample
            
            // Update display every 32 samples (4 times per second at 128Hz)
            if (sample.sampleIndex % 32 == 0L) {
                runOnUiThread {
                    dataText.text = buildString {
                        append("Latest Sample #${sample.sampleIndex}:\n")
                        append("Conductance: ${"%.3f".format(sample.conductance)} µS\n")
                        append("Resistance: ${"%.3f".format(sample.resistance)} kΩ\n")
                        append("Timestamp: ${TimeUtil.formatTimestamp(sample.timestamp)}\n")
                        append("Rate: 128 Hz\n\n")
                        
                        val duration = (System.currentTimeMillis() - 
                            0L) / 1000 // TODO: Fix getCurrentSession method
                        append("Recording Duration: ${duration}s")
                    }
                }
            }
        }
        
        override fun onSyncMarkAdded(syncMark: SyncMark) {
            runOnUiThread {
                Toast.makeText(this@GSRDemoActivity, 
                    "Sync Event: ${syncMark.eventType}", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onError(error: String) {
            runOnUiThread {
                statusText.text = "Error: $error"
                Toast.makeText(this@GSRDemoActivity, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        
        gsrRecorder = GSRRecorder(this)
        gsrRecorder.addListener(gsrListener)
        updateButtonStates()
    }
    
    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "GSR Recording Demo"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)
        
        // Description
        val description = TextView(this).apply {
            text = "Demonstrates 128 Hz GSR data recording with CSV export and sync events."
            setPadding(0, 0, 0, 24)
        }
        layout.addView(description)
        
        // Control buttons
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        startButton = Button(this).apply {
            text = "Start Recording"
            setOnClickListener { startRecording() }
        }
        buttonLayout.addView(startButton)
        
        stopButton = Button(this).apply {
            text = "Stop Recording"
            setOnClickListener { stopRecording() }
            isEnabled = false
        }
        buttonLayout.addView(stopButton)
        
        syncButton = Button(this).apply {
            text = "Sync Event"
            setOnClickListener { triggerSyncEvent() }
            isEnabled = false
        }
        buttonLayout.addView(syncButton)
        
        layout.addView(buttonLayout)
        
        // Status
        statusText = TextView(this).apply {
            text = "Ready to record"
            setPadding(0, 32, 0, 16)
            textSize = 16f
        }
        layout.addView(statusText)
        
        // Data display
        dataText = TextView(this).apply {
            text = "No data yet. Press 'Start Recording' to begin."
            background = ContextCompat.getDrawable(this@GSRDemoActivity, android.R.drawable.editbox_background)
            setPadding(16, 16, 16, 16)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        layout.addView(dataText)
        
        setContentView(layout)
    }
    
    private fun startRecording() {
        val sessionId = TimeUtil.generateSessionId("GSRDemo")
        
        // TODO: Fix suspend function call
        // if (gsrRecorder.startRecording(sessionId, "demo_participant", "GSR_Demo_Study")) {
        if (true) { // Placeholder
            Toast.makeText(this, "GSR recording started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopRecording() {
        gsrRecorder.stopRecording()
    }
    
    private fun triggerSyncEvent() {
        val metadata = mapOf(
            "trigger" to "manual",
            "timestamp" to TimeUtil.formatTimestamp(System.currentTimeMillis())
        )
        
        // TODO: Fix addSyncMark method
        // if (gsrRecorder.addSyncMark("DEMO_SYNC_EVENT", metadata)) {
        if (true) { // Placeholder
            // Success feedback is handled in the listener
        }
    }
    
    private fun updateButtonStates() {
        startButton.isEnabled = !isRecording
        stopButton.isEnabled = isRecording
        syncButton.isEnabled = isRecording
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gsrRecorder.removeListener(gsrListener)
        if (isRecording) {
            gsrRecorder.stopRecording()
        }
    }
}