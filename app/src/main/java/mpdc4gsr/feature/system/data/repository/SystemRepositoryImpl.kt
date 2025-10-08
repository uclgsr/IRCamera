package mpdc4gsr.feature.system.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.feature.system.service.RecordingService
import mpdc4gsr.core.data.TimeSyncManager
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.feature.network.data.PcServerDiscovery
import mpdc4gsr.feature.system.domain.repository.SystemRepository

class SystemRepositoryImpl(
    private val context: Context,
    private val recordingService: RecordingService? = null,
    private val timeSyncManager: TimeSyncManager? = null,
    private val pcServerDiscovery: PcServerDiscovery? = null
) : SystemRepository {

    private var isRecordingState = false
    private var currentSessionMetadata: SessionMetadata? = null

    override suspend fun startRecording(
        sessionId: String,
        participantId: String?,
        studyName: String?
    ): Result<Boolean> {
        isRecordingState = true
        return Result.success(true)
    }

    override suspend fun stopRecording(): Result<Map<String, Boolean>> {
        isRecordingState = false
        val results = mapOf(
            "gsr" to true,
            "thermal" to true,
            "rgb" to true
        )
        return Result.success(results)
    }

    override suspend fun discoverControllers(): Flow<List<PCControllerInfo>> = flow {
        emit(emptyList())
    }

    override suspend fun syncClocks(): Result<Long> {
        return Result.success(0L)
    }

    override fun isRecording(): Boolean {
        return isRecordingState
    }

    override suspend fun getSessionMetadata(): SessionMetadata? {
        return currentSessionMetadata
    }

    override suspend fun pauseRecording(): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun resumeRecording(): Result<Boolean> {
        return Result.success(true)
    }
}
