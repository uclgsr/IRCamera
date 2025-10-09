package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import mpdc4gsr.core.session.SessionInfo
import mpdc4gsr.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SessionManagerViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val sessionDirectoryManager: SessionDirectoryManager
) : AppBaseViewModel() {
    // StateFlow for session management
    private val _allSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    private val _filteredSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val filteredSessions: StateFlow<List<SessionInfo>> = _filteredSessions.asStateFlow()
    private val _storageInfo = MutableStateFlow(StorageInfo("0 MB", 0, false))
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // SharedFlow for one-time events
    private val _sessionEvents = MutableSharedFlow<SessionEvent>()
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    // UI State
    private val _sessionUiState = MutableStateFlow(SessionManagerUiState())
    val sessionUiState: StateFlow<SessionManagerUiState> = _sessionUiState.asStateFlow()
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class SessionManagerUiState(
        val isLoading: Boolean = false,
        val sessionCount: Int = 0,
        val filteredCount: Int = 0,
        val currentFilter: FilterType = FilterType.ALL,
        val searchQuery: String = ""
    )

    data class StorageInfo(
        val formattedAvailable: String,
        val usagePercentage: Int,
        val isLowStorage: Boolean
    )

    sealed class SessionEvent {
        data class OpenDetails(val session: SessionInfo) : SessionEvent()
        data class DeleteConfirm(val session: SessionInfo) : SessionEvent()
        data class Export(val session: SessionInfo) : SessionEvent()
        data class DeletedSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportFailed(val session: SessionInfo, val message: String) : SessionEvent()
        data class ShowError(val message: String) : SessionEvent()
        data class ShowToast(val message: String) : SessionEvent()
    }

    enum class FilterType {
        ALL, RECENT, COMPLETED, WITH_DATA
    }

    fun loadSessions(context: Context) {
        _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
        launchWithErrorHandling {
            try {
                // Display storage info
                updateStorageInfo()
                // Clean up failed sessions
                val cleanedSessions = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.cleanupFailedSessions()
                }
                if (cleanedSessions.isNotEmpty()) {
                    _sessionEvents.emit(SessionEvent.ShowToast("Cleaned up ${cleanedSessions.size} failed sessions"))
                }
                // Load sessions
                val loadedSessions = withContext(Dispatchers.IO) {
                    val activeSessions = sessionManager.getActiveSessions()
                    val historicalSessions = loadHistoricalSessions(context)
                    (activeSessions + historicalSessions).distinctBy { it.sessionId }
                }
                val sortedSessions = loadedSessions.sortedByDescending { it.startTime }
                _allSessions.value = sortedSessions
                applyCurrentFilters()
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    sessionCount = sortedSessions.size
                )
            } catch (e: Exception) {
                _sessionEvents.emit(SessionEvent.ShowError("Failed to load sessions: ${e.message}"))
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun updateStorageInfo() {
        try {
            val storageStatus = sessionDirectoryManager.checkStorageSpace()
            _storageInfo.value = StorageInfo(
                formattedAvailable = storageStatus.formattedAvailable,
                usagePercentage = storageStatus.usagePercentage,
                isLowStorage = storageStatus.isLowStorage
            )
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("SessionManagerViewModel", "Unexpected Exception in SessionManagerViewModel catch block", e)
        }
    }

    private suspend fun loadHistoricalSessions(context: Context): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            val historicalSessions = mutableListOf<SessionInfo>()
            try {
                val baseDir = File(context.getExternalFilesDir(null), "recordings")
                if (baseDir.exists() && baseDir.isDirectory) {
                    baseDir.listFiles()?.forEach { sessionDir ->
                        if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
                            try {
                                val sessionInfo = parseSessionFromDirectory(sessionDir)
                                historicalSessions.add(sessionInfo)
                            } catch (e: Exception) {
                                mpdc4gsr.core.utils.AppLogger.e("SessionManagerViewModel", "Unexpected Exception in SessionManagerViewModel catch block", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("SessionManagerViewModel", "Unexpected Exception in SessionManagerViewModel catch block", e)
            }
            historicalSessions
        }
    }

    private fun parseSessionFromDirectory(sessionDir: File): SessionInfo {
        val sessionId = sessionDir.name
        val metadataFile = File(sessionDir, "session_metadata.txt")
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            startTime = sessionDir.lastModified(),
        )
        if (metadataFile.exists()) {
            try {
                metadataFile.readLines().forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val value = parts[1]
                        when (key.trim()) {
                            "participantId" -> sessionInfo.participantId = value.trim()
                            "studyName" -> sessionInfo.studyName = value.trim()
                            "endTime" -> sessionInfo.endTime = value.trim().toLongOrNull()
                            "sampleCount" -> sessionInfo.sampleCount =
                                value.trim().toLongOrNull() ?: 0

                            else -> sessionInfo.metadata[key.trim()] = value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("SessionManagerViewModel", "Unexpected Exception in SessionManagerViewModel catch block", e)
            }
        }
        // Calculate data file counts and sizes
        calculateSessionDataInfo(sessionDir, sessionInfo)
        return sessionInfo
    }

    private fun calculateSessionDataInfo(sessionDir: File, sessionInfo: SessionInfo) {
        try {
            var totalSize = 0L
            var gsrFileCount = 0
            var thermalFileCount = 0
            var rgbFileCount = 0
            sessionDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                    when {
                        file.name.contains("gsr") -> gsrFileCount++
                        file.name.contains("thermal") -> thermalFileCount++
                        file.name.contains("rgb") -> rgbFileCount++
                    }
                }
            }
            sessionInfo.totalDataSize = totalSize
            sessionInfo.metadata["gsrFileCount"] = gsrFileCount.toString()
            sessionInfo.metadata["thermalFileCount"] = thermalFileCount.toString()
            sessionInfo.metadata["rgbFileCount"] = rgbFileCount.toString()
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("SessionManagerViewModel", "Unexpected Exception in SessionManagerViewModel catch block", e)
        }
    }

    fun filterSessions(query: String?) {
        currentSearchQuery = query ?: ""
        _sessionUiState.value = _sessionUiState.value.copy(searchQuery = currentSearchQuery)
        applyCurrentFilters()
    }

    fun filterSessionsByType(filterPosition: Int) {
        currentFilter = when (filterPosition) {
            0 -> FilterType.ALL
            1 -> FilterType.RECENT
            2 -> FilterType.COMPLETED
            3 -> FilterType.WITH_DATA
            else -> FilterType.ALL
        }
        _sessionUiState.value = _sessionUiState.value.copy(currentFilter = currentFilter)
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val allSessions = _allSessions.value
        var filtered = allSessions
        // Apply type filter
        filtered = when (currentFilter) {
            FilterType.ALL -> filtered
            FilterType.RECENT -> filtered.filter {
                System.currentTimeMillis() - it.startTime < 24 * 60 * 60 * 1000 // Last 24 hours
            }

            FilterType.COMPLETED -> filtered.filter { it.endTime != null }
            FilterType.WITH_DATA -> filtered.filter { it.totalDataSize > 0 }
        }
        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { session ->
                session.sessionId.contains(currentSearchQuery, ignoreCase = true) ||
                        session.participantId?.contains(
                            currentSearchQuery,
                            ignoreCase = true
                        ) == true ||
                        session.studyName?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }
        _filteredSessions.value = filtered
        _sessionUiState.value = _sessionUiState.value.copy(filteredCount = filtered.size)
    }

    fun onSessionClick(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.OpenDetails(session))
        }
    }

    fun onSessionDelete(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.DeleteConfirm(session))
        }
    }

    fun onSessionExport(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.Export(session))
        }
    }

    fun deleteSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.deleteSession(session.sessionId)
            }
            if (success) {
                // Remove from local list and update UI
                val updatedSessions =
                    _allSessions.value.filter { it.sessionId != session.sessionId }
                _allSessions.value = updatedSessions
                applyCurrentFilters()
                _sessionUiState.value =
                    _sessionUiState.value.copy(sessionCount = updatedSessions.size)
                _sessionEvents.emit(
                    SessionEvent.DeletedSuccess(
                        session,
                        "Session ${session.sessionId} deleted successfully"
                    )
                )
            } else {
                _sessionEvents.emit(SessionEvent.ShowError("Failed to delete session ${session.sessionId}"))
            }
        }
    }

    fun exportSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.exportSession(session.sessionId)
            }
            if (success) {
                _sessionEvents.emit(
                    SessionEvent.ExportSuccess(
                        session,
                        "Session ${session.sessionId} exported successfully"
                    )
                )
            } else {
                _sessionEvents.emit(
                    SessionEvent.ExportFailed(
                        session,
                        "Failed to export session ${session.sessionId}"
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "SessionManagerViewModel"
    }
}
