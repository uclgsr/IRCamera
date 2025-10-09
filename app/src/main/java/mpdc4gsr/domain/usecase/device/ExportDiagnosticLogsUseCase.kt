package mpdc4gsr.domain.usecase

import mpdc4gsr.domain.repository.DiagnosticsRepository
import javax.inject.Inject

class ExportDiagnosticLogsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(): String {
        return diagnosticsRepository.exportDiagnosticLogs()
    }
}
