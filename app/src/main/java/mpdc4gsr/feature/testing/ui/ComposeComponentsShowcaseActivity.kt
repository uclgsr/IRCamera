package mpdc4gsr.feature.testing.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.*
import mpdc4gsr.core.ui.theme.IRCameraTheme

class ComposeComponentsShowcaseViewModel : AppBaseViewModel() {
    private val _showSensorDialog = mutableStateOf(false)
    val showSensorDialog: State<Boolean> = _showSensorDialog

    private val _selectedSensors = mutableStateOf<Set<mpdc4gsr.core.ui.components.SensorType>>(emptySet())
    val selectedSensors: State<Set<mpdc4gsr.core.ui.components.SensorType>> = _selectedSensors

    fun showSensorSelection() {
        _showSensorDialog.value = true
    }

    fun hideSensorSelection() {
        _showSensorDialog.value = false
    }

    fun updateSelectedSensors(sensors: Set<mpdc4gsr.core.ui.components.SensorType>) {
        _selectedSensors.value = sensors
    }
}

/**
 * Showcase activity for all new Compose components
 * Demonstrates the enhanced UI components that can be used throughout the application
 */
class ComposeComponentsShowcaseActivity :
    BaseComposeActivity<ComposeComponentsShowcaseViewModel>() {

    private val showcaseVM: ComposeComponentsShowcaseViewModel by viewModels()

    override fun createViewModel(): ComposeComponentsShowcaseViewModel =
        showcaseVM

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ComposeComponentsShowcaseViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val showSensorDialog by viewModel.showSensorDialog
            val selectedSensors by viewModel.selectedSensors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Compose Components",
                    onBackClick = { finish() }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Introduction card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Enhanced Compose Components",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Modernized UI components with improved functionality and user experience",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Component sections
                    ComponentSection(
                        title = "Sensor Dashboard",
                        description = "Demo component - sensors shown are disconnected. Use actual sensor dashboard for real connections."
                    ) {
                        SensorDashboardDemo(
                            onSensorClick = { sensor ->
                                // Handle sensor click - could show details dialog
                            },
                            onRefresh = {
                                // Handle refresh action
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ComponentSection(
                        title = "Recording Controls",
                        description = "Advanced recording controls with session management"
                    ) {
                        RecordingControlsDemo()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ComponentSection(
                        title = "Sensor Selection",
                        description = "Interactive sensor selection with availability checks"
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Button(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Sensors (${selectedSensors.size})")
                                }

                                /* Sensor display temporarily disabled - requires SensorAvailability component
                                if (selectedSensors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Selected Sensors:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    selectedSensors.forEach { sensor ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = sensor.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = sensor.displayName,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                                */
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Benefits section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Component Benefits",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val benefits = listOf(
                                "Real-time animated status indicators",
                                "Improved user interaction and feedback",
                                "Consistent Material 3 theming",
                                "Enhanced accessibility support",
                                "Reactive state management",
                                "Optimized performance with Compose",
                                "Modern UI patterns and animations",
                                "Better error handling and recovery"
                            )

                            benefits.forEach { benefit ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = benefit,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sensor selection dialog
            if (showSensorDialog) {
                SensorSelectionDialog(
                    availableSensors = getSampleSensorAvailability(),
                    selectedSensors = selectedSensors,
                    onSensorsSelected = { newSelection ->
                        viewModel.updateSelectedSensors(newSelection)
                    },
                    onDismiss = { viewModel.hideSensorSelection() },
                    title = "Research Sensors",
                    subtitle = "Select sensors for your research session"
                )
            }
        }
    }
}

@Composable
private fun ComponentSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        content()
    }
}

/* Sensor availability sample data temporarily disabled
private fun getSampleSensorAvailability(): List<mpdc4gsr.compose.components.SensorAvailability> {
    return listOf(
        mpdc4gsr.compose.components.SensorAvailability(
            sensorType = mpdc4gsr.compose.components.SensorType.THERMAL,
            isAvailable = true,
            isSelected = false
        ),
        mpdc4gsr.compose.components.SensorAvailability(
            sensorType = mpdc4gsr.compose.components.SensorType.GSR,
            isAvailable = true,
            isSelected = false
        ),
        mpdc4gsr.compose.components.SensorAvailability(
            sensorType = mpdc4gsr.compose.components.SensorType.RGB,
            isAvailable = true,
            isSelected = false
        ),
        mpdc4gsr.compose.components.SensorAvailability(
            sensorType = mpdc4gsr.compose.components.SensorType.AUDIO,
            isAvailable = true,
            isSelected = false
        )
    )
}
*/