package mpdc4gsr.feature.gsr.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
data class SessionInfo(
    val id: String,
    val title: String,
    val date: String,
    val duration: String,
    val participantId: String,
    val sensorTypes: List<String>,
    val dataPoints: Int,
    val notes: String
)
data class SessionMetrics(
    val gsrMean: Double,
    val gsrStd: Double,
    val gsrMin: Double,
    val gsrMax: Double,
    val thermalMean: Double,
    val thermalStd: Double,
    val heartRateAvg: Int,
    val stressLevel: String
)
data class TimeSeriesData(
    val timestamp: Long,
    val gsrValue: Double,
    val thermalValue: Double
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String = "SESSION-2024-001",
    onNavigateBack: () -> Unit = {},
    onExportSession: () -> Unit = {},
    onPlayVideo: () -> Unit = {}
) {
    val session = remember { getSampleSession(sessionId) }
    val metrics = remember { getSampleMetrics() }
    val timeSeriesData = remember { getSampleTimeSeriesData() }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Session Details",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Share,
                    contentDescription = "Export session",
                    onClick = onExportSession
                )
                TitleBarAction(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    onClick = onPlayVideo
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session Header
                item {
                    SessionHeaderCard(session = session)
                }
                // Metrics Overview
                item {
                    MetricsOverviewCard(metrics = metrics)
                }
                // GSR Waveform
                item {
                    GSRWaveformCard(data = timeSeriesData)
                }
                // Thermal Data
                item {
                    ThermalDataCard(data = timeSeriesData)
                }
                // Analysis Summary
                item {
                    AnalysisSummaryCard(session = session, metrics = metrics)
                }
                // Export Options
                item {
                    ExportOptionsCard(
                        onExportRaw = { onExportSession() },
                        onExportReport = { onExportSession() },
                        onExportVideo = { onPlayVideo() }
                    )
                }
            }
        }
    }
}
@Composable
fun SessionHeaderCard(session: SessionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = session.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Date",
                    value = session.date
                )
                SessionInfoItem(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = session.duration
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.Person,
                    label = "Participant",
                    value = session.participantId
                )
                SessionInfoItem(
                    icon = Icons.Default.DataUsage,
                    label = "Data Points",
                    value = session.dataPoints.toString()
                )
            }
            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}
@Composable
fun SessionInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF4ECDC4),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFFCCFFFFFF)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
@Composable
fun MetricsOverviewCard(metrics: SessionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Metrics Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "GSR Mean",
                    value = "${String.format("%.2f", metrics.gsrMean)} μS",
                    color = Color(0xFF4ECDC4)
                )
                MetricItem(
                    label = "Thermal Avg",
                    value = "${String.format("%.1f", metrics.thermalMean)}°C",
                    color = Color(0xFFFF6B6B)
                )
                MetricItem(
                    label = "Heart Rate",
                    value = "${metrics.heartRateAvg} BPM",
                    color = Color(0xFF6B73FF)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stress Level: ",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
                Text(
                    text = metrics.stressLevel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (metrics.stressLevel) {
                        "Low" -> Color(0xFF4ECDC4)
                        "Medium" -> Color(0xFFFFB74D)
                        "High" -> Color(0xFFFF6B6B)
                        else -> Color.White
                    }
                )
            }
        }
    }
}
@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}
@Composable
fun GSRWaveformCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "GSR Waveform",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.gsrValue }
                    val maxValue = data.maxOf { it.gsrValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.gsrValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
@Composable
fun ThermalDataCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Thermal Data",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.thermalValue }
                    val maxValue = data.maxOf { it.thermalValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.thermalValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6B6B),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
@Composable
fun AnalysisSummaryCard(
    session: SessionInfo,
    metrics: SessionMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Analysis Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val summaryText = buildString {
                append("This ${session.duration} session recorded ${session.dataPoints} data points ")
                append("from ${session.sensorTypes.joinToString(", ")} sensors. ")
                append("Average GSR was ${String.format("%.2f", metrics.gsrMean)} μS with ")
                append("standard deviation of ${String.format("%.2f", metrics.gsrStd)}. ")
                append(
                    "Thermal readings averaged ${
                        String.format(
                            "%.1f",
                            metrics.thermalMean
                        )
                    }°C. "
                )
                append("Overall stress level assessed as ${metrics.stressLevel.lowercase()}.")
            }
            Text(
                text = summaryText,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
        }
    }
}
@Composable
fun ExportOptionsCard(
    onExportRaw: () -> Unit,
    onExportReport: () -> Unit,
    onExportVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Export Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onExportRaw,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4ECDC4)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Export Raw Data",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raw Data")
                }
                OutlinedButton(
                    onClick = onExportReport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B73FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
                OutlinedButton(
                    onClick = onExportVideo,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = "Export Video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video")
                }
            }
        }
    }
}
private fun getSampleSession(id: String) = SessionInfo(
    id = id,
    title = "Stress Response Study - Session A",
    date = "Dec 15, 2024 14:30",
    duration = "25:42",
    participantId = "P001-UCL-2024",
    sensorTypes = listOf("GSR", "Thermal", "Heart Rate"),
    dataPoints = 15420,
    notes = "Baseline recording with cognitive stress tasks. Participant reported feeling moderately stressed during math problems."
)
private fun getSampleMetrics() = SessionMetrics(
    gsrMean = 12.45,
    gsrStd = 3.21,
    gsrMin = 8.12,
    gsrMax = 18.67,
    thermalMean = 36.4,
    thermalStd = 0.8,
    heartRateAvg = 78,
    stressLevel = "Medium"
)
private fun getSampleTimeSeriesData(): List<TimeSeriesData> {
    return (0..100).map { i ->
        TimeSeriesData(
            timestamp = System.currentTimeMillis() + i * 1000,
            gsrValue = 12.0 + 3.0 * sin(i * 0.1) + (Math.random() - 0.5) * 2.0,
            thermalValue = 36.4 + 0.5 * sin(i * 0.05) + (Math.random() - 0.5) * 0.3
        )
    }
}
@Preview(showBackground = true)
@Composable
fun SessionDetailScreenPreview() {
    SessionDetailScreen()
}