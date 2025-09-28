package mpdc4gsr.sensors.gsr

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.service.SessionManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.utils.SessionDirectoryManager
import java.io.File

class SessionManagerViewModel : BaseViewModel() {

    private val _sessions = MutableLiveData<List<SessionInfo>>()
    val sessions: LiveData<List<SessionInfo>> = _sessions

    private val _filteredSessions = MutableLiveData<List<SessionInfo>>()
    val filteredSessions: LiveData<List<SessionInfo>> = _filteredSessions

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo

    private val _sessionAction = MutableLiveData<SessionAction?>()
    val sessionAction: LiveData<SessionAction?> = _sessionAction

    private lateinit var sessionManager: SessionManager
    private lateinit var sessionDirectoryManager: SessionDirectoryManager
    private var allSessions = listOf<SessionInfo>()
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class StorageInfo(
        val formattedAvailable: String,
        val usagePercentage: Int,
        val isLowStorage: Boolean
    )

    data class SessionAction(
        val type: ActionType,
        val session: SessionInfo,
        val message: String? = null
    )

    enum class ActionType {
        OPEN_DETAILS,
        DELETE_CONFIRM,
        EXPORT,
        DELETED_SUCCESS,
        EXPORT_SUCCESS,
        EXPORT_FAILED
    }

    enum class FilterType {
        ALL, RECENT, COMPLETED, WITH_DATA
    }

    fun initialize(context: Context) {
        sessionManager = SessionManager.getInstance(context)
        sessionDirectoryManager = SessionDirectoryManager(context)
    }

    fun loadSessions(context: Context) {
        if (!::sessionManager.isInitialized) {
            initialize(context)
        }

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Display storage info
                updateStorageInfo()

                // Clean up failed sessions
                val cleanedSessions = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.cleanupFailedSessions()
                }

                if (cleanedSessions.isNotEmpty()) {
                    Log.i(TAG, "Cleaned up ${cleanedSessions.size} failed sessions")
                    _sessionAction.value = SessionAction(
                        type = ActionType.DELETED_SUCCESS,
                        session = SessionInfo("", 0), // Dummy session for cleanup message
                        message = "Cleaned up ${cleanedSessions.size} failed sessions"
                    )
                }

                // Load sessions
                val loadedSessions = withContext(Dispatchers.IO) {
                    val activeSessions = sessionManager.getActiveSessions()
                    val historicalSessions = loadHistoricalSessions(context)
                    (activeSessions + historicalSessions).distinctBy { it.sessionId }
                }

                allSessions = loadedSessions.sortedByDescending { it.startTime }
                _sessions.value = allSessions
                applyCurrentFilters()

                Log.i(TAG, "Loaded ${allSessions.size} sessions")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sessions", e)
                _error.value = "Failed to load sessions: ${e.message}"
            } finally {
                _loading.value = false
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
            Log.e(TAG, "Failed to get storage info", e)
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
                                Log.w(TAG, "Failed to parse session from ${sessionDir.name}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load historical sessions", e)
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
                Log.w(TAG, "Failed to parse metadata for ${sessionInfo.sessionId}", e)
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
            Log.w(TAG, "Failed to calculate data info for ${sessionInfo.sessionId}", e)
        }
    }

    fun filterSessions(query: String?) {
        currentSearchQuery = query ?: ""
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
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
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
    }

    fun onSessionClick(session: SessionInfo) {
        _sessionAction.value = SessionAction(
            type = ActionType.OPEN_DETAILS,
            session = session
        )
    }

    fun onSessionDelete(session: SessionInfo) {
        _sessionAction.value = SessionAction(
            type = ActionType.DELETE_CONFIRM,
            session = session
        )
    }

    fun onSessionExport(session: SessionInfo) {
        _sessionAction.value = SessionAction(
            type = ActionType.EXPORT,
            session = session
        )
    }

    fun deleteSession(session: SessionInfo) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.deleteSession(session.sessionId)
                }

                if (success) {
                    // Remove from local list and update UI
                    allSessions = allSessions.filter { it.sessionId != session.sessionId }
                    _sessions.value = allSessions
                    applyCurrentFilters()

                    _sessionAction.value = SessionAction(
                        type = ActionType.DELETED_SUCCESS,
                        session = session,
                        message = "Session ${session.sessionId} deleted successfully"
                    )
                } else {
                    _error.value = "Failed to delete session ${session.sessionId}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session", e)
                _error.value = "Failed to delete session: ${e.message}"
            }
        }
    }

    fun exportSession(session: SessionInfo) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.exportSession(session.sessionId)
                }

                _sessionAction.value = if (success) {
                    SessionAction(
                        type = ActionType.EXPORT_SUCCESS,
                        session = session,
                        message = "Session ${session.sessionId} exported successfully"
                    )
                } else {
                    SessionAction(
                        type = ActionType.EXPORT_FAILED,
                        session = session,
                        message = "Failed to export session ${session.sessionId}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export session", e)
                _sessionAction.value = SessionAction(
                    type = ActionType.EXPORT_FAILED,
                    session = session,
                    message = "Failed to export session: ${e.message}"
                )
            }
        }
    }

    fun clearSessionAction() {
        _sessionAction.value = null
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "SessionManagerViewModel"
    }
}