package mpdc4gsr.feature.control.settings.ui

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.compose.dialogs.TipDialogState
import com.mpdc4gsr.libunified.app.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.theme.IRCameraTheme
import javax.inject.Inject

data class HelpStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isActionable: Boolean = false,
    val actionText: String = "",
    val action: (() -> Unit)? = null
)

@HiltViewModel
class MoreHelpViewModel @Inject constructor() : ViewModel() {
    private val _connectionType = mutableStateOf(0)
    val connectionType: State<Int> = _connectionType
    private val _helpSteps = mutableStateOf<List<HelpStep>>(emptyList())
    val helpSteps: State<List<HelpStep>> = _helpSteps

    fun setConnectionType(type: Int) {
        _connectionType.value = type
        updateHelpSteps(type)
    }

    private fun updateHelpSteps(type: Int) {
        _helpSteps.value = if (type == Constants.SETTING_CONNECTION) {
            getConnectionHelpSteps()
        } else {
            getDisconnectionHelpSteps()
        }
    }

    private fun getConnectionHelpSteps(): List<HelpStep> {
        return listOf(
            HelpStep(
                icon = Icons.Default.Power,
                title = "Power On Device",
                description = "Ensure your thermal camera device is powered on and in pairing mode."
            ),
            HelpStep(
                icon = Icons.Default.Wifi,
                title = "Enable WiFi",
                description = "Make sure WiFi is enabled on your mobile device for wireless connection.",
                isActionable = true,
                actionText = "Open WiFi Settings"
            ),
            HelpStep(
                icon = Icons.Default.Search,
                title = "Search for Device",
                description = "Use the device discovery feature to locate your thermal camera on the network."
            ),
            HelpStep(
                icon = Icons.Default.Link,
                title = "Establish Connection",
                description = "Select your device from the list and follow the pairing instructions."
            ),
            HelpStep(
                icon = Icons.Default.CheckCircle,
                title = "Verify Connection",
                description = "Once connected, you should see the device status as 'Connected' in the main screen."
            )
        )
    }

    private fun getDisconnectionHelpSteps(): List<HelpStep> {
        return listOf(
            HelpStep(
                icon = Icons.Default.Warning,
                title = "Connection Lost",
                description = "If your device disconnected unexpectedly, try the following steps to reconnect."
            ),
            HelpStep(
                icon = Icons.Default.Refresh,
                title = "Restart Device",
                description = "Power off your thermal camera device and turn it back on after 10 seconds."
            ),
            HelpStep(
                icon = Icons.Default.Wifi,
                title = "Check Network",
                description = "Ensure both devices are connected to the same WiFi network.",
                isActionable = true,
                actionText = "Open Network Settings"
            ),
            HelpStep(
                icon = Icons.Default.Bluetooth,
                title = "Reset Bluetooth",
                description = "If using Bluetooth connection, toggle Bluetooth off and on again.",
                isActionable = true,
                actionText = "Open Bluetooth Settings"
            ),
            HelpStep(
                icon = Icons.Default.RestartAlt,
                title = "Restart Application",
                description = "Close and reopen the IRCamera application to refresh all connections."
            )
        )
    }
}

@AndroidEntryPoint
class MoreHelpComposeActivity : ComponentActivity() {
    private lateinit var wifiManager: WifiManager
    private val viewModel: MoreHelpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionType = intent.getIntExtra(Constants.SETTING_CONNECTION_TYPE, 0)
        viewModel.setConnectionType(connectionType)
        setContent {
            IRCameraTheme {
                MoreHelpScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHelpScreen(
    viewModel: MoreHelpViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val connectionType by viewModel.connectionType
    val helpSteps by viewModel.helpSteps
    val title = if (connectionType == Constants.SETTING_CONNECTION) {
        stringResource(R.string.connection_help)
    } else {
        stringResource(R.string.disconnection_troubleshooting)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16131E))
    ) {
        TitleBar(
            title = title,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (connectionType == Constants.SETTING_CONNECTION)
                            Icons.AutoMirrored.Filled.Help else Icons.Default.BugReport,
                        contentDescription = if (connectionType == Constants.SETTING_CONNECTION)
                            "Connection Help" else "Troubleshooting",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (connectionType == Constants.SETTING_CONNECTION)
                            "Device Connection Guide" else "Troubleshooting Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (connectionType == Constants.SETTING_CONNECTION)
                            "Follow these steps to connect your thermal camera device"
                        else
                            "Steps to resolve connection issues",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            // Help steps
            helpSteps.forEachIndexed { index, step ->
                HelpStepCard(
                    step = step,
                    stepNumber = index + 1,
                    onActionClick = { action ->
                        when {
                            step.actionText.contains("WiFi") -> openWifiSettings()
                            step.actionText.contains("Network") -> openNetworkSettings()
                            step.actionText.contains("Bluetooth") -> openBluetoothSettings()
                            else -> action?.invoke()
                        }
                    }
                )
                if (index < helpSteps.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Additional help section
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
                            imageVector = Icons.AutoMirrored.Filled.ContactSupport,
                            contentDescription = "Contact Support",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Need More Help?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If you're still experiencing issues, check the device manual or contact technical support for additional assistance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
}

private fun openWifiSettings() {
    try {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    } catch (e: Exception) {
        showSettingsError("WiFi settings")
    }
}

private fun openNetworkSettings() {
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        startActivity(intent)
    } catch (e: Exception) {
        showSettingsError("Network settings")
    }
}

private fun openBluetoothSettings() {
    try {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    } catch (e: Exception) {
        showSettingsError("Bluetooth settings")
    }
}

private fun showSettingsError(settingType: String) {
    val tipDialogState = TipDialogState(this)
    tipDialogState.show(
        title = "",
        message = "Unable to open $settingType. Please access it manually from your device settings.",
        showCancel = false,
        positiveText = getString(R.string.app_got_it),
        onPositive = { }
    )
}
}

@Composable
private fun HelpStepCard(
    step: HelpStep,
    stepNumber: Int,
    onActionClick: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number and icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Step content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (step.isActionable && step.action != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onActionClick(step.action) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(step.actionText)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

