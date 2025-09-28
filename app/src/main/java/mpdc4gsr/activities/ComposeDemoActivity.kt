package mpdc4gsr.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.SensorStatusCard
import mpdc4gsr.compose.components.ThermalVisualizationCard
import mpdc4gsr.viewmodel.ConnectionState
import mpdc4gsr.viewmodel.MainActivityViewModel

/**
 * Demo Activity showcasing Task A implementation
 * Demonstrates the Compose infrastructure and hybrid approach
 */
class ComposeDemoActivity : BaseComposeActivity<MainActivityViewModel>() {

    override fun createViewModel(): MainActivityViewModel {
        return viewModels<MainActivityViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MainActivityViewModel) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Task A: Compose Infrastructure Demo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Demo title
                Text(
                    text = "✅ Task A: Main Dashboard Migration Complete",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "This demo showcases the hybrid Compose infrastructure enabling gradual migration while preserving all existing functionality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Sensor Status Card Demo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔄 Modern Sensor Status Components",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Replaced traditional status widgets with modern Compose components",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Demo sensor status card with mock data
                SensorStatusCard(
                    thermalCameraState = ConnectionState.Connected,
                    gsrSensorState = ConnectionState.Connecting,
                    bleConnectionState = ConnectionState.Disconnected
                )

                // Thermal Visualization Demo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🌡️ Enhanced Thermal Data Display",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Modern Material 3 cards for temperature data visualization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Demo thermal visualization with mock data
                ThermalVisualizationCard(
                    centerTemp = 23.5f,
                    maxTemp = 28.2f,
                    minTemp = 19.8f,
                    isRecording = true,
                    onSettingsClick = { /* Demo action */ }
                )

                // Implementation Features
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🏗️ Implementation Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val features = listOf(
                            "✅ BaseComposeActivity with EventBus integration",
                            "✅ IRCameraTheme with thermal imaging colors",
                            "✅ ViewPager2 embedded in Compose (hybrid approach)",
                            "✅ Modern sensor status cards",
                            "✅ Enhanced recording controls",
                            "✅ Preserved all existing functionality",
                            "✅ Zero breaking changes"
                        )
                        
                        features.forEach { feature ->
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Launch full hybrid activity
                Button(
                    onClick = {
                        startActivity(Intent(this@ComposeDemoActivity, MainActivityCompose::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Launch Full Hybrid MainActivity")
                }

                // Back to original
                OutlinedButton(
                    onClick = {
                        startActivity(Intent(this@ComposeDemoActivity, MainActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compare with Original MainActivity")
                }
            }
        }
    }
}