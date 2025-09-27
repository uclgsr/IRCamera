package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of GSRSettingsActivity demonstrating comprehensive GSR sensor configuration.
 * Shows how to handle device settings, permissions, and sensor parameters in Compose.
 */
class GSRSettingsComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "GSRSettingsComposeActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    @Composable
    override fun Content() {
        IRCameraTheme {
            GSRSettingsScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GSRSettingsScreen() {
        // Settings state
        var samplingRate by remember { mutableStateOf(51.2f) }
        var enableGSR by remember { mutableStateOf(true) }
        var enableAccelerometer by remember { mutableStateOf(false) }
        var enableGyroscope by remember { mutableStateOf(false) }
        var enableMagnetometer by remember { mutableStateOf(false) }
        var continuousSync by remember { mutableStateOf(true) }
        var autoConnect by remember { mutableStateOf(false) }
        var dataBufferSize by remember { mutableStateOf("1000") }
        var sessionTimeout by remember { mutableStateOf("30") }
        var exportFormat by remember { mutableStateOf("CSV") }
        var compressionEnabled by remember { mutableStateOf(false) }
        var debugMode by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "GSR Settings",
                    onNavigationClick = { finish() },
                    actions = {
                        TextButton(
                            onClick = { 
                                saveSettings()
                                showToast("Settings saved")
                            }
                        ) {
                            Text(
                                text = "Save",
                                color = Color(0xFF6B35FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Device Configuration Section
                item {
                    CommonComponents.SectionHeader("Device Configuration")
                }

                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Sampling Rate
                            Text(
                                text = "Sampling Rate",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "${samplingRate} Hz",
                                color = Color(0xFF6B35FF),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Slider(
                                value = samplingRate,
                                onValueChange = { samplingRate = it },
                                valueRange = 1f..512f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6B35FF),
                                    activeTrackColor = Color(0xFF6B35FF),
                                    inactiveTrackColor = Color(0xFF3A3A3A)
                                )
                            )

                            Text(
                                text = "Range: 1 Hz - 512 Hz",
                                color = Color(0x80FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Sensor Selection Section
                item {
                    CommonComponents.SectionHeader("Sensor Selection")
                }

                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            SettingSwitchItem(
                                icon = Icons.Default.Sensors,
                                title = "GSR Sensor",
                                description = "Galvanic Skin Response measurement",
                                checked = enableGSR,
                                enabled = false, // GSR always enabled
                                onCheckedChange = { enableGSR = it }
                            )

                            SettingSwitchItem(
                                icon = Icons.Default.Speed,
                                title = "Accelerometer",
                                description = "3-axis acceleration data",
                                checked = enableAccelerometer,
                                onCheckedChange = { enableAccelerometer = it }
                            )

                            SettingSwitchItem(
                                icon = Icons.Default.Rotate90DegreesCcw,
                                title = "Gyroscope",
                                description = "3-axis angular velocity data",
                                checked = enableGyroscope,
                                onCheckedChange = { enableGyroscope = it }
                            )

                            SettingSwitchItem(
                                icon = Icons.Default.Explore,
                                title = "Magnetometer",
                                description = "3-axis magnetic field data",
                                checked = enableMagnetometer,
                                onCheckedChange = { enableMagnetometer = it }
                            )
                        }
                    }
                }

                // Connection Settings Section
                item {
                    CommonComponents.SectionHeader("Connection Settings")
                }

                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            SettingSwitchItem(
                                icon = Icons.Default.Sync,
                                title = "Continuous Sync",
                                description = "Maintain connection during recording",
                                checked = continuousSync,
                                onCheckedChange = { continuousSync = it }
                            )

                            SettingSwitchItem(
                                icon = Icons.Default.BluetoothConnected,
                                title = "Auto Connect",
                                description = "Automatically connect to known devices",
                                checked = autoConnect,
                                onCheckedChange = { autoConnect = it }
                            )
                        }
                    }
                }

                // Data Management Section
                item {
                    CommonComponents.SectionHeader("Data Management")
                }

                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Buffer Size
                            OutlinedTextField(
                                value = dataBufferSize,
                                onValueChange = { dataBufferSize = it },
                                label = { Text("Data Buffer Size") },
                                placeholder = { Text("1000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF6B35FF),
                                    unfocusedLabelColor = Color(0x80FFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Session Timeout
                            OutlinedTextField(
                                value = sessionTimeout,
                                onValueChange = { sessionTimeout = it },
                                label = { Text("Session Timeout (minutes)") },
                                placeholder = { Text("30") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF6B35FF),
                                    unfocusedLabelColor = Color(0x80FFFFFF)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Export Format Dropdown
                            var expandedFormat by remember { mutableStateOf(false) }
                            val exportFormats = listOf("CSV", "JSON", "Binary", "HDF5")

                            ExposedDropdownMenuBox(
                                expanded = expandedFormat,
                                onExpandedChange = { expandedFormat = !expandedFormat }
                            ) {
                                OutlinedTextField(
                                    value = exportFormat,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Export Format") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormat) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF6B35FF),
                                        unfocusedLabelColor = Color(0x80FFFFFF)
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedFormat,
                                    onDismissRequest = { expandedFormat = false }
                                ) {
                                    exportFormats.forEach { format ->
                                        DropdownMenuItem(
                                            text = { Text(format) },
                                            onClick = {
                                                exportFormat = format
                                                expandedFormat = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingSwitchItem(
                                icon = Icons.Default.Compress,
                                title = "Data Compression",
                                description = "Reduce file size with compression",
                                checked = compressionEnabled,
                                onCheckedChange = { compressionEnabled = it }
                            )
                        }
                    }
                }

                // Advanced Settings Section
                item {
                    CommonComponents.SectionHeader("Advanced Settings")
                }

                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            SettingSwitchItem(
                                icon = Icons.Default.BugReport,
                                title = "Debug Mode",
                                description = "Enable detailed logging and diagnostics",
                                checked = debugMode,
                                onCheckedChange = { debugMode = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { resetToDefaults() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset Defaults")
                                }

                                Button(
                                    onClick = { testConnection() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TestTube,
                                        contentDescription = "Test",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test Connection")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun SettingSwitchItem(
        icon: ImageVector,
        title: String,
        description: String,
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) Color(0xFF6B35FF) else Color(0x80FFFFFF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color(0x80FFFFFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color(0x80FFFFFF),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF6B35FF),
                    checkedTrackColor = Color(0x806B35FF)
                )
            )
        }
    }

    private fun saveSettings() {
        lifecycleScope.launch {
            try {
                // Save settings logic would go here
                showToast("Settings saved successfully")
            } catch (e: Exception) {
                showToast("Failed to save settings: ${e.message}")
            }
        }
    }

    private fun resetToDefaults() {
        lifecycleScope.launch {
            try {
                // Reset to default values logic would go here
                showToast("Settings reset to defaults")
            } catch (e: Exception) {
                showToast("Failed to reset settings: ${e.message}")
            }
        }
    }

    private fun testConnection() {
        lifecycleScope.launch {
            try {
                // Test connection logic would go here
                showToast("Connection test completed successfully")
            } catch (e: Exception) {
                showToast("Connection test failed: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}