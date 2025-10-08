package mpdc4gsr.feature.system.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.feature.system.domain.repository.SystemRepository
import javax.inject.Inject

class SystemRepositoryImpl @Inject constructor() : SystemRepository {
    override suspend fun startRecording(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun stopRecording(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun discoverControllers(): Flow<List<PCControllerInfo>> = flow {
        emit(emptyList())
    }

    override suspend fun syncClocks(controllerId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isRecording(): Flow<Boolean> = flow {
        emit(false)
    }
}
