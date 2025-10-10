package mpdc4gsr.feature.device.domain.usecase

import mpdc4gsr.feature.device.domain.repository.DiagnosticsRepository
import javax.inject.Inject

class ExportDiagnosticLogsUseCase
    @Inject
    constructor(
        private val diagnosticsRepository: DiagnosticsRepository,
    ) {
        suspend operator fun invoke(): String = diagnosticsRepository.exportDiagnosticLogs()
    }
