package mpdc4gsr.core.sensors.gsr.data

import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GsrDataRepository : BaseRepository() {
    data class GsrReading(
        val timestamp: Long,
        val conductance: Float, // microsiemens
        val resistance: Float, // kiloohms
        val deviceId: String,
        val sessionId: String?,
        val quality: SignalQuality = SignalQuality.GOOD,
    )

    enum class SignalQuality { EXCELLENT, GOOD, FAIR, POOR, DISCONNECTED }

    data class GsrSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long?,
        val deviceId: String,
        val participantId: String?,
        val readingCount: Int,
        val avgConductance: Float,
        val status: SessionStatus,
    )

    enum class SessionStatus { ACTIVE, PAUSED, COMPLETED, CANCELLED }

    // Real-time GSR data stream
    fun getDataStream(deviceId: String): Flow<BaseRepository.Result<GsrReading>> =
        flow {
            emit(BaseRepository.Result.Loading)
            try {
                var counter = 0
                while (true) {
                    delay(100) // 10Hz sampling rate
                    val reading = generateReading(deviceId, counter++)
                    emit(BaseRepository.Result.Success(reading))
                }
            } catch (e: Exception) {
                emit(BaseRepository.Result.Error(e))
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    // Historical GSR data with advanced caching
    fun getHistoricalData(
        sessionId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<BaseRepository.Result<List<GsrReading>>> =
        safeFlow {
            val cacheKey = "gsr_${sessionId}_${startTime}_$endTime"
            val ttlMs = 600_000L // 10 minutes
            val data =
                getCachedOrExecute(
                    cacheKey = cacheKey,
                    ttlMs = ttlMs,
                ) {
                    // Simulate database query
                    delay(2000)
                    generateHistoricalData(sessionId, startTime, endTime)
                }
            data
        }

    // Session management
    fun getSessions(deviceId: String): Flow<BaseRepository.Result<List<GsrSession>>> =
        safeFlow {
            val cacheKey = "sessions_$deviceId"
            val ttlMs = 120_000L // 2 minutes
            val data =
                getCachedOrExecute(
                    cacheKey = cacheKey,
                    ttlMs = ttlMs,
                ) {
                    delay(1000)
                    generateSampleSessions(deviceId)
                }
            data
        }

    private fun generateReading(
        deviceId: String,
        counter: Int,
    ): GsrReading {
        val baselineResistance = 50.0f // kiloohms
        val variation = (Math.sin(counter * 0.01) * 10 + Math.random() * 5).toFloat()
        val resistance = (baselineResistance + variation).coerceAtLeast(1.0f)
        val conductance = 1000.0f / resistance // Convert to microsiemens
        return GsrReading(
            timestamp = System.currentTimeMillis(),
            conductance = conductance,
            resistance = resistance,
            deviceId = deviceId,
            sessionId = "session_${System.currentTimeMillis() / 100000}",
            quality = if (Math.random() < 0.9) SignalQuality.GOOD else SignalQuality.FAIR,
        )
    }

    private fun generateHistoricalData(
        sessionId: String,
        startTime: Long,
        endTime: Long,
    ): List<GsrReading> {
        val readings = mutableListOf<GsrReading>()
        val interval = 100L // 100ms intervals (10Hz)
        var currentTime = startTime
        var counter = 0
        while (currentTime <= endTime) {
            readings.add(
                generateReading("device_001", counter++).copy(
                    timestamp = currentTime,
                    sessionId = sessionId,
                ),
            )
            currentTime += interval
        }
        return readings
    }

    private fun generateSampleSessions(deviceId: String): List<GsrSession> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            GsrSession(
                sessionId = "session_001",
                startTime = currentTime - 3600000, // 1 hour ago
                endTime = currentTime - 3000000, // 50 minutes ago
                deviceId = deviceId,
                participantId = "participant_001",
                readingCount = 600,
                avgConductance = 15.5f,
                status = SessionStatus.COMPLETED,
            ),
            GsrSession(
                sessionId = "session_002",
                startTime = currentTime - 1800000, // 30 minutes ago
                endTime = null,
                deviceId = deviceId,
                participantId = "participant_002",
                readingCount = 300,
                avgConductance = 18.2f,
                status = SessionStatus.ACTIVE,
            ),
        )
    }
}
