package mpdc4gsr.feature.system.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.core.data.model.PCControllerInfo

interface SystemRepository {

    suspend fun startRecording(
        sessionId: String,
        participantId: String?,
        studyName: String?
    ): Result<Boolean>

    suspend fun stopRecording(): Result<Map<String, Boolean>>

    suspend fun discoverControllers(): Flow<List<PCControllerInfo>>

    suspend fun syncClocks(): Result<Long>

    fun isRecording(): Boolean

    suspend fun getSessionMetadata(): SessionMetadata?

    suspend fun pauseRecording(): Result<Boolean>

    suspend fun resumeRecording(): Result<Boolean>
}
