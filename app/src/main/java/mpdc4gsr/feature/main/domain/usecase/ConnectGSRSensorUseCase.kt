package mpdc4gsr.feature.main.domain.usecase

import mpdc4gsr.feature.main.domain.repository.GSRRepository
import javax.inject.Inject

class ConnectGSRSensorUseCase @Inject constructor(
    private val gsrRepository: GSRRepository
) {
    suspend operator fun invoke(): Boolean {
        val initialized = gsrRepository.initialize()
        if (!initialized) return false
        
        val discovered = gsrRepository.startDeviceDiscovery()
        if (!discovered) return false
        
        return true
    }
}
