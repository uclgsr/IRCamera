package com.mpdc4gsr.component.thermal.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import com.mpdc4gsr.component.thermal.feature.device.ThermalColorPalette
import com.mpdc4gsr.component.thermal.feature.device.ThermalDeviceConfig
import com.mpdc4gsr.component.thermal.feature.device.ThermalDeviceManager
import com.mpdc4gsr.component.thermal.feature.device.ThermalDeviceStatus
import com.mpdc4gsr.component.thermal.feature.device.ThermalGainMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : BaseViewModel() {
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _deviceStatus = MutableStateFlow(ThermalDeviceStatus())
    val deviceStatus: StateFlow<ThermalDeviceStatus> = _deviceStatus.asStateFlow()

    private var deviceManager: ThermalDeviceManager? = null
    private var deviceStatusJob: Job? = null
    private var currentConfig = ThermalDeviceConfig()
    private val _selectedToolAction = MutableStateFlow<Int?>(null)
    val selectedToolAction: StateFlow<Int?> = _selectedToolAction.asStateFlow()

    fun attachDeviceManager(manager: ThermalDeviceManager) {
        if (deviceManager === manager) return
        deviceManager = manager
        deviceStatusJob?.cancel()
        deviceStatusJob =
            viewModelScope.launch {
                manager.status.collect { status ->
                    _deviceStatus.value = status
                }
            }
    }

    fun connectHardware() {
        val manager = deviceManager ?: return
        viewModelScope.launch {
            manager.connect()
        }
    }

    fun startStream(config: ThermalDeviceConfig = currentConfig) {
        val manager = deviceManager ?: return
        currentConfig = config
        viewModelScope.launch {
            manager.startStream(config)
        }
    }

    fun stopStream() {
        val manager = deviceManager ?: return
        viewModelScope.launch {
            manager.stopStream()
        }
    }

    fun triggerManualCalibration() {
        val manager = deviceManager ?: return
        viewModelScope.launch {
            manager.triggerManualCalibration()
        }
    }

    fun updatePalette(palette: ThermalColorPalette) {
        startStream(currentConfig.copy(colorPalette = palette))
    }

    fun updateGainMode(gainMode: ThermalGainMode) {
        startStream(currentConfig.copy(gainMode = gainMode))
    }

    fun exportData(
        context: Context,
        format: ExportFormat,
    ) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                val exportFile = createExportFile(context, format)
                _exportStatus.value = ExportStatus.Success(exportFile)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun createExportFile(
        context: Context,
        format: ExportFormat,
    ): File {
        val fileName = "thermal_export_${System.currentTimeMillis()}.${format.extension}"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun captureSnapshot() {
        viewModelScope.launch {
            // Capture thermal snapshot
        }
    }

    fun onToolActionSelected(actionCode: Int) {
        _selectedToolAction.value = actionCode
    }

    override fun onCleared() {
        super.onCleared()
        deviceStatusJob?.cancel()
    }

    sealed class ExportStatus {
        object Idle : ExportStatus()

        object Exporting : ExportStatus()

        data class Success(
            val file: File,
        ) : ExportStatus()

        data class Error(
            val message: String,
        ) : ExportStatus()
    }

    enum class ExportFormat(
        val extension: String,
    ) {
        CSV("csv"),
        JSON("json"),
        PDF("pdf"),
    }
}



