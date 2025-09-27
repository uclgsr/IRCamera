package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme

data class Permission(
    val name: String,
    val description: String,
    val rationale: String,
    val icon: ImageVector,
    val status: PermissionStatus,
    val isRequired: Boolean = true,
    val category: PermissionCategory
)

enum class PermissionStatus {
    GRANTED, DENIED, NOT_REQUESTED, PERMANENTLY_DENIED
}

enum class PermissionCategory {
    CAMERA, STORAGE, LOCATION, BLUETOOTH, NETWORK, SENSORS
}

class PermissionManagerComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            PermissionManagerScreen()
        }
    }

    @Composable
    private fun PermissionManagerScreen() {
        var permissions by remember { mutableStateOf(generatePermissions()) }
        var selectedCategory by remember { mutableStateOf<PermissionCategory?>(null) }
        var showRationaleDialog by remember { mutableStateOf<Permission?>(null) }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "Permission Manager",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(onClick = { /* Refresh permissions */ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Permission summary
                PermissionSummaryCard(permissions = permissions)

                // Category filter
                CategoryFilter(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                // Permissions list
                val filteredPermissions = if (selectedCategory != null) {
                    permissions.filter { it.category == selectedCategory }
                } else {
                    permissions
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPermissions) { permission ->
                        PermissionCard(
                            permission = permission,
                            onToggle = { 
                                permissions = permissions.map {
                                    if (it.name == permission.name) {
                                        it.copy(
                                            status = if (it.status == PermissionStatus.GRANTED) 
                                                PermissionStatus.DENIED 
                                            else 
                                                PermissionStatus.GRANTED
                                        )
                                    } else it
                                }
                            },
                            onShowRationale = { showRationaleDialog = permission }
                        )
                    }
                }
            }
        }

        // Rationale dialog
        showRationaleDialog?.let { permission ->
            PermissionRationaleDialog(
                permission = permission,
                onDismiss = { showRationaleDialog = null },
                onGranted = {
                    permissions = permissions.map {
                        if (it.name == permission.name) {
                            it.copy(status = PermissionStatus.GRANTED)
                        } else it
                    }
                    showRationaleDialog = null
                }
            )
        }
    }

    @Composable
    private fun PermissionSummaryCard(permissions: List<Permission>) {
        val grantedCount = permissions.count { it.status == PermissionStatus.GRANTED }
        val requiredCount = permissions.count { it.isRequired }
        val totalCount = permissions.size

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (grantedCount == totalCount) 
                    MaterialTheme.colorScheme.primaryContainer
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Permission Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$grantedCount of $totalCount permissions granted",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Icon(
                        if (grantedCount == totalCount) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (grantedCount == totalCount) Color.Green else Color.Orange
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = grantedCount.toFloat() / totalCount.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (grantedCount < requiredCount) {
                    Text(
                        text = "${requiredCount - grantedCount} required permissions missing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "All required permissions granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
            }
        }
    }

    @Composable
    private fun CategoryFilter(
        selectedCategory: PermissionCategory?,
        onCategorySelected: (PermissionCategory?) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(null) + PermissionCategory.values().toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { 
                        Text(category?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All")
                    },
                    leadingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }

    @Composable
    private fun PermissionCard(
        permission: Permission,
        onToggle: () -> Unit,
        onShowRationale: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            permission.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = permission.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = permission.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        PermissionStatusChip(permission.status)
                        
                        if (permission.isRequired) {
                            Text(
                                text = "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onShowRationale
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Why needed?")
                    }
                    
                    when (permission.status) {
                        PermissionStatus.NOT_REQUESTED -> {
                            Button(onClick = onToggle) {
                                Text("Request")
                            }
                        }
                        PermissionStatus.DENIED -> {
                            Button(onClick = onToggle) {
                                Text("Grant")
                            }
                        }
                        PermissionStatus.PERMANENTLY_DENIED -> {
                            OutlinedButton(onClick = { /* Open settings */ }) {
                                Text("Settings")
                            }
                        }
                        PermissionStatus.GRANTED -> {
                            Button(
                                onClick = onToggle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Revoke")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionStatusChip(status: PermissionStatus) {
        val (color, text) = when (status) {
            PermissionStatus.GRANTED -> Color.Green to "Granted"
            PermissionStatus.DENIED -> Color.Orange to "Denied"
            PermissionStatus.NOT_REQUESTED -> Color.Gray to "Not Requested"
            PermissionStatus.PERMANENTLY_DENIED -> Color.Red to "Permanently Denied"
        }

        Surface(
            color = color,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }

    @Composable
    private fun PermissionRationaleDialog(
        permission: Permission,
        onDismiss: () -> Unit,
        onGranted: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    permission.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(permission.name)
            },
            text = {
                Column {
                    Text(
                        text = permission.rationale,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (permission.isRequired) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This permission is required for the app to function properly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onGranted) {
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

    private fun generatePermissions(): List<Permission> {
        return listOf(
            Permission(
                name = "Camera",
                description = "Access camera for thermal and RGB recording",
                rationale = "The app needs camera access to capture thermal images and record RGB video for research sessions. This is essential for the core functionality of the IRCamera application.",
                icon = Icons.Default.Camera,
                status = PermissionStatus.GRANTED,
                isRequired = true,
                category = PermissionCategory.CAMERA
            ),
            Permission(
                name = "Storage",
                description = "Read and write files to device storage",
                rationale = "Storage access is required to save recorded sessions, GSR data, thermal images, and export data files. Without this permission, the app cannot store your research data.",
                icon = Icons.Default.Storage,
                status = PermissionStatus.GRANTED,
                isRequired = true,
                category = PermissionCategory.STORAGE
            ),
            Permission(
                name = "Bluetooth",
                description = "Connect to GSR sensors and other devices",
                rationale = "Bluetooth permission is needed to connect to Shimmer GSR sensors and other Bluetooth-enabled research equipment. This enables multi-modal data collection.",
                icon = Icons.Default.Bluetooth,
                status = PermissionStatus.DENIED,
                isRequired = true,
                category = PermissionCategory.BLUETOOTH
            ),
            Permission(
                name = "Location",
                description = "Access device location for context data",
                rationale = "Location access helps provide contextual information for research sessions and is required for some Bluetooth device discovery on newer Android versions.",
                icon = Icons.Default.LocationOn,
                status = PermissionStatus.NOT_REQUESTED,
                isRequired = false,
                category = PermissionCategory.LOCATION
            ),
            Permission(
                name = "Network",
                description = "Access network for PC controller connection",
                rationale = "Network access enables communication with PC controllers and remote data synchronization. This allows for coordinated multi-device research sessions.",
                icon = Icons.Default.Wifi,
                status = PermissionStatus.GRANTED,
                isRequired = true,
                category = PermissionCategory.NETWORK
            ),
            Permission(
                name = "Microphone",
                description = "Record audio during sessions",
                rationale = "Microphone access allows recording of audio annotations and environmental sounds during research sessions for comprehensive data collection.",
                icon = Icons.Default.Mic,
                status = PermissionStatus.PERMANENTLY_DENIED,
                isRequired = false,
                category = PermissionCategory.SENSORS
            )
        )
    }
}