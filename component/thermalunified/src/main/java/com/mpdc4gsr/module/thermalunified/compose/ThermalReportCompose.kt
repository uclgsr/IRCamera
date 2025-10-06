package com.mpdc4gsr.module.thermalunified.compose
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
// Data classes for report components
data class ReportData(
    val id: String,
    val title: String,
    val description: String,
    val createdDate: String,
    val modifiedDate: String,
    val images: List<String>,
    val measurements: List<MeasurementData>,
    val metadata: ReportMetadata
)
data class MeasurementData(
    val id: Int,
    val name: String,
    val value: String,
    val unit: String,
    val type: MeasurementType,
    val isEditable: Boolean = true
)
data class ReportMetadata(
    val author: String,
    val location: String,
    val equipment: String,
    val conditions: String,
    val notes: String
)
enum class MeasurementType {
    TEMPERATURE, HUMIDITY, PRESSURE, DISTANCE, EMISSIVITY
}
data class WatermarkData(
    val text: String,
    val position: WatermarkPosition,
    val opacity: Float = 0.3f,
    val fontSize: Float = 14f,
    val color: Color = Color.Gray
)
enum class WatermarkPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIRInputCompose(
    reportData: ReportData,
    onReportUpdated: (ReportData) -> Unit,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentReport by remember { mutableStateOf(reportData) }
    LaunchedEffect(reportData) {
        currentReport = reportData
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header
        item {
            ReportHeaderCompose(
                report = currentReport,
                onTitleChanged = { newTitle ->
                    currentReport = currentReport.copy(title = newTitle)
                    onReportUpdated(currentReport)
                },
                onDescriptionChanged = { newDescription ->
                    currentReport = currentReport.copy(description = newDescription)
                    onReportUpdated(currentReport)
                }
            )
        }
        // Images section
        item {
            ReportImagesCompose(
                images = currentReport.images,
                onImageAdded = onImageAdded,
                onImageRemoved = onImageRemoved
            )
        }
        // Measurements section
        item {
            ReportMeasurementsCompose(
                measurements = currentReport.measurements,
                onMeasurementUpdated = { updatedMeasurement ->
                    val updatedMeasurements = currentReport.measurements.map { measurement ->
                        if (measurement.id == updatedMeasurement.id) updatedMeasurement else measurement
                    }
                    currentReport = currentReport.copy(measurements = updatedMeasurements)
                    onReportUpdated(currentReport)
                }
            )
        }
        // Metadata section
        item {
            ReportMetadataCompose(
                metadata = currentReport.metadata,
                onMetadataUpdated = { updatedMetadata ->
                    currentReport = currentReport.copy(metadata = updatedMetadata)
                    onReportUpdated(currentReport)
                }
            )
        }
    }
}
@Composable
private fun ReportHeaderCompose(
    report: ReportData,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = report.title,
                onValueChange = onTitleChanged,
                label = { Text("Report Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = report.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created: ${report.createdDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "Modified: ${report.modifiedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
@Composable
private fun ReportImagesCompose(
    images: List<String>,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = "Images (${images.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { onImageAdded("") }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { imagePath ->
                        ReportImageItem(
                            imagePath = imagePath,
                            onRemoved = { onImageRemoved(imagePath) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onImageAdded("") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ImageSearch,
                            contentDescription = "Add images",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to add images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun ReportImageItem(
    imagePath: String,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imagePath)
                .crossfade(true)
                .build(),
            contentDescription = "Report image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemoved,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
                    .padding(4.dp)
            )
        }
    }
}
@Composable
private fun ReportMeasurementsCompose(
    measurements: List<MeasurementData>,
    onMeasurementUpdated: (MeasurementData) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Measurements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            measurements.forEach { measurement ->
                MeasurementItemCompose(
                    measurement = measurement,
                    onUpdated = onMeasurementUpdated
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
private fun MeasurementItemCompose(
    measurement: MeasurementData,
    onUpdated: (MeasurementData) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(measurement.value) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (measurement.type) {
                MeasurementType.TEMPERATURE -> Icons.Default.Thermostat
                MeasurementType.HUMIDITY -> Icons.Default.WaterDrop
                MeasurementType.PRESSURE -> Icons.Default.Speed
                MeasurementType.DISTANCE -> Icons.Default.Straighten
                MeasurementType.EMISSIVITY -> Icons.Default.Opacity
            },
            contentDescription = measurement.type.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = measurement.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (measurement.isEditable) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    value = newValue
                    onUpdated(measurement.copy(value = newValue))
                },
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = measurement.unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
@Composable
private fun ReportMetadataCompose(
    metadata: ReportMetadata,
    onMetadataUpdated: (ReportMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMetadata by remember { mutableStateOf(metadata) }
    LaunchedEffect(metadata) {
        currentMetadata = metadata
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Report Metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = currentMetadata.author,
                onValueChange = { newAuthor ->
                    currentMetadata = currentMetadata.copy(author = newAuthor)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Author")
                }
            )
            OutlinedTextField(
                value = currentMetadata.location,
                onValueChange = { newLocation ->
                    currentMetadata = currentMetadata.copy(location = newLocation)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location")
                }
            )
            OutlinedTextField(
                value = currentMetadata.equipment,
                onValueChange = { newEquipment ->
                    currentMetadata = currentMetadata.copy(equipment = newEquipment)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Equipment") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Build, contentDescription = "Equipment")
                }
            )
            OutlinedTextField(
                value = currentMetadata.conditions,
                onValueChange = { newConditions ->
                    currentMetadata = currentMetadata.copy(conditions = newConditions)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Conditions") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = "Conditions")
                }
            )
            OutlinedTextField(
                value = currentMetadata.notes,
                onValueChange = { newNotes ->
                    currentMetadata = currentMetadata.copy(notes = newNotes)
                    onMetadataUpdated(currentMetadata)
                },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes")
                }
            )
        }
    }
}
@Composable
fun ReportIRShowCompose(
    reportData: ReportData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = reportData.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reportData.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Created: ${reportData.createdDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Modified: ${reportData.modifiedDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        // Images display
        item {
            if (reportData.images.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Images (${reportData.images.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(reportData.images) { imagePath ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imagePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Report image",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
        // Measurements display (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    reportData.measurements.forEach { measurement ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (measurement.type) {
                                    MeasurementType.TEMPERATURE -> Icons.Default.Thermostat
                                    MeasurementType.HUMIDITY -> Icons.Default.WaterDrop
                                    MeasurementType.PRESSURE -> Icons.Default.Speed
                                    MeasurementType.DISTANCE -> Icons.Default.Straighten
                                    MeasurementType.EMISSIVITY -> Icons.Default.Opacity
                                },
                                contentDescription = measurement.type.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = measurement.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${measurement.value} ${measurement.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        // Metadata display (read-only)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Report Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    MetadataRow("Author", reportData.metadata.author, Icons.Default.Person)
                    MetadataRow("Location", reportData.metadata.location, Icons.Default.LocationOn)
                    MetadataRow("Equipment", reportData.metadata.equipment, Icons.Default.Build)
                    MetadataRow("Conditions", reportData.metadata.conditions, Icons.Default.Cloud)
                    if (reportData.metadata.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = reportData.metadata.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    if (value.isNotEmpty()) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun WatermarkCompose(
    watermarkData: WatermarkData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = when (watermarkData.position) {
            WatermarkPosition.TOP_LEFT -> Alignment.TopStart
            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
            WatermarkPosition.CENTER -> Alignment.Center
        }
    ) {
        Text(
            text = watermarkData.text,
            color = watermarkData.color.copy(alpha = watermarkData.opacity),
            fontSize = watermarkData.fontSize.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}
// Preview functions
@Preview(showBackground = true)
@Composable
private fun ReportIRInputPreview() {
    LibUnifiedTheme {
        val sampleReport = ReportData(
            id = "1",
            title = "Thermal Analysis Report",
            description = "Comprehensive thermal analysis of equipment",
            createdDate = "2023-12-01",
            modifiedDate = "2023-12-02",
            images = listOf("image1.jpg", "image2.jpg"),
            measurements = listOf(
                MeasurementData(1, "Max Temperature", "85.5", "°C", MeasurementType.TEMPERATURE),
                MeasurementData(2, "Humidity", "45", "%", MeasurementType.HUMIDITY)
            ),
            metadata = ReportMetadata(
                author = "John Doe",
                location = "Factory Floor A",
                equipment = "FLIR T640",
                conditions = "Ambient 20°C",
                notes = "Regular inspection"
            )
        )
        ReportIRInputCompose(
            reportData = sampleReport,
            onReportUpdated = {},
            onImageAdded = {},
            onImageRemoved = {}
        )
    }
}