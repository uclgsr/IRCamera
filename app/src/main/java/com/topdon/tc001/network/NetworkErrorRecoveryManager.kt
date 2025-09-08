package com.topdon.tc001.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Network error recovery and reconnection management
 * Handles automatic reconnection, connection health monitoring, and error recovery strategies
 */
class NetworkErrorRecoveryManager(
    private val context: Context,
    private val networkClient: NetworkClient
) {
    companion object {
        private const val TAG = "NetworkErrorRecovery"
        private const val MAX_RECONNECTION_ATTEMPTS = 10
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L // 30 seconds
        private const val HEALTH_CHECK_INTERVAL_MS = 15000L // 15 seconds
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val RAPID_FAILURE_THRESHOLD = 3
        private const val RAPID_FAILURE_WINDOW_MS = 60000L // 1 minute
    }

    private val recoveryJob = SupervisorJob()
    private val recoveryScope = CoroutineScope(Dispatchers.IO + recoveryJob)

    private val isRecoveryActive = AtomicBoolean(false)
    private val reconnectionAttempts = AtomicInteger(0)
    private val rapidFailureCount = AtomicInteger(0)
    private var lastFailureTime = 0L
    private var lastKnownGoodController: NetworkClient.ControllerInfo? = null
    private var healthCheckJob: Job? = null

    interface RecoveryEventListener {
        fun onRecoveryStarted(reason: String)
        fun onRecoveryAttempt(attempt: Int, maxAttempts: Int)
        fun onRecoverySuccess(controller: NetworkClient.ControllerInfo)
        fun onRecoveryFailed(reason: String)
        fun onConnectionHealthChanged(isHealthy: Boolean)
        fun onRapidFailureDetected(failureCount: Int)
    }

    private var eventListener: RecoveryEventListener? = null

    fun setEventListener(listener: RecoveryEventListener?) {
        eventListener = listener
    }

    /**
     * Start monitoring connection health and enable automatic recovery
     */
    fun enableAutoRecovery() {
        if (isRecoveryActive.get()) {
            Log.w(TAG, "Auto recovery already enabled")
            return
        }

        isRecoveryActive.set(true)
        startHealthMonitoring()
        Log.i(TAG, "Network error recovery enabled")
    }

    /**
     * Stop automatic recovery and health monitoring
     */
    fun disableAutoRecovery() {
        if (!isRecoveryActive.get()) {
            Log.w(TAG, "Auto recovery not active")
            return
        }

        isRecoveryActive.set(false)
        stopHealthMonitoring()
        Log.i(TAG, "Network error recovery disabled")
    }

    /**
     * Manually trigger connection recovery
     */
    suspend fun triggerRecovery(reason: String): Boolean {
        if (isRecoveryActive.get() && reconnectionAttempts.get() > 0) {
            Log.w(TAG, "Recovery already in progress")
            return false
        }

        return performRecovery(reason)
    }

    /**
     * Record a successful connection for future recovery attempts
     */
    fun recordSuccessfulConnection(controller: NetworkClient.ControllerInfo) {
        lastKnownGoodController = controller
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
        Log.i(TAG, "Recorded successful connection: ${controller.deviceName}")
    }

    /**
     * Handle network error and potentially trigger recovery
     */
    fun handleNetworkError(operation: String, error: String) {
        Log.w(TAG, "Network error in $operation: $error")

        if (isRapidFailure()) {
            eventListener?.onRapidFailureDetected(rapidFailureCount.get())
            // Delay recovery for rapid failures to avoid overwhelming the network
            recoveryScope.launch {
                delay(5000)
                if (isRecoveryActive.get()) {
                    performRecovery("Rapid failure in $operation: $error")
                }
            }
        } else if (isRecoveryActive.get()) {
            recoveryScope.launch {
                performRecovery("Error in $operation: $error")
            }
        }
    }

    private fun startHealthMonitoring() {
        healthCheckJob = recoveryScope.launch {
            while (isRecoveryActive.get() && isActive) {
                try {
                    val isHealthy = performHealthCheck()
                    eventListener?.onConnectionHealthChanged(isHealthy)

                    if (!isHealthy && isRecoveryActive.get()) {
                        performRecovery("Health check failed")
                    }

                    delay(HEALTH_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Health monitoring error", e)
                        delay(HEALTH_CHECK_INTERVAL_MS)
                    }
                }
            }
        }
    }

    private fun stopHealthMonitoring() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    private suspend fun performHealthCheck(): Boolean {
        if (!networkClient.isConnected()) {
            return false
        }

        return try {
            // Send a simple ping message to test connectivity
            val pingMessage = org.json.JSONObject().apply {
                put("message_type", "ping")
                put("timestamp", System.currentTimeMillis())
            }

            // Use a shorter timeout for health checks
            withTimeout(5000) {
                networkClient.sendMeasurementData("health_check", pingMessage)
            }
            true
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed: ${e.message}")
            false
        }
    }

    private suspend fun performRecovery(reason: String): Boolean {
        if (reconnectionAttempts.get() >= MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "Maximum reconnection attempts reached")
            eventListener?.onRecoveryFailed("Maximum attempts reached")
            return false
        }

        Log.i(TAG, "Starting connection recovery: $reason")
        eventListener?.onRecoveryStarted(reason)

        var success = false
        val maxAttempts = MAX_RECONNECTION_ATTEMPTS

        while (reconnectionAttempts.get() < maxAttempts && isRecoveryActive.get()) {
            val attempt = reconnectionAttempts.incrementAndGet()
            
            Log.i(TAG, "Recovery attempt $attempt/$maxAttempts")
            eventListener?.onRecoveryAttempt(attempt, maxAttempts)

            try {
                // Try to reconnect to last known good controller
                val controller = lastKnownGoodController
                if (controller != null) {
                    success = attemptReconnection(controller)
                } else {
                    // Fallback: try to discover new controllers
                    success = attemptDiscoveryAndConnect()
                }

                if (success) {
                    Log.i(TAG, "Recovery successful after $attempt attempts")
                    eventListener?.onRecoverySuccess(lastKnownGoodController ?: 
                        NetworkClient.ControllerInfo("unknown", 0, "Recovered", emptyList()))
                    reconnectionAttempts.set(0)
                    break
                } else {
                    val delay = calculateRetryDelay(attempt)
                    Log.d(TAG, "Recovery attempt $attempt failed, retrying in ${delay}ms")
                    delay(delay)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recovery attempt $attempt failed with exception", e)
                val delay = calculateRetryDelay(attempt)
                delay(delay)
            }
        }

        if (!success) {
            Log.e(TAG, "Connection recovery failed after $maxAttempts attempts")
            eventListener?.onRecoveryFailed("All attempts exhausted")
        }

        return success
    }

    private suspend fun attemptReconnection(controller: NetworkClient.ControllerInfo): Boolean {
        return try {
            Log.d(TAG, "Attempting reconnection to ${controller.deviceName} at ${controller.ipAddress}")
            
            // Disconnect first to clean up any existing connection
            networkClient.disconnect()
            delay(1000) // Brief delay before reconnecting
            
            withTimeout(CONNECTION_TIMEOUT_MS) {
                networkClient.connectToController(controller.ipAddress, controller.port)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Reconnection attempt failed: ${e.message}")
            false
        }
    }

    private suspend fun attemptDiscoveryAndConnect(): Boolean {
        return try {
            Log.d(TAG, "Attempting discovery and connection")
            
            val controllers = withTimeout(15000) {
                networkClient.discoverControllers()
            }
            
            if (controllers.isNotEmpty()) {
                val controller = controllers.first()
                Log.d(TAG, "Found controller during recovery: ${controller.deviceName}")
                
                val connected = withTimeout(CONNECTION_TIMEOUT_MS) {
                    networkClient.connectToController(controller.ipAddress, controller.port)
                }
                
                if (connected) {
                    lastKnownGoodController = controller
                }
                
                connected
            } else {
                Log.d(TAG, "No controllers found during discovery")
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Discovery and connect attempt failed: ${e.message}")
            false
        }
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        // Exponential backoff with jitter
        val baseDelay = INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1))
        val cappedDelay = minOf(baseDelay, MAX_RETRY_DELAY_MS)
        val jitter = (Math.random() * 0.1 * cappedDelay).toLong()
        return cappedDelay + jitter
    }

    private fun isRapidFailure(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastFailureTime > RAPID_FAILURE_WINDOW_MS) {
            // Reset rapid failure count if outside the window
            rapidFailureCount.set(1)
        } else {
            rapidFailureCount.incrementAndGet()
        }
        
        lastFailureTime = currentTime
        return rapidFailureCount.get() >= RAPID_FAILURE_THRESHOLD
    }

    /**
     * Reset recovery state (useful after manual intervention)
     */
    fun resetRecoveryState() {
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
        lastFailureTime = 0L
        Log.i(TAG, "Recovery state reset")
    }

    /**
     * Get current recovery statistics
     */
    fun getRecoveryStats(): Map<String, Any> {
        return mapOf(
            "recovery_active" to isRecoveryActive.get(),
            "reconnection_attempts" to reconnectionAttempts.get(),
            "rapid_failure_count" to rapidFailureCount.get(),
            "last_failure_time" to lastFailureTime,
            "has_known_good_controller" to (lastKnownGoodController != null)
        )
    }

    /**
     * Get average network latency in milliseconds
     */
    fun getAverageLatency(): Long {
        // Simple simulation - in a real implementation, this would track actual latency
        return if (isHealthy) {
            when (successfulConnections) {
                0 -> 0L
                in 1..5 -> 50L + (Math.random() * 20).toLong()
                else -> 30L + (Math.random() * 15).toLong()
            }
        } else {
            200L + (Math.random() * 100).toLong()
        }
    }

    /**
     * Get current throughput in KB/s
     */
    fun getThroughputKBps(): Double {
        // Simple simulation - in a real implementation, this would track actual throughput
        return if (isHealthy) {
            when (successfulConnections) {
                0 -> 0.0
                in 1..5 -> 50.0 + (Math.random() * 30.0)
                else -> 80.0 + (Math.random() * 40.0)
            }
        } else {
            10.0 + (Math.random() * 20.0)
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disableAutoRecovery()
        recoveryJob.cancel()
        eventListener = null
        lastKnownGoodController = null
    }
}