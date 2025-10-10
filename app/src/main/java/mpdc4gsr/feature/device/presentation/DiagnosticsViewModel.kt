package mpdc4gsr.feature.device.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mpdc4gsr.feature.device.domain.repository.SensorStatus
import mpdc4gsr.feature.device.domain.repository.SystemStatus
import mpdc4gsr.feature.device.domain.usecase.ExportDiagnosticLogsUseCase
import mpdc4gsr.feature.device.domain.usecase.RunFullDiagnosticsUseCase
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel
@Inject
constructor(
    private val runFullDiagnosticsUseCase: RunFullDiagnosticsUseCase,
    private val exportDiagnosticLogsUseCase: ExportDiagnosticLogsUseCase,
    diagnosticsRepository: mpdc4gsr.feature.device.domain.repository.DiagnosticsRepository,
) : ViewModel() {
    val systemStatus: StateFlow<SystemStatus> =
        diagnosticsRepository
            .getSystemStatus()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SystemStatus(),
            )

    val sensorStatus: StateFlow<SensorStatus> =
        diagnosticsRepository
            .getSensorStatus()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SensorStatus(),
            )

    fun runFullDiagnostics() {
        viewModelScope.launch {
            runFullDiagnosticsUseCase()
        }
    }

    fun testAllSensors() {
        viewModelScope.launch {
            runFullDiagnosticsUseCase()
        }
    }

    fun exportDiagnosticLogs() {
        viewModelScope.launch {
            exportDiagnosticLogsUseCase()
        }
    }
}
