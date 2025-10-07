// Merged .kt under 'feature\thermal\data\repository' subtree
// Files: 1; Generated 2025-10-07 19:59:56


// ===== feature\thermal\data\repository\ThermalRepositoryImpl.kt =====

package mpdc4gsr.feature.thermal.data.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository

class ThermalRepositoryImpl(
    private val topdonDataSource: TopdonDataSource
) : ThermalRepository {
    override suspend fun connectCamera(): Result<Unit> {
        return topdonDataSource.connectDevice()
    }

    override suspend fun disconnectCamera() {
        topdonDataSource.disconnectDevice()
    }

    override suspend fun getThermalStream(): Flow<ThermalFrameData> {
        return topdonDataSource.startStreaming()
    }

    override suspend fun stopStream() {
        topdonDataSource.stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        return topdonDataSource.captureSnapshot()
    }

    override suspend fun startRecording(): Result<Unit> {
        return topdonDataSource.startRecording()
    }

    override suspend fun stopRecording(): Result<String> {
        return topdonDataSource.stopRecording()
    }

    override fun isCameraConnected(): Boolean {
        return topdonDataSource.isConnected()
    }

    override suspend fun setTemperatureRange(minTemp: Float, maxTemp: Float): Result<Unit> {
        return topdonDataSource.setTemperatureRange(minTemp, maxTemp)
    }
}


