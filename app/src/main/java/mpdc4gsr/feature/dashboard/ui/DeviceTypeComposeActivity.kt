package mpdc4gsr.feature.dashboard.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.R
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.config.RouterConfig
import com.mpdc4gsr.component.shared.app.navigation.NavigationManager
import mpdc4gsr.core.designsystem.AppBaseViewModel
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

enum class IRDeviceType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isTS004: Boolean,
) {
    TS004(
        "TS004 Thermal Camera",
        "High-precision thermal imaging device",
        Icons.Default.Thermostat,
        true,
    ),
    TC007(
        "TC007 Thermal Camera",
        "Compact thermal imaging solution",
        Icons.Default.CameraAlt,
        false,
    ),
}

class DeviceTypeViewModel : AppBaseViewModel() {
    private val _selectedDevice = mutableStateOf<IRDeviceType?>(null)
    val selectedDevice: State<IRDeviceType?> = _selectedDevice
    private val _availableDevices = mutableStateOf(IRDeviceType.values().toList())
    val availableDevices: State<List<IRDeviceType>> = _availableDevices

    fun selectDevice(device: IRDeviceType) {
        _selectedDevice.value = device
    }

    fun getDeviceList(): List<IRDeviceType> =
        listOf(
            IRDeviceType.TS004,
            IRDeviceType.TC007,
        )
}

class DeviceTypeComposeActivity : BaseComposeActivity<DeviceTypeViewModel>() {
    private val deviceTypeVM: DeviceTypeViewModel by viewModels()

    override fun createViewModel(): DeviceTypeViewModel = deviceTypeVM

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DeviceTypeViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val selectedDevice by viewModel.selectedDevice
            val devices = viewModel.getDeviceList()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF16131E)),
            ) {
                TitleBar(
                    title = stringResource(R.string.device_type_selection),
                    onBackClick = { finish() },
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                ) {
                    // Header section
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeviceHub,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select Device Type",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Choose the thermal camera device you want to connect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    // Device list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(devices) { device ->
                            DeviceTypeCard(
                                device = device,
                                isSelected = selectedDevice == device,
                                onDeviceClick = { selectedDevice ->
                                    viewModel.selectDevice(selectedDevice)
                                    // Navigate based on device type
                                    when (selectedDevice) {
                                        IRDeviceType.TS004 -> {
                                            NavigationManager
                                                .getInstance()
                                                .build(RouterConfig.IR_DEVICE_ADD)
                                                .withBoolean("isTS004", true)
                                                .navigation(context as DeviceTypeComposeActivity)
                                        }

                                        IRDeviceType.TC007 -> {
                                            NavigationManager
                                                .getInstance()
                                                .build(RouterConfig.IR_DEVICE_ADD)
                                                .withBoolean("isTS004", false)
                                                .navigation(context as DeviceTypeComposeActivity)
                                        }
                                    }
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Information section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Device Information",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Make sure your thermal camera device is powered on and ready for connection before proceeding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceTypeCard(
    device: IRDeviceType,
    isSelected: Boolean,
    onDeviceClick: (IRDeviceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onDeviceClick(device) },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 4.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Device icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
            ) {
                Icon(
                    imageVector = device.icon,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                    tint =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Device info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}



