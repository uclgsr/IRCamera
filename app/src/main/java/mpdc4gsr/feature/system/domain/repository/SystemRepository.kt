package mpdc4gsr.feature.system.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.PCControllerInfo

interface SystemRepository {
    suspend fun startRecording(): Result<Unit>
    suspend fun stopRecording(): Result<Unit>
    suspend fun discoverControllers(): Flow<List<PCControllerInfo>>
    suspend fun syncClocks(controllerId: String): Result<Unit>
    fun isRecording(): Flow<Boolean>
}
