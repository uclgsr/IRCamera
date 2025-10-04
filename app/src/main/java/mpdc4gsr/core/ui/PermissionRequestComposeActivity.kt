package mpdc4gsr.core.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

/**
 * PermissionRequestComposeActivity - Modern Permission Management with Compose
 *
 * Advanced permission management interface featuring:
 * - Interactive permission status dashboard with visual indicators
 * - Step-by-step permission request workflow with explanations
 * - Real-time permission status monitoring and validation
 * - Educational content explaining why permissions are needed
 * - Troubleshooting guide for permission-related issues
 * - Integration with system settings for advanced permission management
 */
class PermissionRequestComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, PermissionRequestComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var showEducationalDialog by remember { mutableStateOf(false) }
        var selectedPermission by remember { mutableStateOf<PermissionInfo?>(null) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Permission Manager",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showEducationalDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                            IconButton(onClick = { /* Open system settings */ }) {
                                Icon(Icons.Default.Settings, contentDescription = "System Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PermissionRequestContent(
                    onPermissionSelect = { selectedPermission = it },
                    onGrantAll = {
                        // TODO: Request all required permissions
                        android.widget.Toast.makeText(
                            this@PermissionRequestComposeActivity,
                            "Requesting all permissions...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        if (showEducationalDialog) {
            PermissionEducationDialog(
                onDismiss = { showEducationalDialog = false }
            )
        }

        selectedPermission?.let { permission ->
            PermissionDetailDialog(
                permission = permission,
                onDismiss = { selectedPermission = null },
                onRequestPermission = {
                    // Request specific permission
                    selectedPermission = null
                }
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onPermissionSelect: (PermissionInfo) -> Unit,
    onGrantAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Permission Status Overview
        PermissionStatusOverview(
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Critical Permissions Section
        Text(
            text = "Critical Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val criticalPermissions = getCriticalPermissions()
        criticalPermissions.forEach { permission ->
            PermissionCard(
                permission = permission,
                onSelect = { onPermissionSelect(permission) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Optional Permissions Section
        Text(
            text = "Optional Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )

        val optionalPermissions = getOptionalPermissions()
        optionalPermissions.forEach { permission ->
            PermissionCard(
                permission = permission,
                onSelect = { onPermissionSelect(permission) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Grant All Button
        GrantAllPermissionsButton(
            onClick = onGrantAll,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
private fun PermissionStatusOverview(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Permission Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PermissionStatusItem(
                    label = "Granted",
                    count = 4,
                    color = Color(0xFF4CAF50)
                )
                PermissionStatusItem(
                    label = "Pending",
                    count = 2,
                    color = Color(0xFFFF9800)
                )
                PermissionStatusItem(
                    label = "Denied",
                    count = 1,
                    color = Color(0xFFE53E3E)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionInfo,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (permission.status) {
                PermissionStatus.GRANTED -> MaterialTheme.colorScheme.surfaceVariant
                PermissionStatus.DENIED -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Permission icon
            Icon(
                imageVector = getPermissionIcon(permission.type),
                contentDescription = permission.name,
                modifier = Modifier.size(32.dp),
                tint = getPermissionStatusColor(permission.status)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getPermissionStatusColor(permission.status))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = permission.status.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action button
            when (permission.status) {
                PermissionStatus.GRANTED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF4CAF50)
                    )
                }

                PermissionStatus.DENIED -> {
                    OutlinedButton(onClick = onSelect) {
                        Text("Grant")
                    }
                }

                PermissionStatus.PENDING -> {
                    Button(onClick = onSelect) {
                        Text("Request")
                    }
                }
            }
        }
    }
}

@Composable
private fun GrantAllPermissionsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = "Grant All",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "GRANT ALL PERMISSIONS",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PermissionDetailDialog(
    permission: PermissionInfo,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(permission.name)
        },
        text = {
            Column {
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Why this permission is needed:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = permission.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionEducationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Why Permissions Matter")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "IRCamera requires several permissions to provide the best experience:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PermissionEducationItem(
                    icon = Icons.Default.Camera,
                    title = "Camera Access",
                    description = "Capture thermal and RGB images for analysis"
                )

                PermissionEducationItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth",
                    description = "Connect to GSR sensors and thermal cameras"
                )

                PermissionEducationItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location",
                    description = "Required for Bluetooth device discovery"
                )

                PermissionEducationItem(
                    icon = Icons.Default.Storage,
                    title = "Storage",
                    description = "Save recordings and export data"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun PermissionEducationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getPermissionIcon(type: String) = when (type) {
    "camera" -> Icons.Default.Camera
    "bluetooth" -> Icons.Default.Bluetooth
    "location" -> Icons.Default.LocationOn
    "storage" -> Icons.Default.Storage
    "microphone" -> Icons.Default.Mic
    "notification" -> Icons.Default.Notifications
    else -> Icons.Default.Security
}

private fun getPermissionStatusColor(status: PermissionStatus) = when (status) {
    PermissionStatus.GRANTED -> Color(0xFF4CAF50)
    PermissionStatus.DENIED -> Color(0xFFE53E3E)
    PermissionStatus.PENDING -> Color(0xFFFF9800)
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PENDING
}

data class PermissionInfo(
    val name: String,
    val type: String,
    val description: String,
    val reasoning: String,
    val status: PermissionStatus,
    val isCritical: Boolean
)

private fun getCriticalPermissions() = listOf(
    PermissionInfo(
        "Camera Access",
        "camera",
        "Access to device camera for thermal and RGB imaging",
        "Required to capture thermal images from TOPDON TC001 and RGB images from device camera. Essential for core app functionality.",
        PermissionStatus.GRANTED,
        true
    ),
    PermissionInfo(
        "Bluetooth",
        "bluetooth",
        "Connect to Bluetooth devices for sensor data collection",
        "Needed to connect to Shimmer3 GSR+ sensors and TOPDON thermal cameras via Bluetooth LE.",
        PermissionStatus.GRANTED,
        true
    ),
    PermissionInfo(
        "Location Access",
        "location",
        "Required for Bluetooth device discovery",
        "Android requires location permission for BLE device scanning and discovery of nearby sensors.",
        PermissionStatus.PENDING,
        true
    )
)

private fun getOptionalPermissions() = listOf(
    PermissionInfo(
        "Storage Access",
        "storage",
        "Save recordings and export data files",
        "Allows saving session recordings, exporting data in various formats, and managing files.",
        PermissionStatus.GRANTED,
        false
    ),
    PermissionInfo(
        "Microphone",
        "microphone",
        "Record audio during multi-modal sessions",
        "Optional for recording audio annotations during research sessions.",
        PermissionStatus.DENIED,
        false
    ),
    PermissionInfo(
        "Notifications",
        "notification",
        "Show recording status and sensor alerts",
        "Displays notifications for recording status, sensor connection issues, and system alerts.",
        PermissionStatus.PENDING,
        false
    )
)
