package mpdc4gsr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStatusCard(
    thermalCameraState: ConnectionState,
    gsrSensorState: ConnectionState,
    bleConnectionState: ConnectionState,
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
            Text(
                text = "Sensor Status",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SensorStatusRow(
                label = "Thermal Camera",
                state = thermalCameraState,
                details = "TC001 384x288"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SensorStatusRow(
                label = "GSR Sensor",
                state = gsrSensorState,
                details = "Shimmer3 51.2Hz"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SensorStatusRow(
                label = "BLE Connection",
                state = bleConnectionState,
                details = "Bluetooth LE"
            )
        }
    }
}

@Composable
private fun SensorStatusRow(
    label: String,
    state: ConnectionState,
    details: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        StatusIndicator(state = state)
    }
}

@Composable
private fun StatusIndicator(state: ConnectionState) {
    val (icon, color, text) = when (state) {
        is ConnectionState.Connected -> Triple(
            Icons.Default.CheckCircle,
            Color.Green,
            "Connected"
        )

        is ConnectionState.Connecting -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.primary,
            "Connecting"
        )

        is ConnectionState.Disconnected -> Triple(
            Icons.Default.Error,
            Color.Red,
            "Disconnected"
        )

        is ConnectionState.Error -> Triple(
            Icons.Default.Error,
            Color.Red,
            "Error"
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}