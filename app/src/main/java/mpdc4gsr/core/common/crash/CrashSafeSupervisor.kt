package mpdc4gsr.core.common.crash

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CrashSafeSupervisor private constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "CrashSafeSupervisor"

        @Volatile
        private var instance: CrashSafeSupervisor? = null

        fun getInstance(context: Context): CrashSafeSupervisor =
            instance ?: synchronized(this) {
                instance ?: CrashSafeSupervisor(context.applicationContext).also { instance = it }
            }
    }

    private val isRunning = AtomicBoolean(false)
    private val supervisorScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default +
                    CoroutineName("CrashSafeSupervisor") +
                    CoroutineExceptionHandler { _, exception ->
                        handleSupervisorException(exception)
                    },
        )
    private val managedJobs = ConcurrentHashMap<String, ManagedJob>()
    private val healthChecks = ConcurrentHashMap<String, HealthCheck>()
    private val restartCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val maxRestartAttempts = 3
    private val restartDelayMs = 5000L
    private val healthCheckIntervalMs = 30000L
    private val logger = StructuredLogger.getInstance(context)

    data class ManagedJob(
        val id: String,
        val name: String,
        val job: Job,
        val stopToken: StopToken,
        val restartable: Boolean = true,
        val critical: Boolean = false,
        val startTime: Long = System.currentTimeMillis(),
    )

    interface HealthCheck {
        suspend fun checkHealth(): HealthStatus
    }

    data class HealthStatus(
        val isHealthy: Boolean,
        val message: String,
        val details: Map<String, Any> = emptyMap(),
    )

    class StopToken {
        private val stopped = AtomicBoolean(false)

        fun isStopRequested(): Boolean = stopped.get()

        fun requestStop() {
            stopped.set(true)
        }

        fun reset() {
            stopped.set(false)
        }
    }

    fun initialize() {
        if (isRunning.getAndSet(true)) {
            return
        }
        startHealthMonitoring()
    }

    fun registerJob(
        id: String,
        name: String,
        critical: Boolean = false,
        restartable: Boolean = true,
        healthCheck: HealthCheck? = null,
        jobFactory: suspend (StopToken) -> Unit,
    ): Job {
        if (!isRunning.get()) {
            throw IllegalStateException("Supervisor not initialized")
        }
        val stopToken = StopToken()
        val job =
            supervisorScope.launch {
                try {
                    jobFactory(stopToken)
                } catch (e: Exception) {
                    if (critical) {
                        handleCriticalJobFailure(id, name, e)
                    } else if (restartable) {
                        scheduleJobRestart(id, name, jobFactory, stopToken)
                    }
                    throw e
                }
            }
        val managedJob = ManagedJob(id, name, job, stopToken, restartable, critical)
        managedJobs[id] = managedJob
        restartCounts[id] = AtomicInteger(0)
        if (healthCheck != null) {
            healthChecks[id] = healthCheck
        }
        return job
    }

    fun unregisterJob(id: String) {
        val managedJob = managedJobs.remove(id)
        healthChecks.remove(id)
        restartCounts.remove(id)
        managedJob?.let { job ->
            job.stopToken.requestStop()
            if (!job.job.isCompleted) {
                job.job.cancel()
            }
        }
    }

    fun stopJob(id: String) {
        val managedJob = managedJobs[id]
        if (managedJob != null) {
            managedJob.stopToken.requestStop()
        }
    }

    fun getJobStatuses(): Map<String, JobStatus> =
        managedJobs.mapValues { (_, managedJob) ->
            JobStatus(
                id = managedJob.id,
                name = managedJob.name,
                isActive = managedJob.job.isActive,
                isCompleted = managedJob.job.isCompleted,
                isCancelled = managedJob.job.isCancelled,
                critical = managedJob.critical,
                restartable = managedJob.restartable,
                restartCount = restartCounts[managedJob.id]?.get() ?: 0,
                startTime = managedJob.startTime,
                upTimeSeconds = (System.currentTimeMillis() - managedJob.startTime) / 1000,
            )
        }

    data class JobStatus(
        val id: String,
        val name: String,
        val isActive: Boolean,
        val isCompleted: Boolean,
        val isCancelled: Boolean,
        val critical: Boolean,
        val restartable: Boolean,
        val restartCount: Int,
        val startTime: Long,
        val upTimeSeconds: Long,
    )

    private fun handleSupervisorException(exception: Throwable) {
    }

    private fun handleCriticalJobFailure(
        id: String,
        name: String,
        exception: Exception,
    ) {
    }

    private fun scheduleJobRestart(
        id: String,
        name: String,
        jobFactory: suspend (StopToken) -> Unit,
        originalStopToken: StopToken,
    ) {
        val restartCount = restartCounts[id]?.incrementAndGet() ?: 1
        if (restartCount > maxRestartAttempts) {
            return
        }
        supervisorScope.launch {
            delay(restartDelayMs)
            if (originalStopToken.isStopRequested()) {
                return@launch
            }
            try {
                val newStopToken = StopToken()
                val newJob =
                    supervisorScope.launch {
                        jobFactory(newStopToken)
                    }
                managedJobs[id]?.let { oldManagedJob ->
                    val updatedJob =
                        oldManagedJob.copy(
                            job = newJob,
                            stopToken = newStopToken,
                            startTime = System.currentTimeMillis(),
                        )
                    managedJobs[id] = updatedJob
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun startHealthMonitoring() {
        supervisorScope.launch {
            while (isRunning.get()) {
                try {
                    performHealthChecks()
                } catch (e: Exception) {
                }
                delay(healthCheckIntervalMs)
            }
        }
    }

    private suspend fun performHealthChecks() {
        healthChecks.forEach { (jobId, healthCheck) ->
            try {
                val status = healthCheck.checkHealth()
                if (!status.isHealthy) {
                    handleUnhealthyJob(jobId, status)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun handleUnhealthyJob(
        jobId: String,
        status: HealthStatus,
    ) {
        val managedJob = managedJobs[jobId]
        if (managedJob != null && managedJob.restartable) {
            managedJob.job.cancel("Health check failed: ${status.message}")
        }
    }

    fun shutdown() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        managedJobs.values.forEach { managedJob ->
            managedJob.stopToken.requestStop()
        }
        supervisorScope.cancel()
        // Note: supervisorScope is cancelled, jobs will cleanup asynchronously
        // No blocking wait needed - this prevents ANR
        managedJobs.clear()
        healthChecks.clear()
        restartCounts.clear()
    }
}

