package mpdc4gsr.compose.examples

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.SensorStatusCard
import mpdc4gsr.compose.components.ThermalVisualizationCard
import mpdc4gsr.compose.utils.AndroidViewWrapper
import mpdc4gsr.viewmodel.MainActivityViewModel
import mpdc4gsr.viewmodel.ConnectionState

/**
 * Example of hybrid Activity that demonstrates gradual migration approach:
 * - Uses Compose for some UI components (status cards, controls)
 * - Retains existing Views for complex components (thermal camera surface)
 * - Maintains full backward compatibility
 * 
 * This can serve as a pattern for migrating other Activities
 */
class HybridMainActivity : BaseComposeActivity<MainActivityViewModel>() {

    override fun createViewModel(): MainActivityViewModel {
        val viewModel: MainActivityViewModel by viewModels()
        return viewModel
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MainActivityViewModel) {
        // Observe ViewModel state
        val isRecording by viewModel.isRecording.observeAsState(false)
        val thermalData by viewModel.thermalData.observeAsState()
        val connectionStates by viewModel.connectionStates.observeAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("IRCamera - Hybrid UI") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pure Compose components
                connectionStates?.let { states ->
                    SensorStatusCard(
                        thermalCameraState = states.thermalCamera,
                        gsrSensorState = states.gsrSensor,
                        bleConnectionState = states.bleConnection
                    )
                }
                
                thermalData?.let { data ->
                    ThermalVisualizationCard(
                        centerTemp = data.centerTemp,
                        maxTemp = data.maxTemp,
                        minTemp = data.minTemp,
                        isRecording = isRecording,
                        onSettingsClick = {
                            // Navigate to settings - could use existing navigation
                            // or new Compose navigation
                        }
                    )
                }
                
                // Embed existing thermal camera surface view
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AndroidViewWrapper(
                        viewFactory = {
                            // Create the existing thermal camera surface view
                            // This preserves all existing functionality while allowing
                            // gradual replacement with Compose alternatives
                            createThermalCameraSurfaceView(it)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { view ->
                        // Configure view updates as needed
                        // This can handle state changes from the ViewModel
                    }
                }
                
                // Compose-based recording controls
                RecordingControls(
                    isRecording = isRecording,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecording() }
                )
            }
        }
    }

    @Composable
    private fun RecordingControls(
        isRecording: Boolean,
        onStartRecording: () -> Unit,
        onStopRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isRecording) {
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Recording")
                    }
                } else {
                    Button(onClick = onStartRecording) {
                        Text("Start Recording")
                    }
                }
                
                OutlinedButton(onClick = { /* Open settings */ }) {
                    Text("Settings")
                }
            }
        }
    }
    
    // Placeholder for creating thermal camera surface view
    // This would use existing thermal camera implementation
    private fun createThermalCameraSurfaceView(context: android.content.Context): android.view.View {
        // Return existing thermal camera surface view
        // Implementation would integrate with existing ThermalFragment/IrSurfaceView
        return androidx.compose.ui.platform.ComposeView(context)
    }
}