package com.mpdc4gsr.module.thermalunified.feature.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.module.thermalunified.feature.device.ThermalDeviceStatus

@Composable
fun ThermalStatusBanner(
    status: ThermalDeviceStatus,
    modifier: Modifier = Modifier,
) {
    val bannerColor =
        when {
            status.isStreaming -> Color(0xFF1B5E20)
            status.isConnected -> Color(0xFF33691E)
            status.lastError != null -> Color(0xFFB71C1C)
            else -> Color(0xFF37474F)
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = status.deviceLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val secondary =
                when {
                    status.lastError != null -> status.lastError
                    status.isStreaming -> "Streaming active - Radiometric capture ready"
                    status.isConnected -> "Hardware linked - Start stream when ready"
                    else -> "Connect the Topdon TC001 to begin"
                }
            Text(
                text = secondary ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
