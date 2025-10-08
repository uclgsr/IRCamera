// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\repository\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_repository_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\repository' subtree
// Files: 2; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\repository\ConfigRepository.kt =====

package com.mpdc4gsr.module.thermalunified.repository

import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import com.mpdc4gsr.module.thermalunified.bean.ModelBean

object ConfigRepository {
    fun read(isTC007: Boolean): ModelBean =
        try {
            // TC007 functionality removed
            Gson().fromJson(
                if (isTC007) "{}" else SharedManager.getIRConfig(), // Use empty JSON for TC007
                ModelBean::class.java
            )
        } catch (_: Exception) {
            ModelBean(DataBean(id = 0, use = true))
        }

    fun update(
        isTC007: Boolean,
        bean: ModelBean,
    ) {
        if (isTC007) {
            // TC007 functionality removed - do nothing
        } else {
            SharedManager.setIRConfig(Gson().toJson(bean))
        }
    }

    fun readConfig(isTC007: Boolean): DataBean {
        val config = read(isTC007)
        if (config.defaultModel.use) {
            return config.defaultModel
        }
        config.myselfModel.forEach {
            if (it.use) {
                return it
            }
        }
        return config.defaultModel
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\repository\ThermalDataRepository.kt =====

package com.mpdc4gsr.module.thermalunified.repository

import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ThermalDataRepository : BaseRepository() {
    data class ThermalReading(
        val timestamp: Long,
        val temperature: Float,
        val x: Int,
        val y: Int,
        val deviceId: String,
        val quality: ReadingQuality = ReadingQuality.GOOD
    )

    enum class ReadingQuality { EXCELLENT, GOOD, FAIR, POOR, INVALID }
    data class ThermalFrameData(
        val frameId: String,
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val readings: List<ThermalReading>,
        val metadata: FrameMetadata
    )

    data class FrameMetadata(
        val deviceType: String,
        val calibrationData: String,
        val ambientTemperature: Float,
        val frameRate: Int
    )

    // Real-time thermal data stream
    fun getThermalDataStream(deviceId: String): Flow<BaseRepository.Result<ThermalFrameData>> =
        flow {
            emit(BaseRepository.Result.Loading)
            try {
                while (true) {
                    delay(100) // 10 FPS simulation
                    val frame = generateThermalFrame(deviceId)
                    emit(BaseRepository.Result.Success(frame))
                }
            } catch (e: Exception) {
                emit(BaseRepository.Result.Error(e))
            }
        }.flowOn(Dispatchers.IO)

    // Historical thermal data with caching
    fun getHistoricalThermalData(
        deviceId: String,
        startTime: Long,
        endTime: Long
    ): Flow<BaseRepository.Result<List<ThermalFrameData>>> = safeFlow {
        val cacheKey = "thermal_${deviceId}_${startTime}_${endTime}"
        val ttlMs = 300000L // 5 minutes TTL
        val data = getCachedOrExecute<List<ThermalFrameData>>(cacheKey, ttlMs) {
            delay(1500) // Simulate data loading
            generateHistoricalData(deviceId, startTime, endTime)
        }
        data
    }

    private fun generateThermalFrame(deviceId: String): ThermalFrameData {
        val timestamp = System.currentTimeMillis()
        val readings = mutableListOf<ThermalReading>()
        // Generate sample thermal readings for a 32x24 array
        for (y in 0 until 24) {
            for (x in 0 until 32) {
                val baseTemp = 25.0f
                val variation = (Math.sin(x * 0.2) * Math.cos(y * 0.3) * 10).toFloat()
                val noise = (Math.random() * 2 - 1).toFloat()
                readings.add(
                    ThermalReading(
                        timestamp = timestamp,
                        temperature = baseTemp + variation + noise,
                        x = x,
                        y = y,
                        deviceId = deviceId,
                        quality = if (Math.random() < 0.95) ReadingQuality.GOOD else ReadingQuality.FAIR
                    )
                )
            }
        }
        return ThermalFrameData(
            frameId = "frame_${timestamp}",
            timestamp = timestamp,
            width = 32,
            height = 24,
            readings = readings,
            metadata = FrameMetadata(
                deviceType = "TC007",
                calibrationData = "cal_${deviceId}",
                ambientTemperature = 22.5f,
                frameRate = 10
            )
        )
    }

    private fun generateHistoricalData(
        deviceId: String,
        startTime: Long,
        endTime: Long
    ): List<ThermalFrameData> {
        val frames = mutableListOf<ThermalFrameData>()
        val interval = 1000L // 1 second intervals
        var currentTime = startTime
        while (currentTime <= endTime) {
            frames.add(generateThermalFrame(deviceId).copy(timestamp = currentTime))
            currentTime += interval
        }
        return frames
    }
}