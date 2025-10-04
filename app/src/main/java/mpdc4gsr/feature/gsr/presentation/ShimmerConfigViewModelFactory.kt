package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*

/**
 * Factory for ShimmerConfigViewModel that constructs the entire dependency graph.
 *
 * This factory creates:
 * - ShimmerDeviceManager (requires Application context and LifecycleOwner)
 * - ShimmerDataSource (wraps ShimmerDeviceManager)
 * - ShimmerRepository (uses ShimmerDataSource)
 * - All use cases (each requires ShimmerRepository)
 * - ShimmerConfigViewModel (uses all use cases)
 *
 * Note: Since ShimmerDeviceManager requires LifecycleOwner, we use a simplified approach
 * by creating instances without lifecycle binding. For full lifecycle management,
 * consider migrating to Hilt dependency injection.
 */
class ShimmerConfigViewModelFactory(
    private val application: Application,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShimmerConfigViewModel::class.java)) {
            // Build the dependency graph from bottom up
            val shimmerDeviceManager = ShimmerDeviceManager(application, lifecycleOwner)
            val shimmerDataSource = ShimmerDataSourceImpl(shimmerDeviceManager)
            val shimmerRepository: ShimmerRepository = ShimmerRepositoryImpl(shimmerDataSource)

            // Create all use cases with the repository
            val scanDevicesUseCase = ScanShimmerDevicesUseCase(shimmerRepository)
            val connectDeviceUseCase = ConnectShimmerDeviceUseCase(shimmerRepository)
            val disconnectDeviceUseCase = DisconnectShimmerDeviceUseCase(shimmerRepository)
            val getBatteryLevelUseCase = GetDeviceBatteryUseCase(shimmerRepository)
            val checkConnectionUseCase = CheckDeviceConnectionUseCase(shimmerRepository)

            // Create the ViewModel with all use cases
            return ShimmerConfigViewModel(
                scanDevicesUseCase = scanDevicesUseCase,
                connectDeviceUseCase = connectDeviceUseCase,
                disconnectDeviceUseCase = disconnectDeviceUseCase,
                getBatteryLevelUseCase = getBatteryLevelUseCase,
                checkConnectionUseCase = checkConnectionUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
