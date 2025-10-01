package com.mpdc4gsr.module.thermalunified.lite.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

/**
 * Modern Compose version of IRMonitorLiteActivity
 * Provides real-time thermal monitoring with advanced analytics,
 * temperature tracking, and comprehensive data visualization
 */
class IRMonitorLiteComposeActivity : ComponentActivity() {

    private lateinit var viewModel: IRMonitorLiteViewModel

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        viewModel = ViewModelProvider(this)[IRMonitorLiteViewModel::class.java]

        setContent {
            LibUnifiedTheme {
                IRMonitorLiteScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToChart = { navigateToChart() }
                )
            }
        }

        viewModel.startMonitoring()
    }

    private fun navigateToChart() {
        // Navigate to chart activity
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopMonitoring()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRMonitorLiteScreen(
    viewModel: IRMonitorLiteViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChart: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thermal Monitor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChart) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Show Chart")
                    }
                    IconButton(onClick = { viewModel.toggleMonitoring() }) {
                        Icon(
                            imageVector = if (uiState.isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isMonitoring) "Pause" else "Resume"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main monitoring display
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Status header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Real-time Monitoring",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isMonitoring) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Status",
                                tint = if (uiState.isMonitoring) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isMonitoring) "ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.isMonitoring) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Current temperature display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Current Temperature",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${uiState.currentTemperature.format(2)}°C",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Temperature trend chart
                    Text(
                        text = "Temperature Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            drawTemperatureTrend(uiState.temperatureHistory)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resetData() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportData() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                }
            }

            // Statistics and alerts panel
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TemperatureStatisticsSection(
                            statistics = uiState.statistics
                        )
                    }

                    item {
                        MonitoringAlertsSection(
                            alerts = uiState.alerts,
                            onDismissAlert = { viewModel.dismissAlert(it) }
                        )
                    }

                    item {
                        MonitoringSettingsSection(
                            alertThreshold = uiState.alertThreshold,
                            onAlertThresholdChange = { viewModel.setAlertThreshold(it) },
                            samplingRate = uiState.samplingRate,
                            onSamplingRateChange = { viewModel.setSamplingRate(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemperatureStatisticsSection(
    statistics: TemperatureStatistics,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticRow(
                    label = "Average",
                    value = "${statistics.average.format(2)}°C"
                )
                StatisticRow(
                    label = "Maximum",
                    value = "${statistics.maximum.format(2)}°C",
                    valueColor = Color(0xFFFF5722)
                )
                StatisticRow(
                    label = "Minimum",
                    value = "${statistics.minimum.format(2)}°C",
                    valueColor = Color(0xFF2196F3)
                )
                StatisticRow(
                    label = "Standard Dev",
                    value = "${statistics.standardDeviation.format(2)}°C"
                )
                StatisticRow(
                    label = "Samples",
                    value = statistics.sampleCount.toString()
                )
            }
        }
    }
}

@Composable
fun StatisticRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun MonitoringAlertsSection(
    alerts: List<MonitoringAlert>,
    onDismissAlert: (MonitoringAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Alerts (${alerts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Card {
                Text(
                    text = "No active alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            alerts.forEach { alert ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (alert.severity) {
                            AlertSeverity.HIGH -> Color(0xFFFFEBEE)
                            AlertSeverity.MEDIUM -> Color(0xFFFFF3E0)
                            AlertSeverity.LOW -> Color(0xFFE8F5E8)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = alert.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onDismissAlert(alert) }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MonitoringSettingsSection(
    alertThreshold: Float,
    onAlertThresholdChange: (Float) -> Unit,
    samplingRate: Int,
    onSamplingRateChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Alert threshold
                Column {
                    Text(
                        text = "Alert Threshold: ${alertThreshold.format(1)}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = alertThreshold,
                        onValueChange = onAlertThresholdChange,
                        valueRange = 30f..80f
                    )
                }

                // Sampling rate
                Column {
                    Text(
                        text = "Sampling Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 5, 10).forEach { rate ->
                            FilterChip(
                                onClick = { onSamplingRateChange(rate) },
                                label = { Text("${rate}s") },
                                selected = samplingRate == rate
                            )
                        }
                    }
                }
            }
        }
    }
}

fun DrawScope.drawTemperatureTrend(temperatureHistory: List<Float>) {
    if (temperatureHistory.size < 2) return

    val width = size.width
    val height = size.height
    val pointSpacing = width / (temperatureHistory.size - 1)

    val minTemp = temperatureHistory.minOrNull() ?: 0f
    val maxTemp = temperatureHistory.maxOrNull() ?: 100f
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)

    // Draw grid lines
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    for (i in 0..4) {
        val y = height * i / 4
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Draw temperature line
    val path = androidx.compose.ui.graphics.Path()
    var started = false

    temperatureHistory.forEachIndexed { index, temp ->
        val x = index * pointSpacing
        val y = height - ((temp - minTemp) / tempRange * height)

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    // Draw gradient fill
    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2196F3).copy(alpha = 0.3f),
            Color.Transparent
        ),
        startY = 0f,
        endY = height
    )

    val fillPath = androidx.compose.ui.graphics.Path().apply {
        addPath(path)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }

    drawPath(
        path = fillPath,
        brush = gradient
    )

    // Draw line
    drawPath(
        path = path,
        color = Color(0xFF2196F3),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )

    // Draw data points
    temperatureHistory.forEachIndexed { index, temp ->
        val x = index * pointSpacing
        val y = height - ((temp - minTemp) / tempRange * height)

        drawCircle(
            color = Color(0xFF2196F3),
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(x, y)
        )

        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
}

fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

/**
 * ViewModel for IR Monitor Lite Compose Activity
 */
class IRMonitorLiteViewModel : ViewModel() {

    companion object {
        // Temperature simulation constants
        private const val BASE_TEMPERATURE = 25f
        private const val SIN_AMPLITUDE = 5f
        private const val COS_AMPLITUDE = 2f
        private const val SIN_FREQUENCY = 0.1
        private const val COS_FREQUENCY = 0.05
        private const val NOISE_RANGE = 1f
        private const val MAX_TEMPERATURE_HISTORY = 50
    }

    private val _uiState = MutableStateFlow(IRMonitorUiState())
    val uiState: StateFlow<IRMonitorUiState> = _uiState.asStateFlow()

    // Job reference for monitoring coroutine management
    private var monitoringJob: Job? = null

    fun startMonitoring() {
        // Cancel any existing monitoring job before starting a new one
        monitoringJob?.cancel()

        _uiState.value = _uiState.value.copy(isMonitoring = true)
        startTemperatureMonitoring()
    }

    fun stopMonitoring() {
        // Cancel the monitoring job when stopping
        monitoringJob?.cancel()
        monitoringJob = null

        _uiState.value = _uiState.value.copy(isMonitoring = false)
    }

    fun toggleMonitoring() {
        if (_uiState.value.isMonitoring) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    private fun startTemperatureMonitoring() {
        monitoringJob = viewModelScope.launch {
            var timeStep = 0

            while (_uiState.value.isMonitoring) {
                val currentState = _uiState.value

                // Simulate temperature reading with some variation using extracted constants
                val baseTemp =
                    BASE_TEMPERATURE + sin(timeStep * SIN_FREQUENCY).toFloat() * SIN_AMPLITUDE + cos(timeStep * COS_FREQUENCY).toFloat() * COS_AMPLITUDE
                val noise = Random.nextFloat() * (2 * NOISE_RANGE) - NOISE_RANGE
                val currentTemp = baseTemp + noise

                // Update temperature history
                val newHistory =
                    (currentState.temperatureHistory + listOf(currentTemp)).takeLast(MAX_TEMPERATURE_HISTORY)

                // Calculate statistics
                val newStatistics = calculateStatistics(newHistory)

                // Check for alerts
                val newAlerts = checkForAlerts(currentTemp, currentState.alertThreshold, currentState.alerts)

                _uiState.value = currentState.copy(
                    currentTemperature = currentTemp,
                    temperatureHistory = newHistory,
                    statistics = newStatistics,
                    alerts = newAlerts
                )

                timeStep++
                kotlinx.coroutines.delay((currentState.samplingRate * 1000).toLong())
            }
        }
    }

    private fun calculateStatistics(temperatures: List<Float>): TemperatureStatistics {
        if (temperatures.isEmpty()) {
            return TemperatureStatistics()
        }

        val average = temperatures.average().toFloat()
        val maximum = temperatures.maxOrNull() ?: 0f
        val minimum = temperatures.minOrNull() ?: 0f

        val variance = temperatures.map { (it - average) * (it - average) }.average().toFloat()
        val standardDeviation = kotlin.math.sqrt(variance.toDouble()).toFloat()

        return TemperatureStatistics(
            average = average,
            maximum = maximum,
            minimum = minimum,
            standardDeviation = standardDeviation,
            sampleCount = temperatures.size
        )
    }

    private fun checkForAlerts(
        currentTemp: Float,
        threshold: Float,
        existingAlerts: List<MonitoringAlert>
    ): List<MonitoringAlert> {
        val alerts = existingAlerts.toMutableList()

        if (currentTemp > threshold) {
            val alertMessage = "Temperature exceeded threshold: ${currentTemp.format(2)}°C > ${threshold.format(1)}°C"
            val newAlert = MonitoringAlert(
                id = System.currentTimeMillis(),
                message = alertMessage,
                severity = when {
                    currentTemp > threshold + 10 -> AlertSeverity.HIGH
                    currentTemp > threshold + 5 -> AlertSeverity.MEDIUM
                    else -> AlertSeverity.LOW
                },
                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )

            // Don't add duplicate alerts for similar temperatures
            if (!alerts.any {
                    it.message.contains("exceeded threshold") &&
                            System.currentTimeMillis() - it.id < 30000
                }) {
                alerts.add(newAlert)
            }
        }

        return alerts.takeLast(10) // Keep only recent alerts
    }

    fun resetData() {
        _uiState.value = _uiState.value.copy(
            temperatureHistory = emptyList(),
            statistics = TemperatureStatistics(),
            alerts = emptyList()
        )
    }

    fun exportData() {
        // Implement data export functionality
        viewModelScope.launch {
            // Export temperature history to CSV or other format
        }
    }

    fun dismissAlert(alert: MonitoringAlert) {
        _uiState.value = _uiState.value.copy(
            alerts = _uiState.value.alerts.filter { it.id != alert.id }
        )
    }

    fun setAlertThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(alertThreshold = threshold)
    }

    fun setSamplingRate(rate: Int) {
        _uiState.value = _uiState.value.copy(samplingRate = rate)
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel monitoring job when ViewModel is destroyed
        monitoringJob?.cancel()
    }
}

data class IRMonitorUiState(
    val isMonitoring: Boolean = false,
    val currentTemperature: Float = 25.0f,
    val temperatureHistory: List<Float> = emptyList(),
    val statistics: TemperatureStatistics = TemperatureStatistics(),
    val alerts: List<MonitoringAlert> = emptyList(),
    val alertThreshold: Float = 40.0f,
    val samplingRate: Int = 1
)

data class TemperatureStatistics(
    val average: Float = 0f,
    val maximum: Float = 0f,
    val minimum: Float = 0f,
    val standardDeviation: Float = 0f,
    val sampleCount: Int = 0
)

data class MonitoringAlert(
    val id: Long,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: String
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH
}

// Helper extension for range generation using Kotlin Random
fun ClosedRange<Float>.random(): Float {
    return start + Random.nextFloat() * (endInclusive - start)
}