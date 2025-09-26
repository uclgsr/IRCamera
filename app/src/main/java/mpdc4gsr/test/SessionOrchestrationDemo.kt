package mpdc4gsr.test

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.controller.SessionManifest
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
    suspend fun demonstrateLocalSessionWithPartialFailure() {        // Initialize sensors (some may fail)
        recordingController.initializeSensors()

        // Start recording via local UI trigger
        val success = recordingController.startRecording(
            sessionId = "demo_local_001",
            participantId = "DEMO_P001",
            studyName = "Session Orchestration Demo",
            enabledSensors = listOf("RGB", "Thermal", "Shimmer"),
            triggerSource = RecordingController.TriggerSource.LOCAL_UI
        )

        if (success) {")

            // Simulate recording duration
            delay(5000)

            // Stop recording
            val stopSuccess = recordingController.stopRecording(
                triggerSource = RecordingController.TriggerSource.LOCAL_UI
            )

            if (stopSuccess) {                // Generate and display session manifest
                val manifest = recordingController.generateSessionManifest()
                displaySessionManifest(manifest)
            }
        } else {        }
    }

    /**
     * Demo 2: Remote PC triggered session
     */
    suspend fun demonstrateRemoteSessionTrigger() {        // Simulate remote PC command
        val success = recordingController.startRecording(
            sessionId = "demo_remote_002",
            participantId = "DEMO_P002",
            studyName = "Remote Control Demo",
            enabledSensors = listOf("RGB", "Shimmer"),
            triggerSource = RecordingController.TriggerSource.REMOTE_PC
        )

        if (success) {            // Simulate recording duration
            delay(3000)

            // Simulate remote stop command
            val stopSuccess = recordingController.stopRecording(
                triggerSource = RecordingController.TriggerSource.REMOTE_PC
            )

            if (stopSuccess) {                // Generate manifest showing remote trigger
                val manifest = recordingController.generateSessionManifest()
                displaySessionManifest(manifest)
            }
        }
    }

    /**
     * Demo 3: Session with sensor health monitoring and reconnection
     */
    suspend fun demonstrateSensorReconnection() {        // Start recording
        val success = recordingController.startRecording(
            sessionId = "demo_reconnect_003",
            participantId = "DEMO_P003",
            studyName = "Reconnection Demo",
            triggerSource = RecordingController.TriggerSource.LOCAL_UI
        )

        if (success) {            // Simulate recording with sensor monitoring
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
    suspend fun demonstrateSessionStateMachine() {        // Monitor state flow
        recordingController.recordingStateFlow
            .take(5) // Collect only the first 5 state changes for demonstration
            .collect { state ->                when (state) {
                    RecordingController.RecordingState.STARTING -> {                    }

                    RecordingController.RecordingState.RECORDING -> {                    }

                    RecordingController.RecordingState.STOPPING -> {                    }

                    RecordingController.RecordingState.STOPPED -> {                    }

                    else -> {                    }
                }
            }
    }

    /**
     * Display session manifest information
     */
    private fun displaySessionManifest(manifest: SessionManifest) {        manifest.sensorActivitySummary.forEach { (sensorName, info) ->            if (info.errorMessages.isNotEmpty()) {}")
            }
            if (info.dropouts.isNotEmpty()) {            }
            if (info.reconnections.isNotEmpty()) {            }
        }

        if (manifest.events.isNotEmpty()) {:")
            manifest.events.take(10).forEach { event ->
                val status = if (event.success) "✓" else "✗"" (${event.sensorId})" else ""}")
                if (!event.success && event.errorMessage != null) {                }
            }
            if (manifest.events.size > 10) {            }
        }

        if (manifest.errors.isNotEmpty()) {            manifest.errors.forEach { error ->            }
        }

        if (manifest.warnings.isNotEmpty()) {            manifest.warnings.forEach { warning ->            }
        }    }

    /**
     * Run all demonstrations
     */
    fun runAllDemonstrations() {
        runBlocking {
            try {                // Demo 1: Local UI with partial failure
                demonstrateLocalSessionWithPartialFailure()
                delay(2000)

                // Demo 2: Remote PC trigger
                demonstrateRemoteSessionTrigger()
                delay(2000)

                // Demo 3: Sensor reconnection
                demonstrateSensorReconnection()
                delay(2000)            } catch (e: Exception) {            }
        }
    }

    /**
     * Demonstrate RecordingService integration
     */
    fun demonstrateServiceIntegration() {        // Start recording service
        RecordingService.startRecording(context, "demo_service_session")        // Service can be stopped via:
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