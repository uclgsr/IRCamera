package mpdc4gsr.presentation.screens.thermal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.ui.components.TitleBar
import mpdc4gsr.ui.components.TitleBarAction
import mpdc4gsr.ui.theme.IRCameraTheme

@Composable
fun AnnotateScreen(
    onBackClick: (() -> Unit)? = null,
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sample annotation data - will be replaced with actual measurement data
    val annotations = remember {
        listOf(
            ThermalAnnotation.Point(Offset(0.3f, 0.4f), 45.2f),
            ThermalAnnotation.Point(Offset(0.7f, 0.6f), 18.9f),
            ThermalAnnotation.Line(
                start = Offset(0.2f, 0.2f),
                end = Offset(0.8f, 0.2f),
                maxTemp = 42.1f,
                minTemp = 35.8f
            )
        )
    }
    var reportInfo by remember {
        mutableStateOf(
            ReportInfo(
                title = "Thermal Analysis Report",
                notes = "Temperature measurement of equipment",
                location = "Lab Room 1",
                timestamp = "2024-01-15 14:30:00"
            )
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e)) // Match reference background
    ) {
        // Title bar with save and share actions
        TitleBar(
            title = "Preview", // Match reference report preview title
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Share,
                contentDescription = "Share report",
                onClick = onShare
            )
            TitleBarAction(
                icon = Icons.Default.Save,
                contentDescription = "Save report",
                onClick = onSave
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thermal image with annotations
            ThermalImageWithAnnotations(
                annotations = annotations,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
            // Report information panel
            ReportInfoPanel(
                reportInfo = reportInfo,
                onInfoChanged = { reportInfo = it }
            )
            // Measurement summary
            MeasurementSummary(annotations = annotations)
            // Watermark preview area
            WatermarkPreview()
        }
    }
}

@Composable
private fun ThermalImageWithAnnotations(
    annotations: List<ThermalAnnotation>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Enhanced thermal image with realistic thermal imaging display
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw sample thermal background
                drawRect(
                    color = Color(0xFF1A1A2E),
                    size = size
                )
                // Draw thermal patterns
                drawCircle(
                    color = Color.Red,
                    radius = 40f,
                    center = Offset(size.width * 0.3f, size.height * 0.4f)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 30f,
                    center = Offset(size.width * 0.7f, size.height * 0.6f)
                )
                // Draw annotations
                annotations.forEach { annotation ->
                    drawAnnotation(annotation, size.width, size.height)
                }
            }
        }
    }
}

private fun DrawScope.drawAnnotation(
    annotation: ThermalAnnotation,
    imageWidth: Float,
    imageHeight: Float
) {
    when (annotation) {
        is ThermalAnnotation.Point -> {
            val center = Offset(
                annotation.position.x * imageWidth,
                annotation.position.y * imageHeight
            )
            // Draw crosshair
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x - 15f, center.y),
                end = Offset(center.x + 15f, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x, center.y - 15f),
                end = Offset(center.x, center.y + 15f),
                strokeWidth = 2.dp.toPx()
            )
            // Draw temperature circle
            drawCircle(
                color = Color.Yellow,
                radius = 8f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        is ThermalAnnotation.Line -> {
            val start = Offset(
                annotation.start.x * imageWidth,
                annotation.start.y * imageHeight
            )
            val end = Offset(
                annotation.end.x * imageWidth,
                annotation.end.y * imageHeight
            )
            drawLine(
                color = Color.Green,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx()
            )
            // Draw end points
            drawCircle(color = Color.Green, radius = 6f, center = start)
            drawCircle(color = Color.Green, radius = 6f, center = end)
        }

        is ThermalAnnotation.Rectangle -> {
            val topLeft = Offset(
                annotation.topLeft.x * imageWidth,
                annotation.topLeft.y * imageHeight
            )
            val size = androidx.compose.ui.geometry.Size(
                (annotation.bottomRight.x - annotation.topLeft.x) * imageWidth,
                (annotation.bottomRight.y - annotation.topLeft.y) * imageHeight
            )
            drawRect(
                color = Color.Cyan,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun ReportInfoPanel(
    reportInfo: ReportInfo,
    onInfoChanged: (ReportInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Report Information",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = reportInfo.title,
                onValueChange = { onInfoChanged(reportInfo.copy(title = it)) },
                label = { Text("Title", color = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // Focus moves to notes field
                    }
                )
            )
            OutlinedTextField(
                value = reportInfo.notes,
                onValueChange = { onInfoChanged(reportInfo.copy(notes = it)) },
                label = { Text("Notes", color = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                ),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Location: ${reportInfo.location}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = reportInfo.timestamp,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MeasurementSummary(
    annotations: List<ThermalAnnotation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Measurement Summary",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            annotations.forEach { annotation ->
                when (annotation) {
                    is ThermalAnnotation.Point -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Point", color = Color.Yellow, fontSize = 14.sp)
                            Text(
                                "${annotation.temperature}°C",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    is ThermalAnnotation.Line -> {
                        Column {
                            Text("Line Measurement", color = Color.Green, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Max: ${annotation.maxTemp}°C",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Min: ${annotation.minTemp}°C",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    is ThermalAnnotation.Rectangle -> {
                        Column {
                            Text("Area Measurement", color = Color.Cyan, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Avg: ${annotation.avgTemp}°C",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Max: ${annotation.maxTemp}°C",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun WatermarkPreview(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Watermark",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Watermark preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IRCamera - Thermal Analysis",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

sealed class ThermalAnnotation {
    data class Point(
        val position: Offset,
        val temperature: Float
    ) : ThermalAnnotation()

    data class Line(
        val start: Offset,
        val end: Offset,
        val maxTemp: Float,
        val minTemp: Float
    ) : ThermalAnnotation()

    data class Rectangle(
        val topLeft: Offset,
        val bottomRight: Offset,
        val maxTemp: Float,
        val minTemp: Float,
        val avgTemp: Float
    ) : ThermalAnnotation()
}

data class ReportInfo(
    val title: String,
    val notes: String,
    val location: String,
    val timestamp: String
)

@Preview(showBackground = true)
@Composable
private fun AnnotateScreenPreview() {
    IRCameraTheme {
        AnnotateScreen()
    }
}