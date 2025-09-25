package mpdc4gsr.test

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.controller.RecordingControllerSessionManifest
import mpdc4gsr.core.RecordingService

/**
 * Demonstration of the Session Orchestration and Lifecycle Management system
 *
 * This class shows how the enhanced RecordingController and RecordingService
 * work together to provide comprehensive session management with:
 * - Trigger source tracking (Local UI vs Remote PC)
 * - Partial sensor failure handling
 * - Automatic reconnection logic
 * - Session event logging and manifest generation
 * - Crash recovery integration
 */
class SessionOrchestrationDemo(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "SessionOrchestrationDemo"
    }

    private val recordingController = RecordingController(context, lifecycleOwner)

    /**
     * Demo 1: Local UI triggered session with partial sensor failure
     */
    suspend fun demonstrateLocalSessionWithPartialFailure() {
        Log.i(TAG, "=== Demo 1: Local UI Session with Partial Sensor Failure ===")

        // Initialize sensors (some may fail)
        recordingController.initializeSensors()

        // Start recording via local UI trigger
        val success = recordingController.startRecording(
            sessionId = "demo_local_001",
            participantId = "DEMO_P001",
            studyName = "Session Orchestration Demo",
            enabledSensors = listOf("RGB", "Thermal", "Shimmer"),
            triggerSource = RecordingController.TriggerSource.LOCAL_UI
        )

        if (success) {
            Log.i(TAG, "Recording started successfully (local trigger)")

            // Simulate recording duration
            delay(5000)

            // Stop recording
            val stopSuccess = recordingController.stopRecording(
                triggerSource = RecordingController.TriggerSource.LOCAL_UI
            )

            if (stopSuccess) {
                Log.i(TAG, "Recording stopped successfully")

                // Generate and display session manifest
                val manifest = recordingController.generateSessionManifest()
                displaySessionManifest(manifest)
            }
        } else {
            Log.e(TAG, "Failed to start recording session")
        }
    }

    /**
     * Demo 2: Remote PC triggered session
     */
    suspend fun demonstrateRemoteSessionTrigger() {
        Log.i(TAG, "=== Demo 2: Remote PC Triggered Session ===")

        // Simulate remote PC command
        val success = recordingController.startRecording(
            sessionId = "demo_remote_002",
            participantId = "DEMO_P002",
            studyName = "Remote Control Demo",
            enabledSensors = listOf("RGB", "Shimmer"),
            triggerSource = RecordingController.TriggerSource.REMOTE_PC
        )

        if (success) {
            Log.i(TAG, "Recording started via remote PC command")

            // Simulate recording duration
            delay(3000)

            // Simulate remote stop command
            val stopSuccess = recordingController.stopRecording(
                triggerSource = RecordingController.TriggerSource.REMOTE_PC
            )

            if (stopSuccess) {
                Log.i(TAG, "Recording stopped via remote PC command")

                // Generate manifest showing remote trigger
                val manifest = recordingController.generateSessionManifest()
                displaySessionManifest(manifest)
            }
        }
    }

    /**
     * Demo 3: Session with sensor health monitoring and reconnection
     */
    suspend fun demonstrateSensorReconnection() {
        Log.i(TAG, "=== Demo 3: Sensor Health Monitoring and Reconnection ===")

        // Start recording
        val success = recordingController.startRecording(
            sessionId = "demo_reconnect_003",
            participantId = "DEMO_P003",
            studyName = "Reconnection Demo",
            triggerSource = RecordingController.TriggerSource.LOCAL_UI
        )

        if (success) {
            Log.i(TAG, "Recording started - monitoring sensor health")

            // Simulate recording with sensor monitoring
            // In real scenario, health monitoring runs automatically
            delay(10000)

            // Stop recording 
            recordingController.stopRecording()

            // Show final manifest with any reconnection events
            val manifest = recordingController.generateSessionManifest()
            displaySessionManifest(manifest)
        }
    }

    /**
     * Demo 4: Session state machine demonstration
     */
    suspend fun demonstrateSessionStateMachine() {
        Log.i(TAG, "=== Demo 4: Session State Machine ===")

        // Monitor state flow
        recordingController.recordingStateFlow
            .take(5) // Collect only the first 5 state changes for demonstration
            .collect { state ->
                Log.i(TAG, "Session State: $state")

                when (state) {
                    RecordingController.RecordingState.STARTING -> {
                        Log.i(TAG, "  → Validating prerequisites and initializing sensors")
                    }

                    RecordingController.RecordingState.RECORDING -> {
                        Log.i(TAG, "  → Active recording with health monitoring")
                    }

                    RecordingController.RecordingState.STOPPING -> {
                        Log.i(TAG, "  → Coordinated sensor shutdown in progress")
                    }

                    RecordingController.RecordingState.STOPPED -> {
                        Log.i(TAG, "  → Session finalized, manifest generated")
                    }

                    else -> {
                        Log.i(TAG, "  → State: $state")
                    }
                }
            }
    }

    /**
     * Display session manifest information
     */
    private fun displaySessionManifest(manifest: RecordingControllerSessionManifest) {
        Log.i(TAG, "")
        Log.i(TAG, "=== SESSION MANIFEST ===")
        Log.i(TAG, "Session ID: ${manifest.sessionId}")
        Log.i(TAG, "Trigger Source: ${manifest.triggerSource}")
        Log.i(TAG, "Duration: ${manifest.duration ?: 0}ms")
        Log.i(TAG, "Final State: ${manifest.sessionState}")
        Log.i(TAG, "")

        Log.i(TAG, "Sensor Activity Summary:")
        manifest.sensorActivitySummary.forEach { (sensorName, info) ->
            Log.i(TAG, "  $sensorName:")
            Log.i(TAG, "    - Active: ${info.wasActive}")
            Log.i(TAG, "    - Started Successfully: ${info.startedSuccessfully}")  
            Log.i(TAG, "    - Final Status: ${info.finalStatus}")
            if (info.errorMessages.isNotEmpty()) {
                Log.i(TAG, "    - Errors: ${info.errorMessages.joinToString(", ")}")
            }
            if (info.dropouts.isNotEmpty()) {
                Log.i(TAG, "    - Dropouts: ${info.dropouts.size}")
            }
            if (info.reconnections.isNotEmpty()) {
                Log.i(TAG, "    - Reconnections: ${info.reconnections.size}")
            }
        }

        if (manifest.events.isNotEmpty()) {
            Log.i(TAG, "")
            Log.i(TAG, "Session Events (${manifest.events.size}):")
            manifest.events.take(10).forEach { event ->
                val status = if (event.success) "✓" else "✗"
                Log.i(TAG, "  $status ${event.eventType}${if (event.sensorId != null) " (${event.sensorId})" else ""}")
                if (!event.success && event.errorMessage != null) {
                    Log.i(TAG, "    Error: ${event.errorMessage}")
                }
            }
            if (manifest.events.size > 10) {
                Log.i(TAG, "  ... and ${manifest.events.size - 10} more events")
            }
        }

        if (manifest.errors.isNotEmpty()) {
            Log.i(TAG, "")
            Log.i(TAG, "Session Errors:")
            manifest.errors.forEach { error ->
                Log.i(TAG, "  ✗ $error")
            }
        }

        if (manifest.warnings.isNotEmpty()) {
            Log.i(TAG, "")
            Log.i(TAG, "Session Warnings:")
            manifest.warnings.forEach { warning ->
                Log.i(TAG, "  ⚠ $warning")
            }
        }

        Log.i(TAG, "=== END MANIFEST ===")
        Log.i(TAG, "")
    }

    /**
     * Run all demonstrations
     */
    fun runAllDemonstrations() {
        runBlocking {
            try {
                Log.i(TAG, "Starting Session Orchestration Demonstrations...")
                Log.i(TAG, "")

                // Demo 1: Local UI with partial failure
                demonstrateLocalSessionWithPartialFailure()
                delay(2000)

                // Demo 2: Remote PC trigger
                demonstrateRemoteSessionTrigger()
                delay(2000)

                // Demo 3: Sensor reconnection
                demonstrateSensorReconnection()
                delay(2000)

                Log.i(TAG, "All demonstrations completed successfully!")

            } catch (e: Exception) {
                Log.e(TAG, "Error during demonstrations", e)
            }
        }
    }

    /**
     * Demonstrate RecordingService integration
     */
    fun demonstrateServiceIntegration() {
        Log.i(TAG, "=== RecordingService Integration Demo ===")

        // Start recording service
        RecordingService.startRecording(context, "demo_service_session")

        Log.i(TAG, "Recording service started with session orchestration")
        Log.i(TAG, "- Foreground notification active")
        Log.i(TAG, "- Remote PC commands enabled")
        Log.i(TAG, "- Crash recovery monitoring active")
        Log.i(TAG, "- Session manifest will be generated on stop")

        // Service can be stopped via:
        // - Local UI (notification action)
        // - Remote PC command
        // - API call: RecordingService.stopRecording(context)
    }
}

/**
 * Extension functions for testing session orchestration
 */
fun RecordingController.TriggerSource.getDisplayName(): String {
    return when (this) {
        RecordingController.TriggerSource.LOCAL_UI -> "Local UI"
        RecordingController.TriggerSource.REMOTE_PC -> "Remote PC Command"
        RecordingController.TriggerSource.LOCAL_NOTIFICATION -> "Notification Action"
        RecordingController.TriggerSource.AUTOMATIC -> "Automatic Trigger"
        RecordingController.TriggerSource.CRASH_RECOVERY -> "Crash Recovery"
    }
}

fun RecordingController.SessionState.getDisplayName(): String {
    return when (this) {
        RecordingController.SessionState.IDLE -> "Idle"
        RecordingController.SessionState.STARTING -> "Starting"
        RecordingController.SessionState.RECORDING -> "Recording"
        RecordingController.SessionState.STOPPING -> "Stopping"
        RecordingController.SessionState.STOPPED_COMPLETED -> "Completed"
        RecordingController.SessionState.STOPPED_FAILED -> "Failed"
        RecordingController.SessionState.STOPPED_INCOMPLETE -> "Incomplete"
    }
}