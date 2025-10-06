package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : BaseViewModel() {
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun yuvArea(
        yuv: ByteArray,
        temp: FloatArray,
        max: Float,
        min: Float,
    ) {
        for (i in temp.indices) {
            if (temp[i] < min) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0x00.toByte()
            }
            if (temp[i] > max) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0xFF.toByte()
            }
        }
    }

    fun exportData(context: Context, format: ExportFormat) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                // Implementation for data export
                val exportFile = createExportFile(context, format)
                // Export data to file based on format
                _exportStatus.value = ExportStatus.Success(exportFile)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun createExportFile(context: Context, format: ExportFormat): File {
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

    sealed class ExportStatus {
        object Idle : ExportStatus()
        object Exporting : ExportStatus()
        data class Success(val file: File) : ExportStatus()
        data class Error(val message: String) : ExportStatus()
    }

    enum class ExportFormat(val extension: String) {
        CSV("csv"),
        JSON("json"),
        PDF("pdf")
    }
}
