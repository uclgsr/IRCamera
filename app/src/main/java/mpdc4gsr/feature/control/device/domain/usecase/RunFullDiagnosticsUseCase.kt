package mpdc4gsr.feature.control.device.domain.usecase

import mpdc4gsr.feature.control.device.domain.repository.DiagnosticsRepository
import javax.inject.Inject

class RunFullDiagnosticsUseCase
@Inject
constructor(
    private val diagnosticsRepository: DiagnosticsRepository,
) {
    suspend operator fun invoke() {
        diagnosticsRepository.runFullDiagnostics()
    }
}

