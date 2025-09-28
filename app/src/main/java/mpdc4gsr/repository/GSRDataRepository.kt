package mpdc4gsr.repository

import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay

/**
 * GSR Data Repository - Complete Repository Pattern Implementation
 * Manages GSR sensor data with real-time streaming, caching, and historical data
 */
class GSRDataRepository : BaseRepository() {
    
    data class GSRReading(
        val timestamp: Long,
        val conductance: Float, // microsiemens
        val resistance: Float,  // kiloohms
        val deviceId: String,
        val sessionId: String?,
        val quality: SignalQuality = SignalQuality.GOOD
    )
    
    enum class SignalQuality { EXCELLENT, GOOD, FAIR, POOR, DISCONNECTED }
    
    data class GSRSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long?,
        val deviceId: String,
        val participantId: String?,
        val readingCount: Int,
        val avgConductance: Float,
        val status: SessionStatus
    )
    
    enum class SessionStatus { ACTIVE, PAUSED, COMPLETED, CANCELLED }
    
    // Real-time GSR data stream
    fun getGSRDataStream(deviceId: String): Flow<Result<GSRReading>> = safeFlow {
        var counter = 0
        while (true) {
            delay(100) // 10Hz sampling rate
            
            val reading = generateGSRReading(deviceId, counter++)
            emit(Result.success(reading))
        }
    }
    
    // Historical GSR data with advanced caching
    fun getHistoricalGSRData(
        sessionId: String,
        startTime: Long,
        endTime: Long
    ): Flow<Result<List<GSRReading>>> = safeFlow {
        
        val cacheKey = "gsr_${sessionId}_${startTime}_${endTime}"
        val cached = getFromCache<List<GSRReading>>(cacheKey)
        
        if (cached != null) {
            return@safeFlow Result.success(cached)
        }
        
        // Simulate database query
        delay(2000)
        
        val historicalData = generateHistoricalGSRData(sessionId, startTime, endTime)
        
        // Cache with longer TTL for historical data
        putInCache(cacheKey, historicalData, ttlMs = 600000) // 10 minutes
        
        Result.success(historicalData)
    }
    
    // Session management
    fun getGSRSessions(deviceId: String): Flow<Result<List<GSRSession>>> = safeFlow {
        val cacheKey = "sessions_$deviceId"
        val cached = getFromCache<List<GSRSession>>(cacheKey)
        
        if (cached != null) {
            return@safeFlow Result.success(cached)
        }
        
        delay(1000)
        
        val sessions = generateSampleSessions(deviceId)
        putInCache(cacheKey, sessions, ttlMs = 120000) // 2 minutes
        
        Result.success(sessions)
    }
    
    private fun generateGSRReading(deviceId: String, counter: Int): GSRReading {
        val baselineResistance = 50.0f // kiloohms
        val variation = (Math.sin(counter * 0.01) * 10 + Math.random() * 5).toFloat()
        val resistance = (baselineResistance + variation).coerceAtLeast(1.0f)
        val conductance = 1000.0f / resistance // Convert to microsiemens
        
        return GSRReading(
            timestamp = System.currentTimeMillis(),
            conductance = conductance,
            resistance = resistance,
            deviceId = deviceId,
            sessionId = "session_${System.currentTimeMillis() / 100000}",
            quality = if (Math.random() < 0.9) SignalQuality.GOOD else SignalQuality.FAIR
        )
    }
    
    private fun generateHistoricalGSRData(
        sessionId: String,
        startTime: Long,
        endTime: Long
    ): List<GSRReading> {
        val readings = mutableListOf<GSRReading>()
        val interval = 100L // 100ms intervals (10Hz)
        
        var currentTime = startTime
        var counter = 0
        
        while (currentTime <= endTime) {
            readings.add(
                generateGSRReading("device_001", counter++).copy(
                    timestamp = currentTime,
                    sessionId = sessionId
                )
            )
            currentTime += interval
        }
        
        return readings
    }
    
    private fun generateSampleSessions(deviceId: String): List<GSRSession> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            GSRSession(
                sessionId = "session_001",
                startTime = currentTime - 3600000, // 1 hour ago
                endTime = currentTime - 3000000,   // 50 minutes ago
                deviceId = deviceId,
                participantId = "participant_001",
                readingCount = 600,
                avgConductance = 15.5f,
                status = SessionStatus.COMPLETED
            ),
            GSRSession(
                sessionId = "session_002",
                startTime = currentTime - 1800000, // 30 minutes ago
                endTime = null,
                deviceId = deviceId,
                participantId = "participant_002",
                readingCount = 300,
                avgConductance = 18.2f,
                status = SessionStatus.ACTIVE
            )
        )
    }
}