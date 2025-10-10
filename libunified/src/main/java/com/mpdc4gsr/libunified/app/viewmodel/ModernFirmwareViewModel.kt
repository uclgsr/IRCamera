package com.mpdc4gsr.libunified.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.repository.FirmwareRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ModernFirmwareViewModel(
    application: Application,
    private val firmwareRepository: FirmwareRepository = FirmwareRepository(application),
) : AndroidViewModel(application) {
    // State management
    private val _firmwareState = MutableStateFlow<FirmwareState>(FirmwareState.Idle)
    val firmwareState: StateFlow<FirmwareState> = _firmwareState.asStateFlow()
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // One-time events
    private val _events = MutableSharedFlow<FirmwareEvent>()
    val events: SharedFlow<FirmwareEvent> = _events.asSharedFlow()

    // Sealed classes for type-safe state management
    sealed class FirmwareState {
        object Idle : FirmwareState()

        object Checking : FirmwareState()

        data class UpdateAvailable(
            val info: FirmwareRepository.FirmwareInfo,
        ) : FirmwareState()

        object UpToDate : FirmwareState()

        data class Error(
            val message: String,
            val isBindError: Boolean = false,
        ) : FirmwareState()
    }

    sealed class DownloadState {
        object Idle : DownloadState()

        data class Downloading(
            val progress: Float,
        ) : DownloadState()

        data class Completed(
            val file: File,
        ) : DownloadState()

        data class Error(
            val message: String,
        ) : DownloadState()
    }

    sealed class FirmwareEvent {
        data class ShowUpdateDialog(
            val info: FirmwareRepository.FirmwareInfo,
        ) : FirmwareEvent()

        data class ShowError(
            val message: String,
        ) : FirmwareEvent()

        data class ShowSuccess(
            val message: String,
        ) : FirmwareEvent()

        object UpdateCompleted : FirmwareEvent()
    }

    // Public API for checking firmware updates
    fun checkFirmwareUpdate(
        isTC007: Boolean,
        deviceInfo: FirmwareRepository.DeviceInfo,
    ) {
        viewModelScope.launch {
            _firmwareState.value = FirmwareState.Checking
            firmwareRepository.checkFirmwareUpdate(isTC007, deviceInfo).collect { result ->
                when (result) {
                    is BaseRepository.Result.Loading -> {
                        _firmwareState.value = FirmwareState.Checking
                    }

                    is BaseRepository.Result.Success -> {
                        val firmwareInfo = result.data
                        if (firmwareInfo != null && firmwareInfo.isUpdateAvailable) {
                            _firmwareState.value = FirmwareState.UpdateAvailable(firmwareInfo)
                            _events.emit(FirmwareEvent.ShowUpdateDialog(firmwareInfo))
                        } else {
                            _firmwareState.value = FirmwareState.UpToDate
                            _events.emit(FirmwareEvent.ShowSuccess("Firmware is up to date"))
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _firmwareState.value = FirmwareState.Error(errorMessage)
                        _events.emit(FirmwareEvent.ShowError(errorMessage))
                    }
                }
            }
        }
    }

    // Download firmware update
    fun downloadFirmwareUpdate(
        firmwareInfo: FirmwareRepository.FirmwareInfo,
        outputDir: File,
    ) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0f)
            when (val result = firmwareRepository.downloadFirmware(firmwareInfo, outputDir)) {
                is BaseRepository.Result.Success -> {
                    _downloadState.value = DownloadState.Completed(result.data)
                    _events.emit(FirmwareEvent.ShowSuccess("Firmware downloaded successfully"))
                    _events.emit(FirmwareEvent.UpdateCompleted)
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Download failed"
                    _downloadState.value = DownloadState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Get firmware from assets as fallback
    fun getFirmwareFromAssets(isTC007: Boolean) {
        viewModelScope.launch {
            when (val result = firmwareRepository.getFirmwareFromAssets(isTC007)) {
                is BaseRepository.Result.Success -> {
                    _firmwareState.value = FirmwareState.UpdateAvailable(result.data)
                    _events.emit(FirmwareEvent.ShowUpdateDialog(result.data))
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Failed to load local firmware"
                    _firmwareState.value = FirmwareState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Error handling
    fun onBindError() {
        _firmwareState.value = FirmwareState.Error("Service binding failed", true)
        viewModelScope.launch {
            _events.emit(FirmwareEvent.ShowError("Firmware service is not available"))
        }
    }

    // Clear error state
    fun clearError() {
        if (_firmwareState.value is FirmwareState.Error) {
            _firmwareState.value = FirmwareState.Idle
        }
        if (_downloadState.value is DownloadState.Error) {
            _downloadState.value = DownloadState.Idle
        }
    }

    // Reset states
    fun reset() {
        _firmwareState.value = FirmwareState.Idle
        _downloadState.value = DownloadState.Idle
    }

    // Compatibility with legacy FirmwareData
    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long,
    ) {
        companion object {
            fun fromFirmwareInfo(info: FirmwareRepository.FirmwareInfo): FirmwareData =
                FirmwareData(
                    version = info.version,
                    updateStr = info.updateDescription,
                    downUrl = info.downloadUrl,
                    size = info.size,
                )
        }
    }

    // Convert to legacy format for compatibility
    fun getFirmwareDataLegacy(): FirmwareData? {
        val state = _firmwareState.value
        return if (state is FirmwareState.UpdateAvailable) {
            FirmwareData.fromFirmwareInfo(state.info)
        } else {
            null
        }
    }
}
