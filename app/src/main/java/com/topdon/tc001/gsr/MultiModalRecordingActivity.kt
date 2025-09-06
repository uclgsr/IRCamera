package com.topdon.tc001.gsr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// Removed ARouter import - using NavigationManager instead
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.GSRRecorder
import com.topdon.gsr.service.MultiModalRecordingService
import com.topdon.gsr.service.SessionManager
import com.topdon.gsr.util.TimeUtil
import com.topdon.lib.core.config.RouterConfig
import com.csl.irCamera.R
import java.io.File

/**
 * Full multi-modal recording interface with GSR and thermal coordination
 * Navigation: Use NavigationManager.getInstance().build(RouterConfig.GSR_MULTI_MODAL).navigation(context)
 */
class MultiModalRecordingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MultiModalActivity"
        private const val REQUEST_PERMISSIONS = 100
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            // Android 12+ Bluetooth permissions for Shimmer3 GSR devices
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        
        fun start(context: Context) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java)
            context.startActivity(intent)
        }
        
        fun startWithTemplate(context: Context, templateId: String) {
            val intent = Intent(context, MultiModalRecordingActivity::class.java).apply {
                putExtra("template_id", templateId)
            }
            context.startActivity(intent)
        }
    }
    
    // UI Components
    private lateinit var sessionIdEdit: EditText
    private lateinit var participantIdEdit: EditText
    private lateinit var studyNameEdit: EditText
    private lateinit var startStopButton: Button
    private lateinit var syncEventButton: Button
    private lateinit var statusText: TextView
    private lateinit var sampleCountText: TextView
    private lateinit var syncCountText: TextView
    private lateinit var sessionDurationText: TextView
    private lateinit var fileLocationText: TextView
    private lateinit var progressBar: ProgressBar
    
    // Recording components
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var isRecording = false
    private var currentSession: SessionInfo? = null
    private var sampleCount = 0L
    private var syncMarkCount = 0
    
    private val gsrListener = object : GSRRecorder.GSRRecordingListener {
        override fun onRecordingStarted(sessionInfo: SessionInfo) {
            runOnUiThread {
                isRecording = true
                currentSession = sessionInfo
                updateUI()
                statusText.text = "Recording GSR data at 128 Hz..."
                progressBar.visibility = View.VISIBLE
                
                val sessionDir = "Unknown" // TODO: Fix getSessionDirectory method
                fileLocationText.text = "Files: $sessionDir"
            }
        }
        
        override fun onRecordingStopped(sessionInfo: SessionInfo) {
            runOnUiThread {
                isRecording = false
                currentSession = null
                updateUI()
                statusText.text = "Recording completed. ${sessionInfo.sampleCount} samples recorded."
                progressBar.visibility = View.GONE
                
                Toast.makeText(this@MultiModalRecordingActivity, 
                    "Recording saved: ${sessionInfo.sessionId}", Toast.LENGTH_LONG).show()
            }
        }
        
        override fun onSampleRecorded(sample: GSRSample) {
            sampleCount = sample.sampleIndex
            
            // Update UI every second (128 samples)
            if (sampleCount % 128 == 0L) {
                runOnUiThread {
                    sampleCountText.text = "Samples: $sampleCount"
                    currentSession?.let { session ->
                        val duration = (System.currentTimeMillis() - session.startTime) / 1000
                        sessionDurationText.text = "Duration: ${duration}s"
                    }
                }
            }
        }
        
        override fun onSyncMarkAdded(syncMark: SyncMark) {
            syncMarkCount++
            runOnUiThread {
                syncCountText.text = "Sync Events: $syncMarkCount"
                Toast.makeText(this@MultiModalRecordingActivity,
                    "Sync: ${syncMark.eventType}", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onError(error: String) {
            runOnUiThread {
                statusText.text = "Error: $error"
                progressBar.visibility = View.GONE
                Toast.makeText(this@MultiModalRecordingActivity, 
                    "GSR Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically since we don't have access to layout files
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "Multi-Modal GSR Recording"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)
        
        // Session ID input
        layout.addView(TextView(this).apply { text = "Session ID:" })
        sessionIdEdit = EditText(this).apply {
            setText(TimeUtil.generateSessionId("MultiModal"))
        }
        layout.addView(sessionIdEdit)
        
        // Participant ID input
        layout.addView(TextView(this).apply { text = "Participant ID (optional):" })
        participantIdEdit = EditText(this)
        layout.addView(participantIdEdit)
        
        // Study name input
        layout.addView(TextView(this).apply { text = "Study Name (optional):" })
        studyNameEdit = EditText(this)
        layout.addView(studyNameEdit)
        
        // Start/Stop button
        startStopButton = Button(this).apply {
            text = "Start Recording"
            setOnClickListener { toggleRecording() }
        }
        layout.addView(startStopButton)
        
        // Sync event button
        syncEventButton = Button(this).apply {
            text = "Trigger Sync Event"
            isEnabled = false
            setOnClickListener { triggerSyncEvent() }
        }
        layout.addView(syncEventButton)
        
        // Status and progress
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
        layout.addView(progressBar)
        
        statusText = TextView(this).apply {
            text = "Ready to record"
        }
        layout.addView(statusText)
        
        // Statistics
        sampleCountText = TextView(this).apply { text = "Samples: 0" }
        layout.addView(sampleCountText)
        
        syncCountText = TextView(this).apply { text = "Sync Events: 0" }
        layout.addView(syncCountText)
        
        sessionDurationText = TextView(this).apply { text = "Duration: 0s" }
        layout.addView(sessionDurationText)
        
        fileLocationText = TextView(this).apply { 
            text = "Files: Not started"
            textSize = 12f
        }
        layout.addView(fileLocationText)
        
        setContentView(layout)
        
        // Initialize recording components
        gsrRecorder = GSRRecorder(this)
        sessionManager = SessionManager.getInstance(this)
        gsrRecorder.addListener(gsrListener)
        
        // Check permissions
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val basePermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        
        // Check base permissions
        val baseGranted = basePermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        // Check Android 12+ Bluetooth permissions for Shimmer3 GSR devices
        val bluetoothGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            bluetoothPermissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            // Legacy Bluetooth permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
        
        return baseGranted && bluetoothGranted
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ permissions including Bluetooth for Shimmer3 GSR devices
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Legacy permissions
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        
        ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                statusText.text = "All permissions granted. GSR recording with Shimmer3 devices ready."
            } else {
                statusText.text = "Permissions required for GSR recording and Shimmer3 device access."
                val missingPermissions = mutableListOf<String>()
                
                // Check which specific permissions are missing
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        when (permission) {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                            Manifest.permission.READ_EXTERNAL_STORAGE -> missingPermissions.add("Storage")
                            Manifest.permission.RECORD_AUDIO -> missingPermissions.add("Audio")
                            Manifest.permission.BLUETOOTH_SCAN, 
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN -> missingPermissions.add("Bluetooth (for Shimmer3 GSR)")
                        }
                    }
                }
                
                Toast.makeText(this, 
                    "Missing permissions: ${missingPermissions.joinToString(", ")}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun toggleRecording() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }
        
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    private fun startRecording() {
        val sessionId = sessionIdEdit.text.toString().trim().ifEmpty { 
            TimeUtil.generateSessionId("MultiModal") 
        }
        val participantId = participantIdEdit.text.toString().trim().takeIf { it.isNotEmpty() }
        val studyName = studyNameEdit.text.toString().trim().takeIf { it.isNotEmpty() }
        
        // TODO: Fix suspend function call
        // if (gsrRecorder.startRecording(sessionId, participantId, studyName)) {
        if (true) { // Placeholder
            // Reset counters
            sampleCount = 0
            syncMarkCount = 0
            
            Log.i(TAG, "Multi-modal recording started: $sessionId")
        } else {
            statusText.text = "Failed to start recording"
            Toast.makeText(this, "Failed to start GSR recording", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopRecording() {
        val session = gsrRecorder.stopRecording()
        session?.let {
            Log.i(TAG, "Multi-modal recording stopped: ${it.sessionId}")
        }
    }
    
    private fun triggerSyncEvent() {
        // TODO: Fix addSyncMark method
        // if (gsrRecorder.addSyncMark("USER_TRIGGER")) {
        if (true) { // Placeholder
            Log.d(TAG, "User sync event triggered")
        }
    }
    
    private fun updateUI() {
        startStopButton.text = if (isRecording) "Stop Recording" else "Start Recording"
        syncEventButton.isEnabled = isRecording
        
        sessionIdEdit.isEnabled = !isRecording
        participantIdEdit.isEnabled = !isRecording
        studyNameEdit.isEnabled = !isRecording
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gsrRecorder.removeListener(gsrListener)
        if (isRecording) {
            stopRecording()
        }
    }
}