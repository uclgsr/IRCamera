package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.feature.device.TopdonThermalDeviceManager
import com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureContent
import com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureDescriptor
import com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureRegistry
import com.mpdc4gsr.module.thermalunified.feature.presentation.ThermalFeatureCoordinator
import com.mpdc4gsr.module.thermalunified.feature.presentation.ThermalPresentationState
import com.mpdc4gsr.module.thermalunified.feature.ui.ThermalFeatureScaffoldContent
import com.mpdc4gsr.module.thermalunified.fragment.AbilityComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.PDFListComposeFragment
import com.mpdc4gsr.module.user.compose.MoreComposeFragment
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel
import kotlinx.coroutines.launch

class IRMainComposeActivity : AppCompatActivity() {
    private lateinit var deviceManager: TopdonThermalDeviceManager
    private lateinit var featureRegistry: ThermalFeatureRegistry
    private lateinit var featureCoordinator: ThermalFeatureCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceManager = TopdonThermalDeviceManager(this, lifecycleScope)
        featureRegistry =
            ThermalFeatureRegistry().apply {
                registerAll(buildFeatureDescriptors())
            }
        featureCoordinator =
            ThermalFeatureCoordinator(
                registry = featureRegistry,
                deviceManager = deviceManager,
                scope = lifecycleScope,
            )

        lifecycleScope.launch {
            deviceManager.connect()
        }

        setContent {
            LibUnifiedTheme {
                val state by featureCoordinator.state.collectAsStateWithLifecycle()
                FeatureStreamGuard(
                    state = state,
                    deviceManager = deviceManager,
                )
                ThermalFeatureScaffoldContent(
                    state = state,
                    onTabSelected = featureCoordinator::selectFeature,
                    topContent = { ThermalDashboardHeader() },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            deviceManager.stopStream()
            deviceManager.disconnect()
        }
    }

    private fun buildFeatureDescriptors(): List<ThermalFeatureDescriptor> =
        listOf(
            ThermalFeatureDescriptor(
                id = ThermalFeatureIds.THERMAL,
                title = "Thermal",
                iconId = "videocam",
                priority = 100,
                content = ThermalFeatureContent.FragmentHost { IRThermalComposeFragment() },
            ),
            ThermalFeatureDescriptor(
                id = ThermalFeatureIds.GALLERY,
                title = "Gallery",
                iconId = "photo",
                priority = 80,
                content = ThermalFeatureContent.FragmentHost { IRGalleryTabComposeFragment() },
            ),
            ThermalFeatureDescriptor(
                id = ThermalFeatureIds.ABILITY,
                title = "Ability",
                iconId = "grid",
                priority = 60,
                content = ThermalFeatureContent.FragmentHost { AbilityComposeFragment() },
            ),
            ThermalFeatureDescriptor(
                id = ThermalFeatureIds.PDF,
                title = "PDF",
                iconId = "description",
                priority = 40,
                content = ThermalFeatureContent.FragmentHost { PDFListComposeFragment() },
            ),
            ThermalFeatureDescriptor(
                id = ThermalFeatureIds.MORE,
                title = "More",
                iconId = "more",
                priority = 20,
                content =
                    ThermalFeatureContent.Compose {
                        val moreViewModel: MoreComposeFragmentViewModel = viewModel()
                        MoreComposeFragment(
                            viewModel = moreViewModel,
                            isTC007 = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
            ),
        )

    @Composable
    private fun FeatureStreamGuard(
        state: ThermalPresentationState,
        deviceManager: TopdonThermalDeviceManager,
    ) {
        val activeFeature = state.activeFeatureId
        LaunchedEffect(activeFeature) {
            if (activeFeature == ThermalFeatureIds.THERMAL) {
                deviceManager.startStream()
            } else {
                deviceManager.stopStream()
            }
        }
    }

    @Composable
    private fun ThermalDashboardHeader() {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Thermal Experience Suite",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Topdon TC001 - Unified presentation across thermal modules",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private object ThermalFeatureIds {
        const val THERMAL = "thermal-core"
        const val GALLERY = "thermal-gallery"
        const val ABILITY = "thermal-ability"
        const val PDF = "thermal-pdf"
        const val MORE = "thermal-more"
    }
}
