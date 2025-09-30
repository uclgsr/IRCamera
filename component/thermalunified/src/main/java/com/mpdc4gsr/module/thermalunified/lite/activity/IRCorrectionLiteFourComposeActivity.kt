package com.mpdc4gsr.module.thermalunified.lite.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Modern Compose implementation of IR Correction Lite Four functionality
 * Provides advanced thermal analysis with multi-point correction and isothermal analysis
 */
class IRCorrectionLiteFourComposeActivity : ComponentActivity() {

    private val viewModel: IRCorrectionLiteFourViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LibUnifiedTheme {
                IRCorrectionLiteFourScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

/**
 * ViewModel for IR Correction Lite Four with advanced thermal analysis
 */
class IRCorrectionLiteFourViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IRCorrectionFourUiState())
    val uiState: StateFlow<IRCorrectionFourUiState> = _uiState.asStateFlow()

    private val _analysisData = MutableStateFlow(ThermalAnalysisData())
    val analysisData: StateFlow<ThermalAnalysisData> = _analysisData.asStateFlow()

    private val _isothermalLines = MutableStateFlow<List<IsothermalLine>>(emptyList())
    val isothermalLines: StateFlow<List<IsothermalLine>> = _isothermalLines.asStateFlow()

    private val _analysisPoints = MutableStateFlow<List<AnalysisPoint>>(emptyList())
    val analysisPoints: StateFlow<List<AnalysisPoint>> = _analysisPoints.asStateFlow()

    init {
        initializeAnalysis()
    }

    fun startAdvancedAnalysis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)

            // Simulate advanced thermal analysis
            generateIsothermalLines()
            generateAnalysisPoints()
            calculateStatistics()

            kotlinx.coroutines.delay(1500) // Simulation delay

            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                analysisComplete = true,
                statusMessage = "Advanced thermal analysis completed"
            )
        }
    }

    fun addAnalysisPoint(x: Float, y: Float) {
        val currentPoints = _analysisPoints.value.toMutableList()
        val newPoint = AnalysisPoint(
            id = currentPoints.size + 1,
            x = x,
            y = y,
            temperature = calculateTemperatureAtPoint(x, y),
            label = "P${currentPoints.size + 1}"
        )
        currentPoints.add(newPoint)
        _analysisPoints.value = currentPoints

        updateStatistics()
    }

    fun removeAnalysisPoint(pointId: Int) {
        val updatedPoints = _analysisPoints.value.filter { it.id != pointId }
        _analysisPoints.value = updatedPoints
        updateStatistics()
    }

    fun updateIsothermalThreshold(temperature: Float) {
        generateIsothermalLines(temperature)
    }

    fun resetAnalysis() {
        _uiState.value = IRCorrectionFourUiState()
        _analysisPoints.value = emptyList()
        _isothermalLines.value = emptyList()
        initializeAnalysis()
    }

    private fun initializeAnalysis() {
        _analysisData.value = ThermalAnalysisData(
            minTemperature = 18.5f,
            maxTemperature = 42.8f,
            averageTemperature = 28.3f,
            standardDeviation = 5.2f
        )
    }

    private fun generateIsothermalLines(threshold: Float = 30f) {
        val lines = mutableListOf<IsothermalLine>()

        // Generate isothermal lines at different temperature levels
        for (temp in 20..40 step 5) {
            if (abs(temp - threshold) <= 2) {
                lines.add(
                    IsothermalLine(
                        temperature = temp.toFloat(),
                        points = generateLinePoints(temp.toFloat()),
                        color = when {
                            temp < 25 -> Color.Blue
                            temp < 35 -> Color.Green
                            else -> Color.Red
                        }
                    )
                )
            }
        }

        _isothermalLines.value = lines
    }

    private fun generateLinePoints(temperature: Float): List<androidx.compose.ui.geometry.Offset> {
        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()

        // Generate curved isothermal line based on temperature
        for (i in 0..100) {
            val x = i / 100f
            val y = 0.5f + 0.3f * sin(x * PI * 2 + temperature / 10).toFloat()
            points.add(androidx.compose.ui.geometry.Offset(x, y))
        }

        return points
    }

    private fun generateAnalysisPoints() {
        val points = listOf(
            AnalysisPoint(1, 0.2f, 0.3f, 24.5f, "Hot Spot 1"),
            AnalysisPoint(2, 0.7f, 0.4f, 36.2f, "Hot Spot 2"),
            AnalysisPoint(3, 0.5f, 0.8f, 19.8f, "Cold Spot"),
            AnalysisPoint(4, 0.3f, 0.6f, 28.1f, "Reference")
        )
        _analysisPoints.value = points
    }

    private fun calculateTemperatureAtPoint(x: Float, y: Float): Float {
        // Simulate temperature calculation based on position
        val baseTemp = 20f
        val variation = 20f * (x * y + 0.5f * sin(x * PI * 4).toFloat())
        return baseTemp + variation
    }

    private fun calculateStatistics() {
        val points = _analysisPoints.value
        if (points.isEmpty()) return

        val temps = points.map { it.temperature }
        val avg = temps.average().toFloat()
        val min = temps.minOrNull() ?: 0f
        val max = temps.maxOrNull() ?: 0f
        val stdDev = sqrt(temps.map { (it - avg).pow(2) }.average()).toFloat()

        _analysisData.value = _analysisData.value.copy(
            minTemperature = min,
            maxTemperature = max,
            averageTemperature = avg,
            standardDeviation = stdDev
        )
    }

    private fun updateStatistics() {
        calculateStatistics()
    }
}

/**
 * Main Compose screen for IR Correction Lite Four
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRCorrectionLiteFourScreen(
    viewModel: IRCorrectionLiteFourViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val analysisData by viewModel.analysisData.collectAsState()
    val isothermalLines by viewModel.isothermalLines.collectAsState()
    val analysisPoints by viewModel.analysisPoints.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "IR Correction Lite IV",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetAnalysis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                    IconButton(onClick = { /* Handle export */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analysis Status
            item {
                AnalysisStatusCard(
                    uiState = uiState,
                    analysisData = analysisData
                )
            }

            // Advanced Thermal Visualization
            item {
                AdvancedThermalVisualizationCard(
                    isothermalLines = isothermalLines,
                    analysisPoints = analysisPoints,
                    onPointAdded = viewModel::addAnalysisPoint,
                    isAnalyzing = uiState.isAnalyzing
                )
            }

            // Analysis Points List
            item {
                AnalysisPointsCard(
                    analysisPoints = analysisPoints,
                    onRemovePoint = viewModel::removeAnalysisPoint
                )
            }

            // Isothermal Controls
            item {
                IsothermalControlsCard(
                    onThresholdChanged = viewModel::updateIsothermalThreshold
                )
            }

            // Statistics Summary
            item {
                StatisticsSummaryCard(
                    analysisData = analysisData
                )
            }

            // Control Actions
            item {
                AdvancedControlActionsCard(
                    onStartAnalysis = { viewModel.startAdvancedAnalysis() },
                    onReset = { viewModel.resetAnalysis() },
                    isAnalyzing = uiState.isAnalyzing,
                    analysisComplete = uiState.analysisComplete
                )
            }
        }
    }
}

@Composable
fun AnalysisStatusCard(
    uiState: IRCorrectionFourUiState,
    analysisData: ThermalAnalysisData
) {
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
                    imageVector = when {
                        uiState.analysisComplete -> Icons.Default.Analytics
                        uiState.isAnalyzing -> Icons.Default.AutoGraph
                        else -> Icons.Default.Science
                    },
                    contentDescription = null,
                    tint = when {
                        uiState.analysisComplete -> Color.Green
                        uiState.isAnalyzing -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        uiState.analysisComplete -> "Advanced Analysis Complete"
                        uiState.isAnalyzing -> "Analyzing Thermal Data..."
                        else -> "Ready for Advanced Analysis"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (uiState.isAnalyzing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvancedThermalVisualizationCard(
    isothermalLines: List<IsothermalLine>,
    analysisPoints: List<AnalysisPoint>,
    onPointAdded: (Float, Float) -> Unit,
    isAnalyzing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Advanced Thermal Visualization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    AdvancedThermalCanvas(
                        isothermalLines = isothermalLines,
                        analysisPoints = analysisPoints,
                        onPointAdded = onPointAdded
                    )
                }
            }

            if (analysisPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${analysisPoints.size} analysis points active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvancedThermalCanvas(
    isothermalLines: List<IsothermalLine>,
    analysisPoints: List<AnalysisPoint>,
    onPointAdded: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawAdvancedThermalVisualization(
            drawScope = this,
            isothermalLines = isothermalLines,
            analysisPoints = analysisPoints
        )
    }
}

fun drawAdvancedThermalVisualization(
    drawScope: DrawScope,
    isothermalLines: List<IsothermalLine>,
    analysisPoints: List<AnalysisPoint>
) {
    with(drawScope) {
        val width = size.width
        val height = size.height

        // Draw thermal background gradient
        for (x in 0 until width.toInt() step 8) {
            for (y in 0 until height.toInt() step 8) {
                val normalizedX = x / width
                val normalizedY = y / height

                val temperature = 20f + (normalizedX * normalizedY * 25f) +
                        5f * sin(normalizedX * PI * 3).toFloat()

                val color = advancedTemperatureToColor(temperature)

                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()),
                    size = androidx.compose.ui.geometry.Size(8f, 8f)
                )
            }
        }

        // Draw isothermal lines
        isothermalLines.forEach { line ->
            drawIsothermalLine(line, width, height)
        }

        // Draw analysis points
        analysisPoints.forEach { point ->
            drawAnalysisPoint(point, width, height)
        }
    }
}

fun DrawScope.drawIsothermalLine(
    line: IsothermalLine,
    canvasWidth: Float,
    canvasHeight: Float
) {
    if (line.points.size < 2) return

    val scaledPoints = line.points.map { point ->
        androidx.compose.ui.geometry.Offset(
            point.x * canvasWidth,
            point.y * canvasHeight
        )
    }

    for (i in 0 until scaledPoints.size - 1) {
        drawLine(
            color = line.color,
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 3f
        )
    }
}

fun DrawScope.drawAnalysisPoint(
    point: AnalysisPoint,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val x = point.x * canvasWidth
    val y = point.y * canvasHeight

    // Draw point circle
    drawCircle(
        color = Color.White,
        radius = 12f,
        center = androidx.compose.ui.geometry.Offset(x, y)
    )

    drawCircle(
        color = Color.Red,
        radius = 8f,
        center = androidx.compose.ui.geometry.Offset(x, y)
    )

    // Draw temperature label
    // Note: Text drawing in Canvas would require more complex implementation
    // This is a simplified version
}

fun advancedTemperatureToColor(temperature: Float): Color {
    val normalized = (temperature - 15f) / 35f // 15-50°C range
    return when {
        normalized < 0.2f -> Color(0xFF000080) // Dark Blue
        normalized < 0.4f -> Color(0xFF0000FF) // Blue
        normalized < 0.6f -> Color(0xFF00FF00) // Green
        normalized < 0.8f -> Color(0xFFFFFF00) // Yellow
        else -> Color(0xFFFF0000) // Red
    }
}

@Composable
fun AnalysisPointsCard(
    analysisPoints: List<AnalysisPoint>,
    onRemovePoint: (Int) -> Unit
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
                Text(
                    text = "Analysis Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                if (analysisPoints.isNotEmpty()) {
                    Text(
                        text = "${analysisPoints.size} points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (analysisPoints.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap on the thermal image to add analysis points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(analysisPoints) { point ->
                        AnalysisPointChip(
                            point = point,
                            onRemove = { onRemovePoint(point.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisPointChip(
    point: AnalysisPoint,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${point.temperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun IsothermalControlsCard(
    onThresholdChanged: (Float) -> Unit
) {
    var threshold by remember { mutableFloatStateOf(30f) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Isothermal Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temperature Threshold",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${threshold.toInt()}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = threshold,
                onValueChange = {
                    threshold = it
                    onThresholdChanged(it)
                },
                valueRange = 15f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun StatisticsSummaryCard(
    analysisData: ThermalAnalysisData
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistical Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem(
                    label = "Min",
                    value = "${analysisData.minTemperature}°C",
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticItem(
                    label = "Max",
                    value = "${analysisData.maxTemperature}°C",
                    color = Color.Red
                )
                StatisticItem(
                    label = "Avg",
                    value = "${analysisData.averageTemperature}°C",
                    color = Color.Green
                )
                StatisticItem(
                    label = "StdDev",
                    value = "${analysisData.standardDeviation}°C",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdvancedControlActionsCard(
    onStartAnalysis: () -> Unit,
    onReset: () -> Unit,
    isAnalyzing: Boolean,
    analysisComplete: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartAnalysis,
                    enabled = !isAnalyzing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isAnalyzing) "Analyzing..." else "Start Analysis"
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    enabled = !isAnalyzing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            if (analysisComplete) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Handle export data */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Data")
                    }

                    OutlinedButton(
                        onClick = { /* Handle generate report */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }
                }
            }
        }
    }
}

// Data classes
data class IRCorrectionFourUiState(
    val isAnalyzing: Boolean = false,
    val analysisComplete: Boolean = false,
    val statusMessage: String = ""
)

data class ThermalAnalysisData(
    val minTemperature: Float = 15.0f,
    val maxTemperature: Float = 45.0f,
    val averageTemperature: Float = 30.0f,
    val standardDeviation: Float = 5.0f
)

data class IsothermalLine(
    val temperature: Float,
    val points: List<androidx.compose.ui.geometry.Offset>,
    val color: Color
)

data class AnalysisPoint(
    val id: Int,
    val x: Float,
    val y: Float,
    val temperature: Float,
    val label: String
)

@Preview(showBackground = true)
@Composable
fun IRCorrectionLiteFourPreview() {
    LibUnifiedTheme {
        // Preview implementation would go here
    }
}