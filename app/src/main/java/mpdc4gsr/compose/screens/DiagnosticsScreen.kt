package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.*
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Diagnostics Screen - System diagnostics and troubleshooting
 */
@Composable
fun DiagnosticsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Diagnostics",
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Status
            SettingsCard(
                title = "System Status",
                icon = Icons.Default.Computer
            ) {
                SettingsRow(
                    label = "System Health",
                    value = "Good"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Battery",
                    value = "85%"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Temperature",
                    value = "42°C"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Memory Usage",
                    value = "2.4 GB / 8 GB"
                )
            }

            // Sensor Status
            SettingsCard(
                title = "Sensor Status",
                icon = Icons.Default.Sensors
            ) {
                SettingsRow(
                    label = "GSR Sensor",
                    value = "OK"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Thermal Camera",
                    value = "OK"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "RGB Camera",
                    value = "OK"
                )
            }

            // Diagnostic Tools
            SettingsCard(
                title = "Diagnostic Tools",
                icon = Icons.Default.Build
            ) {
                Button(
                    onClick = { /* Run diagnostics */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Full Diagnostics")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Test sensors */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test All Sensors")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* Export logs */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Diagnostic Logs")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsScreenPreview() {
    IRCameraTheme {
        DiagnosticsScreen()
    }
}
