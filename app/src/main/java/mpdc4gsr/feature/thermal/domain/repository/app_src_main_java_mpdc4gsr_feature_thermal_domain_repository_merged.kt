// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\thermal\domain\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:33


// ===== FROM: app\src\main\java\mpdc4gsr\feature\thermal\domain\repository\ThermalRepository.kt =====

package mpdc4gsr.feature.thermal.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot

interface ThermalRepository {

    suspend fun connectCamera(): Result<Unit>

    suspend fun disconnectCamera()

    suspend fun getThermalStream(): Flow<ThermalFrameData>

    suspend fun stopStream()

    suspend fun captureSnapshot(): Result<ThermalSnapshot>

    suspend fun startRecording(): Result<Unit>

    suspend fun stopRecording(): Result<String>

    fun isCameraConnected(): Boolean

    suspend fun setTemperatureRange(minTemp: Float, maxTemp: Float): Result<Unit>
}