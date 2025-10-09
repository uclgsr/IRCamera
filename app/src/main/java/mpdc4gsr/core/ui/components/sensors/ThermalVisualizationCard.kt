package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalVisualizationCard(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    isRecording: Boolean,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thermal Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Recording indicator
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated recording dot could be added here
                            Card(
                                modifier = Modifier.size(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red
                                ),
                                shape = RoundedCornerShape(50)
                            ) {}
                        }
                        Text(
                            text = "REC",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Temperature readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThermalReadingItem(
                    label = "Center",
                    temperature = centerTemp,
                    color = MaterialTheme.colorScheme.primary
                )
                ThermalReadingItem(
                    label = "Max",
                    temperature = maxTemp,
                    color = MaterialTheme.colorScheme.secondary
                )
                ThermalReadingItem(
                    label = "Min",
                    temperature = minTemp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSettingsClick) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun ThermalReadingItem(
    label: String,
    temperature: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${temperature.toInt()}°C",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
