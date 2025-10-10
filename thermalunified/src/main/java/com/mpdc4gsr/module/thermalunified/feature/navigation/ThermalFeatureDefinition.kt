package com.mpdc4gsr.module.thermalunified.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.fragment.app.Fragment
import java.util.concurrent.ConcurrentHashMap

sealed interface ThermalFeatureContent {
    class Compose(
        val body: @Composable () -> Unit,
    ) : ThermalFeatureContent

    class FragmentHost(
        val factory: () -> Fragment,
    ) : ThermalFeatureContent
}

@Immutable
data class ThermalFeatureDescriptor(
    val id: String,
    val title: String,
    val iconId: String,
    val priority: Int = 0,
    val content: ThermalFeatureContent,
)

class ThermalFeatureRegistry {
    private val descriptors = ConcurrentHashMap<String, ThermalFeatureDescriptor>()

    fun register(descriptor: ThermalFeatureDescriptor) {
        descriptors[descriptor.id] = descriptor
    }

    fun registerAll(descriptorList: List<ThermalFeatureDescriptor>) {
        descriptorList.forEach(::register)
    }

    fun available(): List<ThermalFeatureDescriptor> =
        descriptors.values.sortedWith(
            compareByDescending<ThermalFeatureDescriptor> { it.priority }
                .thenBy { it.title },
        )

    fun findById(id: String): ThermalFeatureDescriptor? = descriptors[id]

    fun hasFeature(id: String): Boolean = descriptors.containsKey(id)

    fun defaultFeatureId(): String? = available().firstOrNull()?.id
}
