package mpdc4gsr.feature.main.domain.usecase

import mpdc4gsr.feature.main.domain.repository.GsrRepository
import javax.inject.Inject

class ConnectGsrSensorUseCase @Inject constructor(
    private val gsrRepository: GsrRepository
) {
    suspend operator fun invoke(): Boolean {
        val initialized = gsrRepository.initialize()
        if (!initialized) return false

        val discovered = gsrRepository.startDeviceDiscovery()
        if (!discovered) return false

        return true
    }
}
