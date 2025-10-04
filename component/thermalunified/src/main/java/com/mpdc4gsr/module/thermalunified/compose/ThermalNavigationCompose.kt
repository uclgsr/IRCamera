package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalNavigationDrawer(
    selectedDestination: ThermalDestination,
    onNavigate: (ThermalDestination) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thermal Imaging",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Professional Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Navigation items
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(ThermalDestination.values()) { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        selected = selectedDestination == destination,
                        onClick = {
                            onNavigate(destination)
                            onClose()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Footer
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Info, contentDescription = "About")
                }
            }
        }
    }
}

@Composable
fun ThermalActionMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onActionSelected: (ThermalAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Action items
        if (isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                ThermalAction.values().forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = action.title,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        SmallFloatingActionButton(
                            onClick = {
                                onActionSelected(action)
                                onToggle()
                            },
                            containerColor = action.color
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (isExpanded) "Close Menu" else "Open Menu"
            )
        }
    }
}

@Composable
fun ThermalBottomNavigation(
    destinations: List<ThermalDestination>,
    selectedDestination: ThermalDestination,
    onNavigate: (ThermalDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun ThermalMenuGrid(
    menuItems: List<ThermalMenuItem>,
    onItemClick: (ThermalMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(menuItems.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    ThermalMenuCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill empty space for odd number of items
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThermalMenuCard(
    item: ThermalMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(48.dp),
                tint = item.iconColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = item.textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = item.textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ThermalStatusBar(
    status: ThermalStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status.level) {
                ThermalStatusLevel.NORMAL -> MaterialTheme.colorScheme.surface
                ThermalStatusLevel.WARNING -> Color(0xFFFFF3E0)
                ThermalStatusLevel.CRITICAL -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (status.level) {
                            ThermalStatusLevel.NORMAL -> Color.Green
                            ThermalStatusLevel.WARNING -> Color(0xFFFF6600)
                            ThermalStatusLevel.CRITICAL -> Color.Red
                        }
                    )
            )

            // Status text
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Temperature display
            if (status.currentTemp != null) {
                Text(
                    text = "${String.format("%.1f", status.currentTemp)}°C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status.level) {
                        ThermalStatusLevel.NORMAL -> MaterialTheme.colorScheme.primary
                        ThermalStatusLevel.WARNING -> Color(0xFFFF8F00)
                        ThermalStatusLevel.CRITICAL -> Color(0xFFD32F2F)
                    }
                )
            }
        }
    }
}

// Data classes and enums

enum class ThermalDestination(
    val title: String,
    val icon: ImageVector
) {
    CAMERA("Camera", Icons.Default.CameraAlt),
    GALLERY("Gallery", Icons.Default.PhotoLibrary),
    ANALYSIS("Analysis", Icons.Default.Analytics),
    MEASUREMENT("Measurement", Icons.Default.Straighten),
    MONITOR("Monitor", Icons.Default.Monitor),
    REPORTS("Reports", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings)
}

enum class ThermalAction(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    CAPTURE("Capture", Icons.Default.CameraAlt, Color(0xFF4CAF50)),
    RECORD("Record", Icons.Default.Videocam, Color(0xFFFF5722)),
    MEASURE("Measure", Icons.Default.Straighten, Color(0xFF2196F3)),
    ANALYZE("Analyze", Icons.Default.Analytics, Color(0xFF9C27B0))
}

data class ThermalMenuItem(
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val backgroundColor: Color = Color.Transparent,
    val iconColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified
)

data class ThermalStatus(
    val message: String,
    val level: ThermalStatusLevel,
    val currentTemp: Float? = null
)

enum class ThermalStatusLevel {
    NORMAL, WARNING, CRITICAL
}

@Composable
fun ThermalNavigationPreview() {
    val sampleMenuItems = listOf(
        ThermalMenuItem(
            title = "Live Camera",
            subtitle = "Real-time",
            icon = Icons.Default.CameraAlt,
            backgroundColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1976D2),
            textColor = Color(0xFF1976D2)
        ),
        ThermalMenuItem(
            title = "Gallery",
            subtitle = "View saved",
            icon = Icons.Default.PhotoLibrary,
            backgroundColor = Color(0xFFE8F5E8),
            iconColor = Color(0xFF388E3C),
            textColor = Color(0xFF388E3C)
        ),
        ThermalMenuItem(
            title = "Analysis",
            subtitle = "Process data",
            icon = Icons.Default.Analytics,
            backgroundColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFFF8F00),
            textColor = Color(0xFFFF8F00)
        ),
        ThermalMenuItem(
            title = "Reports",
            subtitle = "Generate",
            icon = Icons.Default.Description,
            backgroundColor = Color(0xFFF3E5F5),
            iconColor = Color(0xFF7B1FA2),
            textColor = Color(0xFF7B1FA2)
        )
    )

    val sampleStatus = ThermalStatus(
        message = "Thermal camera connected and calibrated",
        level = ThermalStatusLevel.NORMAL,
        currentTemp = 25.4f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Navigation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        ThermalStatusBar(status = sampleStatus)

        Text("Menu Grid:", style = MaterialTheme.typography.titleMedium)
        ThermalMenuGrid(
            menuItems = sampleMenuItems,
            onItemClick = { },
            modifier = Modifier.weight(1f)
        )

        ThermalBottomNavigation(
            destinations = listOf(
                ThermalDestination.CAMERA,
                ThermalDestination.GALLERY,
                ThermalDestination.ANALYSIS,
                ThermalDestination.SETTINGS
            ),
            selectedDestination = ThermalDestination.CAMERA,
            onNavigate = { }
        )
    }
}