package com.mpdc4gsr.gsr.network

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NetworkErrorRecoveryManager(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "NetworkErrorRecovery"
        private const val MAX_RECONNECTION_ATTEMPTS = 10
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val HEALTH_CHECK_INTERVAL_MS = 15000L
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val RAPID_FAILURE_THRESHOLD = 3
        private const val RAPID_FAILURE_WINDOW_MS = 60000L
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

        fun onRecoveryAttempt(
            attempt: Int,
            maxAttempts: Int,
        )

        fun onRecoverySuccess(controller: NetworkClient.ControllerInfo)

        fun onRecoveryFailed(reason: String)

        fun onConnectionHealthChanged(isHealthy: Boolean)

        fun onRapidFailureDetected(failureCount: Int)
    }

    private var eventListener: RecoveryEventListener? = null

    fun setEventListener(listener: RecoveryEventListener?) {
        eventListener = listener
    }

    fun enableAutoRecovery() {
        if (isRecoveryActive.get()) {            return
        }

        isRecoveryActive.set(true)
        startHealthMonitoring()    }

    fun disableAutoRecovery() {
        if (!isRecoveryActive.get()) {            return
        }

        isRecoveryActive.set(false)
        stopHealthMonitoring()    }

    suspend fun triggerRecovery(reason: String): Boolean {
        if (isRecoveryActive.get() && reconnectionAttempts.get() > 0) {            return false
        }

        return performRecovery(reason)
    }

    fun recordSuccessfulConnection(controller: NetworkClient.ControllerInfo) {
        lastKnownGoodController = controller
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)    }

    fun handleNetworkError(
        operation: String,
        error: String,
    ) {        if (isRapidFailure()) {
            eventListener?.onRapidFailureDetected(rapidFailureCount.get())

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
        healthCheckJob =
            recoveryScope.launch {
                while (isRecoveryActive.get() && isActive) {
                    try {
                        val isHealthy = performHealthCheck()
                        eventListener?.onConnectionHealthChanged(isHealthy)

                        if (!isHealthy && isRecoveryActive.get()) {
                            performRecovery("Health check failed")
                        }

                        delay(HEALTH_CHECK_INTERVAL_MS)
                    } catch (e: Exception) {
                        if (isActive) {                            delay(HEALTH_CHECK_INTERVAL_MS)
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

            val pingMessage =
                org.json.JSONObject().apply {
                    put("message_type", "ping")
                    put("timestamp", System.currentTimeMillis())
                }


            withTimeout(5000) {
                networkClient.sendMeasurementData("health_check", pingMessage)
            }
            true
        } catch (e: Exception) {            false
        }
    }

    private suspend fun performRecovery(reason: String): Boolean {
        if (reconnectionAttempts.get() >= MAX_RECONNECTION_ATTEMPTS) {            eventListener?.onRecoveryFailed("Maximum attempts reached")
            return false
        }        eventListener?.onRecoveryStarted(reason)

        var success = false
        val maxAttempts = MAX_RECONNECTION_ATTEMPTS

        while (reconnectionAttempts.get() < maxAttempts && isRecoveryActive.get()) {
            val attempt = reconnectionAttempts.incrementAndGet()            eventListener?.onRecoveryAttempt(attempt, maxAttempts)

            try {

                val controller = lastKnownGoodController
                if (controller != null) {
                    success = attemptReconnection(controller)
                } else {

                    success = attemptDiscoveryAndConnect()
                }

                if (success) {                    eventListener?.onRecoverySuccess(
                        lastKnownGoodController
                            ?: NetworkClient.ControllerInfo("unknown", 0, "Recovered", emptyList()),
                    )
                    reconnectionAttempts.set(0)
                    break
                } else {
                    val delay = calculateRetryDelay(attempt)                    delay(delay)
                }
            } catch (e: Exception) {                val delay = calculateRetryDelay(attempt)
                delay(delay)
            }
        }

        if (!success) {            eventListener?.onRecoveryFailed("All attempts exhausted")
        }

        return success
    }

    private suspend fun attemptReconnection(controller: NetworkClient.ControllerInfo): Boolean {
        return try {            networkClient.disconnect()
            delay(1000)

            withTimeout(CONNECTION_TIMEOUT_MS) {
                networkClient.connectToController(controller.ipAddress, controller.port)
            }
        } catch (e: Exception) {            false
        }
    }

    private suspend fun attemptDiscoveryAndConnect(): Boolean {
        return try {            val controllers =
                withTimeout(15000) {
                    networkClient.discoverControllers()
                }

            if (controllers.isNotEmpty()) {
                val controller = controllers.first()                val connected =
                    withTimeout(CONNECTION_TIMEOUT_MS) {
                        networkClient.connectToController(controller.ipAddress, controller.port)
                    }

                if (connected) {
                    lastKnownGoodController = controller
                }

                connected
            } else {                false
            }
        } catch (e: Exception) {            false
        }
    }

    private fun calculateRetryDelay(attempt: Int): Long {

        val baseDelay = INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1))
        val cappedDelay = minOf(baseDelay, MAX_RETRY_DELAY_MS)
        val jitter = (Math.random() * 0.1 * cappedDelay).toLong()
        return cappedDelay + jitter
    }

    private fun isRapidFailure(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFailureTime > RAPID_FAILURE_WINDOW_MS) {

            rapidFailureCount.set(1)
        } else {
            rapidFailureCount.incrementAndGet()
        }

        lastFailureTime = currentTime
        return rapidFailureCount.get() >= RAPID_FAILURE_THRESHOLD
    }

    fun resetRecoveryState() {
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
        lastFailureTime = 0L    }

    fun getRecoveryStats(): Map<String, Any> {
        return mapOf(
            "recovery_active" to isRecoveryActive.get(),
            "reconnection_attempts" to reconnectionAttempts.get(),
            "rapid_failure_count" to rapidFailureCount.get(),
            "last_failure_time" to lastFailureTime,
            "has_known_good_controller" to (lastKnownGoodController != null),
        )
    }

    fun cleanup() {
        disableAutoRecovery()
        recoveryJob.cancel()
        eventListener = null
        lastKnownGoodController = null
    }
}
