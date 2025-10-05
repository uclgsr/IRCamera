package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

/**
 * GSR Data Viewer Screen - Comprehensive GSR data analysis and visualization
 * Replaces GSRDataViewActivity with Compose implementation
 */
@Composable
fun GSRDataViewerScreen(
    sessionId: String = "GSR_Session_001",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Sample GSR data - in real app, this would come from ViewModel
    val gsrData = remember { generateSampleGSRData() }
    var selectedAnalysis by remember { mutableStateOf(AnalysisType.RAW_SIGNAL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Data Analysis",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Share,
                contentDescription = "Export Data",
                onClick = {
                    // TODO: Implement GSR data export functionality
                    android.widget.Toast.makeText(
                        androidx.compose.ui.platform.LocalContext.current,
                        "Exporting GSR data...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Info Card
            SessionInfoCard(sessionId = sessionId)

            // Analysis Type Selector
            AnalysisTypeSelector(
                selectedType = selectedAnalysis,
                onTypeSelected = { selectedAnalysis = it }
            )

            // Data Visualization
            when (selectedAnalysis) {
                AnalysisType.RAW_SIGNAL -> {
                    GSRSignalChart(
                        title = "Raw GSR Signal",
                        data = gsrData,
                        color = Color.Cyan
                    )
                }

                AnalysisType.FILTERED -> {
                    GSRSignalChart(
                        title = "Filtered Signal",
                        data = gsrData.map { it * 0.8f + 0.1f }, // Simple filter simulation
                        color = Color.Green
                    )
                }

                AnalysisType.FEATURES -> {
                    GSRFeaturesCard(gsrData)
                }

                AnalysisType.STATISTICS -> {
                    GSRStatisticsCard(gsrData)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SessionInfoCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Information",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Session ID:", color = Color.Gray, fontSize = 14.sp)
                Text(sessionId, color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration:", color = Color.Gray, fontSize = 14.sp)
                Text("5m 32s", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sample Rate:", color = Color.Gray, fontSize = 14.sp)
                Text("128 Hz", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnalysisTypeSelector(
    selectedType: AnalysisType,
    onTypeSelected: (AnalysisType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalysisType.entries.forEach { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = {
                            Text(
                                type.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRSignalChart(
    title: String,
    data: List<Float>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / data.size

                val path = Path()
                data.forEachIndexed { index, value ->
                    val x = index * pointSpacing
                    val y = height - (value * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw grid lines
                for (i in 0..4) {
                    val y = (height / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRFeaturesCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "GSR Features",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val features = listOf(
                "Skin Conductance Response (SCR) Count" to "23",
                "Average SCR Amplitude" to "0.15 μS",
                "Peak Detection" to "17 peaks",
                "Arousal Index" to "High",
                "Stress Level" to "Moderate"
            )

            features.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GSRStatisticsCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistical Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val stats = listOf(
                "Mean" to String.format("%.3f μS", data.average()),
                "Standard Deviation" to "0.045 μS",
                "Min Value" to String.format("%.3f μS", data.minOrNull() ?: 0f),
                "Max Value" to String.format("%.3f μS", data.maxOrNull() ?: 0f),
                "Range" to String.format(
                    "%.3f μS",
                    (data.maxOrNull() ?: 0f) - (data.minOrNull() ?: 0f)
                )
            )

            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

enum class AnalysisType(val displayName: String) {
    RAW_SIGNAL("Raw"),
    FILTERED("Filtered"),
    FEATURES("Features"),
    STATISTICS("Stats")
}

private fun generateSampleGSRData(): List<Float> {
    return (0..200).map { i ->
        0.5f + 0.3f * sin(i * 0.1).toFloat() + 0.1f * Random.nextFloat()
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRDataViewerScreenPreview() {
    IRCameraTheme {
        GSRDataViewerScreen()
    }
}