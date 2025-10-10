package mpdc4gsr.feature.connectivity.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class NetworkErrorRecoveryManager(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
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
    private val totalBytesTransferred = AtomicLong(0)
    private val latencySum = AtomicLong(0)
    private val latencyCount = AtomicLong(0)
    private var transferStartTime = System.currentTimeMillis()

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
        if (isRecoveryActive.get()) {
            return
        }

        isRecoveryActive.set(true)
    }


    fun disableAutoRecovery() {
        if (!isRecoveryActive.get()) {
            return
        }

        isRecoveryActive.set(false)
    }


    fun recordSuccessfulConnection(controller: NetworkClient.ControllerInfo) {
        lastKnownGoodController = controller
        reconnectionAttempts.set(0)
        rapidFailureCount.set(0)
    }


    fun handleNetworkError(
        operation: String,
        error: String,
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFailureTime < RAPID_FAILURE_WINDOW_MS) {
            rapidFailureCount.incrementAndGet()
        } else {
            rapidFailureCount.set(1)
        }

        lastFailureTime = currentTime
        if (rapidFailureCount.get() >= RAPID_FAILURE_THRESHOLD) {
            eventListener?.onRapidFailureDetected(rapidFailureCount.get())
        }
    }


    fun recordDataTransfer(bytes: Long) {
        totalBytesTransferred.addAndGet(bytes)
    }


    fun recordLatency(latencyMs: Long) {
        latencySum.addAndGet(latencyMs)
        latencyCount.incrementAndGet()
    }


    fun getAverageLatency(): Long {
        val count = latencyCount.get()
            return if (count > 0) {
            latencySum.get() / count
        } else {
            0L
        }
    }


    fun getThroughputKBps(): Double {
        val elapsedTimeMs = System.currentTimeMillis() - transferStartTime
        return if (elapsedTimeMs > 0) {
            (totalBytesTransferred.get() / 1024.0) / (elapsedTimeMs / 1000.0)
        } else {
            0.0
        }
    }


    fun cleanup() {
        isRecoveryActive.set(false)
        healthCheckJob?.cancel()
        recoveryJob.cancel()
        eventListener = null
    }
}

