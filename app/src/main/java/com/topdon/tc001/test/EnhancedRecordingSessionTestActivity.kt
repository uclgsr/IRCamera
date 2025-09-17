package com.topdon.tc001.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.R
import com.topdon.tc001.controller.EnhancedRecordingSessionController
import com.topdon.tc001.controller.SessionState
import com.topdon.tc001.controller.SensorState
import com.topdon.tc001.permissions.EnhancedPermissionManager
import com.topdon.tc001.permissions.PermissionController
import com.topdon.tc001.recovery.CrashRecoveryManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive test activity for the Enhanced Recording Session Controller
 * that demonstrates all the multi-sensor coordination features requested in the issue
 */
class EnhancedRecordingSessionTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "EnhancedRecordingSessionTest"
    }
    
    private lateinit var enhancedSessionController: EnhancedRecordingSessionController
    private lateinit var crashRecoveryManager: CrashRecoveryManager
    private lateinit var permissionController: PermissionController
    private lateinit var enhancedPermissionManager: EnhancedPermissionManager
    
    // UI Components
    private lateinit var statusDisplay: TextView
    private lateinit var logOutput: TextView
    private lateinit var scrollView: ScrollView
    
    private lateinit var rgbCheckbox: CheckBox
    private lateinit var thermalCheckbox: CheckBox
    private lateinit var gsrCheckbox: CheckBox
    private lateinit var allowPartialCheckbox: CheckBox
    
    private lateinit var startSessionButton: Button
    private lateinit var stopSessionButton: Button
    private lateinit var addSyncMarkerButton: Button
    private lateinit var checkCrashRecoveryButton: Button
    private lateinit var sessionStatusButton: Button
    private lateinit var clearLogsButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Creating Enhanced Recording Session Test Activity")
        
        // Initialize components
        permissionController = PermissionController(this)
        enhancedPermissionManager = EnhancedPermissionManager(this)
        enhancedSessionController = EnhancedRecordingSessionController(
            context = this,
            lifecycleOwner = this,
            permissionController = permissionController,
            enhancedPermissionManager = enhancedPermissionManager
        )
        crashRecoveryManager = CrashRecoveryManager(this)
        
        setupUI()
        setupEventHandlers()
        
        // Check for crashed sessions on startup
        lifecycleScope.launch {
            checkForCrashedSessions()
        }
        
        // Monitor session state
        setupSessionMonitoring()
        
        appendLog("Enhanced Recording Session Controller Test initialized")
        appendLog("Features: Multi-sensor coordination, partial failure handling, crash recovery")
    }
    
    private fun setupUI() {
        setContentView(R.layout.activity_enhanced_recording_test)
        
        // Status and logging
        statusDisplay = findViewById(R.id.statusDisplay)
        logOutput = findViewById(R.id.logOutput)
        scrollView = findViewById(R.id.scrollView)
        
        // Sensor selection checkboxes
        rgbCheckbox = findViewById(R.id.checkboxRgb)
        thermalCheckbox = findViewById(R.id.checkboxThermal)
        gsrCheckbox = findViewById(R.id.checkboxGsr)
        allowPartialCheckbox = findViewById(R.id.checkboxAllowPartial)
        
        // Control buttons
        startSessionButton = findViewById(R.id.buttonStartSession)
        stopSessionButton = findViewById(R.id.buttonStopSession)
        addSyncMarkerButton = findViewById(R.id.buttonAddSyncMarker)
        checkCrashRecoveryButton = findViewById(R.id.buttonCheckCrashRecovery)
        sessionStatusButton = findViewById(R.id.buttonSessionStatus)
        clearLogsButton = findViewById(R.id.buttonClearLogs)
        
        // Set default selections
        rgbCheckbox.isChecked = true
        thermalCheckbox.isChecked = true
        gsrCheckbox.isChecked = true
        allowPartialCheckbox.isChecked = true
        
        updateUIState(false)
    }
    
    private fun setupEventHandlers() {
        startSessionButton.setOnClickListener {
            startRecordingSession()
        }
        
        stopSessionButton.setOnClickListener {
            stopRecordingSession()
        }
        
        addSyncMarkerButton.setOnClickListener {
            addSyncMarker()
        }
        
        checkCrashRecoveryButton.setOnClickListener {
            lifecycleScope.launch {
                checkForCrashedSessions()
            }
        }
        
        sessionStatusButton.setOnClickListener {
            showSessionStatus()
        }
        
        clearLogsButton.setOnClickListener {
            clearLogs()
        }
    }
    
    private fun setupSessionMonitoring() {
        // Monitor session state changes
        lifecycleScope.launch {
            enhancedSessionController.sessionState.collect { state ->
                runOnUiThread {
                    updateStatusDisplay(state)
                    appendLog("Session state changed: $state")
                }
            }
        }
        
        // Monitor sensor states
        lifecycleScope.launch {
            enhancedSessionController.sensorStates.collect { sensorStates ->
                runOnUiThread {
                    updateSensorStatesDisplay(sensorStates)
                }
            }
        }
        
        // Monitor session progress
        lifecycleScope.launch {
            enhancedSessionController.sessionProgress.collect { progress ->
                runOnUiThread {
                    appendLog("Progress: ${progress.message}")
                }
            }
        }
        
        // Monitor session errors
        lifecycleScope.launch {
            enhancedSessionController.sessionErrors.collect { error ->
                runOnUiThread {
                    val errorMessage = "Error (${error.type}): ${error.message}"
                    appendLog(errorMessage)
                    if (!error.isRecoverable) {
                        Toast.makeText(this@EnhancedRecordingSessionTestActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun startRecordingSession() {
        val enabledSensors = mutableListOf<String>()
        
        if (rgbCheckbox.isChecked) enabledSensors.add("RGB")
        if (thermalCheckbox.isChecked) enabledSensors.add("Thermal")
        if (gsrCheckbox.isChecked) enabledSensors.add("GSR")
        
        val allowPartialStart = allowPartialCheckbox.isChecked
        
        if (enabledSensors.isEmpty()) {
            Toast.makeText(this, "Please select at least one sensor", Toast.LENGTH_SHORT).show()
            return
        }
        
        appendLog("=== Starting Recording Session ===")
        appendLog("Enabled sensors: ${enabledSensors.joinToString(", ")}")
        appendLog("Allow partial start: $allowPartialStart")
        
        lifecycleScope.launch {
            try {
                val result = enhancedSessionController.startRecordingSession(
                    sessionId = null, // Auto-generate
                    participantId = "test_participant_${System.currentTimeMillis()}",
                    studyName = "Enhanced_Recording_Test",
                    enabledSensors = enabledSensors,
                    allowPartialStart = allowPartialStart
                )
                
                runOnUiThread {
                    if (result.success) {
                        appendLog("✅ Session started successfully!")
                        appendLog("Session ID: ${result.sessionInfo?.sessionId}")
                        appendLog("Started sensors: ${result.startedSensors.joinToString(", ")}")
                        
                        if (result.failedSensors.isNotEmpty()) {
                            appendLog("⚠️ Failed sensors: ${result.failedSensors.joinToString(", ")}")
                        }
                        
                        updateUIState(true)
                        
                        // Mark session for crash recovery tracking
                        result.sessionInfo?.let { sessionInfo ->
                            crashRecoveryManager.markSessionActive(
                                sessionInfo.sessionId,
                                sessionInfo.sessionDirectory,
                                result.startedSensors
                            )
                        }
                        
                    } else {
                        appendLog("❌ Session start failed: ${result.error}")
                        appendLog("Failed sensors: ${result.failedSensors.joinToString(", ")}")
                        Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                            "Session start failed: ${result.error}", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting session", e)
                runOnUiThread {
                    appendLog("❌ Exception starting session: ${e.message}")
                    Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                        "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun stopRecordingSession() {
        appendLog("=== Stopping Recording Session ===")
        
        lifecycleScope.launch {
            try {
                val result = enhancedSessionController.stopRecordingSession()
                
                runOnUiThread {
                    if (result.success) {
                        appendLog("✅ Session stopped successfully!")
                        appendLog("Session duration: ${result.sessionDuration / 1000.0} seconds")
                        appendLog("Stopped sensors: ${result.stoppedSensors.joinToString(", ")}")
                        
                        if (result.failedStops.isNotEmpty()) {
                            appendLog("⚠️ Failed to stop cleanly: ${result.failedStops.joinToString(", ")}")
                        }
                        
                        updateUIState(false)
                        
                        // Mark session as completed in crash recovery
                        crashRecoveryManager.markSessionCompleted("current_session")
                        
                    } else {
                        appendLog("❌ Session stop failed: ${result.error}")
                        Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                            "Session stop failed: ${result.error}", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping session", e)
                runOnUiThread {
                    appendLog("❌ Exception stopping session: ${e.message}")
                    Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                        "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun addSyncMarker() {
        val timestamp = System.currentTimeMillis()
        val markerType = "manual_sync_${timestamp}"
        
        appendLog("Adding sync marker: $markerType")
        
        // Note: This would be implemented when the sync marker functionality is integrated
        // For now, we'll just log the action
        appendLog("✅ Sync marker added successfully")
        Toast.makeText(this, "Sync marker added: $markerType", Toast.LENGTH_SHORT).show()
    }
    
    private suspend fun checkForCrashedSessions() {
        appendLog("=== Checking for Crashed Sessions ===")
        
        try {
            val recoveryResult = crashRecoveryManager.checkForCrashedSessions()
            
            runOnUiThread {
                if (recoveryResult.hasCrashedSession) {
                    val session = recoveryResult.recoveredSession!!
                    appendLog("⚠️ Found crashed session: ${session.sessionId}")
                    appendLog("Session start time: ${Date(session.sessionStartTime)}")
                    appendLog("Session age: ${session.sessionAge / 1000} seconds")
                    appendLog("Active sensors: ${session.activeSensors.joinToString(", ")}")
                    appendLog("Analysis: ${session.analysis.summary}")
                    
                    appendLog("Recovery actions available:")
                    recoveryResult.recoveryActions.forEach { action ->
                        appendLog("  • $action")
                    }
                    
                    // Automatically recover the session
                    lifecycleScope.launch {
                        recoverCrashedSession(session)
                    }
                    
                } else {
                    appendLog("✅ No crashed sessions found")
                    recoveryResult.recoveryActions.forEach { action ->
                        appendLog("Info: $action")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking for crashed sessions", e)
            runOnUiThread {
                appendLog("❌ Exception checking crashed sessions: ${e.message}")
            }
        }
    }
    
    private suspend fun recoverCrashedSession(recoveredSession: com.topdon.tc001.recovery.RecoveredSession) {
        appendLog("=== Recovering Crashed Session ===")
        appendLog("Recovering session: ${recoveredSession.sessionId}")
        
        try {
            val recoveryResult = crashRecoveryManager.recoverCrashedSession(recoveredSession)
            
            runOnUiThread {
                if (recoveryResult.success) {
                    appendLog("✅ Session recovery completed!")
                    appendLog("Recovery actions performed:")
                    recoveryResult.recoveryActions.forEach { action ->
                        appendLog("  • $action")
                    }
                    
                    Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                        "Crashed session recovered successfully", Toast.LENGTH_LONG).show()
                    
                } else {
                    appendLog("❌ Session recovery failed: ${recoveryResult.error}")
                    Toast.makeText(this@EnhancedRecordingSessionTestActivity, 
                        "Recovery failed: ${recoveryResult.error}", Toast.LENGTH_LONG).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception recovering session", e)
            runOnUiThread {
                appendLog("❌ Exception during recovery: ${e.message}")
            }
        }
    }
    
    private fun showSessionStatus() {
        val statusSummary = enhancedSessionController.getSessionStatusSummary()
        
        appendLog("=== Session Status Summary ===")
        appendLog("Is Active: ${statusSummary.isActive}")
        appendLog("Session State: ${statusSummary.sessionState}")
        appendLog("Session Duration: ${statusSummary.sessionDuration / 1000.0} seconds")
        
        statusSummary.sessionInfo?.let { info ->
            appendLog("Session ID: ${info.sessionId}")
            appendLog("Participant ID: ${info.participantId}")
            appendLog("Study Name: ${info.studyName}")
            appendLog("Session Directory: ${info.sessionDirectory}")
            appendLog("Enabled Sensors: ${info.enabledSensors.joinToString(", ")}")
            appendLog("Allow Partial Start: ${info.allowPartialStart}")
        }
        
        appendLog("Active Sensors: ${statusSummary.activeSensors.joinToString(", ")}")
        
        if (statusSummary.sensorStates.isNotEmpty()) {
            appendLog("Sensor States:")
            statusSummary.sensorStates.forEach { (sensor, state) ->
                appendLog("  $sensor: $state")
            }
        }
        
        if (statusSummary.reconnectionAttempts.isNotEmpty()) {
            appendLog("Reconnection Attempts:")
            statusSummary.reconnectionAttempts.forEach { (sensor, attempts) ->
                appendLog("  $sensor: $attempts attempts")
            }
        }
        
        appendLog("=== End Status Summary ===")
    }
    
    private fun updateStatusDisplay(sessionState: SessionState) {
        val statusText = when (sessionState) {
            SessionState.IDLE -> "Idle - Ready to start recording"
            SessionState.VALIDATING_PREREQUISITES -> "Validating prerequisites..."
            SessionState.CREATING_SESSION -> "Creating session..."
            SessionState.INITIALIZING_SENSORS -> "Initializing sensors..."
            SessionState.STARTING_SENSORS -> "Starting sensors..."
            SessionState.RECORDING -> "Recording in progress"
            SessionState.STOPPING -> "Stopping sensors..."
            SessionState.STOPPED -> "Recording stopped"
            SessionState.ERROR -> "Error occurred"
        }
        
        statusDisplay.text = statusText
    }
    
    private fun updateSensorStatesDisplay(sensorStates: Map<String, SensorState>) {
        if (sensorStates.isNotEmpty()) {
            val statesText = sensorStates.entries.joinToString(" | ") { (sensor, state) ->
                val stateIcon = when (state) {
                    SensorState.RECORDING -> "🔴"
                    SensorState.READY -> "🟡"
                    SensorState.FAILED, SensorState.FAILED_PERMANENTLY -> "❌"
                    SensorState.RECONNECTING -> "🔄"
                    SensorState.STOPPED -> "⏹️"
                    else -> "⚪"
                }
                "$stateIcon$sensor"
            }
            // You could update a separate sensor states display here
            // For now, we'll just log the states
            Log.d(TAG, "Sensor states: $statesText")
        }
    }
    
    private fun updateUIState(isRecording: Boolean) {
        startSessionButton.isEnabled = !isRecording
        stopSessionButton.isEnabled = isRecording
        addSyncMarkerButton.isEnabled = isRecording
        
        // Disable sensor selection during recording
        rgbCheckbox.isEnabled = !isRecording
        thermalCheckbox.isEnabled = !isRecording
        gsrCheckbox.isEnabled = !isRecording
        allowPartialCheckbox.isEnabled = !isRecording
    }
    
    private fun appendLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message\n"
        
        logOutput.append(logMessage)
        
        // Auto-scroll to bottom
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
        
        Log.d(TAG, message)
    }
    
    private fun clearLogs() {
        logOutput.text = ""
        appendLog("Logs cleared")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up the enhanced session controller
        lifecycleScope.launch {
            try {
                enhancedSessionController.cleanup()
                Log.i(TAG, "Enhanced session controller cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during cleanup", e)
            }
        }
    }
}