package com.mpdc4gsr.component.thermal.viewmodel

import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import com.mpdc4gsr.component.shared.app.repository.BaseRepository
import kotlinx.coroutines.flow.*

class ReportDetailViewModel : BaseViewModel() {
    private val _reportDate = MutableStateFlow("")
    val reportDate: StateFlow<String> = _reportDate.asStateFlow()
    private val _reportTime = MutableStateFlow("")
    val reportTime: StateFlow<String> = _reportTime.asStateFlow()
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    private val _inspector = MutableStateFlow("")
    val inspector: StateFlow<String> = _inspector.asStateFlow()
    private val _equipment = MutableStateFlow("")
    val equipment: StateFlow<String> = _equipment.asStateFlow()
    private val _reportId = MutableStateFlow<String?>(null)
    val reportId: StateFlow<String?> = _reportId.asStateFlow()
    private val _events = MutableSharedFlow<ReportDetailEvent>()
    val events: SharedFlow<ReportDetailEvent> = _events.asSharedFlow()
    private val reportRepository = ReportDetailRepository()

    sealed class ReportDetailEvent {
        data class ShareReport(
            val reportId: String,
        ) : ReportDetailEvent()

        data class DeleteReport(
            val reportId: String,
        ) : ReportDetailEvent()
    }

    fun loadReportData(reportId: String) {
        launchWithLoading {
            _reportId.value = reportId
            val result = reportRepository.getReportById(reportId)
            when (result) {
                is BaseRepository.Result.Success -> {
                    val report = result.data
                    _reportDate.value = report.date
                    _reportTime.value = report.time
                    _location.value = report.location
                    _inspector.value = report.inspector
                    _equipment.value = report.equipment
                }

                is BaseRepository.Result.Error -> {
                    throw result.exception
                }

                else -> {}
            }
        }
    }

    fun shareReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.ShareReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to share"))
            }
        }
    }

    fun deleteReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.DeleteReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to delete"))
            }
        }
    }

    private inner class ReportDetailRepository : BaseRepository() {
        suspend fun getReportById(reportId: String): Result<ReportDetail> =
            safeCall {
                val cacheKey = "report_detail_$reportId"
                getCachedOrExecute(cacheKey, 5 * 60 * 1000L) {
                    ReportDetail(
                        id = reportId,
                        date = "2024-10-01",
                        time = "14:30:00",
                        location = "Building A - Room 101",
                        inspector = "John Doe",
                        equipment = "TC001 Thermal Camera",
                    )
                }
            }
    }

    data class ReportDetail(
        val id: String,
        val date: String,
        val time: String,
        val location: String,
        val inspector: String,
        val equipment: String,
    )
}



