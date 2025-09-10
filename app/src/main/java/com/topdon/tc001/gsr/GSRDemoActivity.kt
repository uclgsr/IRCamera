package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
// Removed ARouter import - using NavigationManager instead
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrDemoBinding
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.util.TimeUtil
import com.topdon.lib.core.ktbase.BaseBindingActivity

/**
 * Simple GSR demonstration activity showing basic functionality
 * Navigation: Use NavigationManager.getInstance().build(RouterConfig.GSR_DEMO).navigation(context)
 */
class GSRDemoActivity : BaseBindingActivity<ActivityGsrDemoBinding>() {
    companion object {
        private const val TAG = "GSRDemoActivity"

        fun start(context: Context) {
            val intent = Intent(context, GSRDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun initContentLayoutId() = R.layout.activity_gsr_demo

    private lateinit var gsrRecorder: GSRRecorder

    private var isRecording = false
    private var lastSample: GSRSample? = null

    private val gsrListener =
        object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                runOnUiThread {
                    isRecording = true
                    updateButtonStates()
                    binding.statusText.text = "Recording started: ${sessionInfo.sessionId}"
                }
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                runOnUiThread {
                    isRecording = false
                    updateButtonStates()
                    binding.statusText.text = "Recording stopped. ${sessionInfo.sampleCount} samples recorded."

                    val sessionDir = gsrRecorder.getSessionDirectory()?.absolutePath
                    binding.dataText.text = "Session saved to:\n$sessionDir\n\n" +
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
                        binding.dataText.text =
                            buildString {
                                append("Latest Sample #${sample.sampleIndex}:\n")
                                append("Conductance: ${"%.3f".format(sample.conductance)} µS\n")
                                append("Resistance: ${"%.3f".format(sample.resistance)} kΩ\n")
                                append("Timestamp: ${TimeUtil.formatTimestamp(sample.timestamp)}\n")
                                append("Rate: 128 Hz\n\n")

                                val duration =
                                    (
                                        System.currentTimeMillis() -
                                            (gsrRecorder.getCurrentSession()?.startTime ?: System.currentTimeMillis())
                                    ) / 1000
                                append("Recording Duration: ${duration}s")
                            }
                    }
                }
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                runOnUiThread {
                    Toast.makeText(
                        this@GSRDemoActivity,
                        "Sync Event: ${syncMark.eventType}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    binding.statusText.text = "Error: $error"
                    Toast.makeText(this@GSRDemoActivity, error, Toast.LENGTH_LONG).show()
                }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        // Setup click listeners
        binding.startButton.setOnClickListener { startRecording() }
        binding.stopButton.setOnClickListener { stopRecording() }
        binding.syncButton.setOnClickListener { triggerSyncEvent() }

        gsrRecorder = GSRRecorder(this)
        gsrRecorder.addListener(gsrListener)
        updateButtonStates()
    }

    private fun startRecording() {
        val sessionId = TimeUtil.generateSessionId("GSRDemo")

        lifecycleScope.launch {
            val success = gsrRecorder.startRecording(sessionId, "demo_participant", "GSR_Demo_Study")

            if (success) {
                Toast.makeText(this@GSRDemoActivity, "GSR recording started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@GSRDemoActivity, "Failed to start recording", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopRecording() {
        gsrRecorder.stopRecording()
    }

    private fun triggerSyncEvent() {
        val metadata =
            mapOf(
                "trigger" to "manual",
                "timestamp" to TimeUtil.formatTimestamp(System.currentTimeMillis()),
            )

        lifecycleScope.launch {
            val metadataJson = com.google.gson.Gson().toJson(metadata)
            val success = gsrRecorder.addSyncMark("DEMO_SYNC_EVENT", metadataJson)

            if (success) {
                // Success feedback is handled in the listener
                Log.d(TAG, "Demo sync event triggered successfully")
            } else {
                Log.w(TAG, "Failed to trigger demo sync event")
            }
        }
    }

    private fun updateButtonStates() {
        binding.startButton.isEnabled = !isRecording
        binding.stopButton.isEnabled = isRecording
        binding.syncButton.isEnabled = isRecording
    }

    override fun onDestroy() {
        super.onDestroy()
        gsrRecorder.removeListener(gsrListener)
        if (isRecording) {
            gsrRecorder.stopRecording()
        }
    }
}
