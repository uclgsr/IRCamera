package com.mpdc4gsr.module.thermalunified.feature.presentation

import com.mpdc4gsr.module.thermalunified.feature.device.ThermalDeviceManager
import com.mpdc4gsr.module.thermalunified.feature.device.ThermalDeviceStatus
import com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureDescriptor
import com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThermalPresentationState(
    val features: List<ThermalFeatureDescriptor> = emptyList(),
    val activeFeatureId: String? = null,
    val deviceStatus: ThermalDeviceStatus = ThermalDeviceStatus(),
)

class ThermalFeatureCoordinator(
    private val registry: ThermalFeatureRegistry,
    private val deviceManager: ThermalDeviceManager,
    private val scope: CoroutineScope,
) {
    private val _state =
        MutableStateFlow(
            ThermalPresentationState(
                features = registry.available(),
                activeFeatureId = registry.defaultFeatureId(),
                deviceStatus = deviceManager.status.value,
            ),
        )
    val state: StateFlow<ThermalPresentationState> = _state.asStateFlow()

    init {
        scope.launch {
            deviceManager.status.collectLatest { status ->
                _state.update { current ->
                    current.copy(deviceStatus = status)
                }
            }
        }
    }

    fun selectFeature(id: String) {
        if (!registry.hasFeature(id)) return
        _state.update { state ->
            state.copy(activeFeatureId = id)
        }
    }

    fun refreshRegistry() {
        _state.update { current ->
            val features = registry.available()
            val active = current.activeFeatureId?.takeIf { registry.hasFeature(it) } ?: registry.defaultFeatureId()
            current.copy(features = features, activeFeatureId = active)
        }
    }
}
