package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportDetailViewModel : BaseViewModel() {

    private val _reportDate = MutableStateFlow("2024-10-01")
    val reportDate: StateFlow<String> = _reportDate.asStateFlow()

    private val _reportTime = MutableStateFlow("14:30:00")
    val reportTime: StateFlow<String> = _reportTime.asStateFlow()

    private val _location = MutableStateFlow("Building A - Room 101")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _inspector = MutableStateFlow("John Doe")
    val inspector: StateFlow<String> = _inspector.asStateFlow()

    private val _equipment = MutableStateFlow("TC001 Thermal Camera")
    val equipment: StateFlow<String> = _equipment.asStateFlow()

    private val _reportId = MutableStateFlow<String?>(null)
    val reportId: StateFlow<String?> = _reportId.asStateFlow()

    private val _events = MutableSharedFlow<ReportDetailEvent>()
    val events: SharedFlow<ReportDetailEvent> = _events.asSharedFlow()

    sealed class ReportDetailEvent {
        data class ShareReport(val reportId: String) : ReportDetailEvent()
        data class DeleteReport(val reportId: String) : ReportDetailEvent()
        data class ShowMessage(val message: String) : ReportDetailEvent()
        data class ShowError(val message: String) : ReportDetailEvent()
    }

    fun loadReportData(reportId: String) {
        launchWithLoading {
            try {
                _reportId.value = reportId
                _reportDate.value = "2024-10-01"
                _reportTime.value = "14:30:00"
                _location.value = "Building A - Room 101"
                _inspector.value = "John Doe"
                _equipment.value = "TC001 Thermal Camera"
                _events.emit(ReportDetailEvent.ShowMessage("Report data loaded successfully"))
            } catch (e: Exception) {
                _events.emit(ReportDetailEvent.ShowError("Failed to load report data: ${e.message}"))
            }
        }
    }

    fun shareReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.ShareReport(currentReportId))
            } else {
                _events.emit(ReportDetailEvent.ShowError("No report loaded to share"))
            }
        }
    }

    fun deleteReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.DeleteReport(currentReportId))
            } else {
                _events.emit(ReportDetailEvent.ShowError("No report loaded to delete"))
            }
        }
    }
}
