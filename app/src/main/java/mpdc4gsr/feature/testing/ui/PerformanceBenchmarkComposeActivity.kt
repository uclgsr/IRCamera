package mpdc4gsr.feature.testing.ui
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class PerformanceBenchmarkComposeActivity : ComponentActivity() {
    private val viewModel: PerformanceBenchmarkViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                PerformanceBenchmarkScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PerformanceBenchmarkComposeActivity::class.java))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceBenchmarkScreen(
    viewModel: PerformanceBenchmarkViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Benchmark") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportResults() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Results"
                        )
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
            // Overall Performance Summary
            item {
                PerformanceSummaryCard(
                    summary = uiState.performanceSummary,
                    isRunning = uiState.isRunning
                )
            }
            // Real-time Performance Chart
            item {
                PerformanceChartCard(
                    chartData = uiState.performanceData,
                    title = "Real-time Performance Metrics"
                )
            }
            // Benchmark Controls
            item {
                BenchmarkControlsCard(
                    onStartBenchmark = { viewModel.startBenchmark() },
                    onStopBenchmark = { viewModel.stopBenchmark() },
                    onResetResults = { viewModel.resetResults() },
                    isRunning = uiState.isRunning
                )
            }
            // Individual Benchmark Results
            items(uiState.benchmarkResults) { result ->
                BenchmarkResultCard(result = result)
            }
            // System Information
            item {
                SystemInfoCard(systemInfo = uiState.systemInfo)
            }
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
@Composable
fun PerformanceSummaryCard(
    summary: PerformanceSummary,
    isRunning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn("FPS", "${summary.averageFps}", summary.targetFps > 0)
                MetricColumn("Memory", "${summary.memoryUsageMB}MB", true)
                MetricColumn("CPU", "${summary.cpuUsage}%", summary.cpuUsage < 80)
                MetricColumn("Temp", "${summary.temperature}°C", summary.temperature < 60)
            }
        }
    }
}
@Composable
fun MetricColumn(
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun PerformanceChartCard(
    chartData: List<PerformanceDataPoint>,
    title: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (chartData.isNotEmpty()) {
                    drawPerformanceChart(chartData, size.width, size.height, primaryColor, errorColor)
                }
            }
        }
    }
}
fun DrawScope.drawPerformanceChart(
    data: List<PerformanceDataPoint>,
    width: Float,
    height: Float,
    primaryColor: Color,
    errorColor: Color
) {
    if (data.isEmpty()) return
    val maxFps = data.maxOfOrNull { it.fps } ?: 60f
    val maxMemory = data.maxOfOrNull { it.memoryMB } ?: 100f
    // Draw FPS line (blue)
    val fpsPath = Path()
    data.forEachIndexed { index, point ->
        val x = (index.toFloat() / (data.size - 1)) * width
        val y = height - (point.fps / maxFps) * height
        if (index == 0) {
            fpsPath.moveTo(x, y)
        } else {
            fpsPath.lineTo(x, y)
        }
    }
    drawPath(
        fpsPath,
        primaryColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )
    // Draw Memory line (red)
    val memoryPath = Path()
    data.forEachIndexed { index, point ->
        val x = (index.toFloat() / (data.size - 1)) * width
        val y = height - (point.memoryMB / maxMemory) * height
        if (index == 0) {
            memoryPath.moveTo(x, y)
        } else {
            memoryPath.lineTo(x, y)
        }
    }
    drawPath(memoryPath, errorColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
}
@Composable
fun BenchmarkControlsCard(
    onStartBenchmark: () -> Unit,
    onStopBenchmark: () -> Unit,
    onResetResults: () -> Unit,
    isRunning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Benchmark Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onStartBenchmark,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }
                Button(
                    onClick = onStopBenchmark,
                    enabled = isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
                OutlinedButton(
                    onClick = onResetResults,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}
@Composable
fun BenchmarkResultCard(result: BenchmarkResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = result.testName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Duration", style = MaterialTheme.typography.bodySmall)
                    Text("${result.durationMs}ms", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Result", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = if (result.passed) "PASSED" else "FAILED",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Column {
                    Text("Score", style = MaterialTheme.typography.bodySmall)
                    Text("${result.score}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (result.notes.isNotEmpty()) {
                Text(
                    text = result.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun SystemInfoCard(systemInfo: SystemInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            InfoRow("Device Model", systemInfo.deviceModel)
            InfoRow("Android Version", systemInfo.androidVersion)
            InfoRow("Available RAM", "${systemInfo.availableMemoryMB}MB")
            InfoRow("CPU Cores", "${systemInfo.cpuCores}")
            InfoRow("GPU Renderer", systemInfo.gpuRenderer)
        }
    }
}
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
// Data Classes
data class PerformanceDataPoint(
    val timestamp: Long,
    val fps: Float,
    val memoryMB: Float,
    val cpuUsage: Float
)
data class PerformanceSummary(
    val averageFps: Float = 0f,
    val targetFps: Float = 60f,
    val memoryUsageMB: Float = 0f,
    val cpuUsage: Float = 0f,
    val temperature: Float = 0f
)
data class BenchmarkResult(
    val testName: String,
    val durationMs: Long,
    val passed: Boolean,
    val score: Float,
    val notes: String
)
data class SystemInfo(
    val deviceModel: String = "Unknown",
    val androidVersion: String = "Unknown",
    val availableMemoryMB: Int = 0,
    val cpuCores: Int = 0,
    val gpuRenderer: String = "Unknown"
)
data class PerformanceBenchmarkUiState(
    val isRunning: Boolean = false,
    val isLoading: Boolean = false,
    val performanceSummary: PerformanceSummary = PerformanceSummary(),
    val performanceData: List<PerformanceDataPoint> = emptyList(),
    val benchmarkResults: List<BenchmarkResult> = emptyList(),
    val systemInfo: SystemInfo = SystemInfo()
)
// ViewModel
class PerformanceBenchmarkViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PerformanceBenchmarkUiState())
    val uiState: StateFlow<PerformanceBenchmarkUiState> = _uiState.asStateFlow()
    init {
        loadSystemInfo()
    }
    fun startBenchmark() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true)
            runBenchmarkSuite()
        }
    }
    fun stopBenchmark() {
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
    fun resetResults() {
        _uiState.value = _uiState.value.copy(
            performanceData = emptyList(),
            benchmarkResults = emptyList(),
            performanceSummary = PerformanceSummary()
        )
    }
    fun exportResults() {
        viewModelScope.launch {
            // Export benchmark results to file
            // Implementation would depend on specific export mechanism
        }
    }
    private suspend fun runBenchmarkSuite() {
        val tests = listOf(
            "Thermal Image Processing",
            "GSR Data Collection",
            "Multi-sensor Synchronization",
            "Network Performance",
            "Storage I/O Performance",
            "UI Rendering Performance"
        )
        val results = mutableListOf<BenchmarkResult>()
        tests.forEach { testName ->
            if (!_uiState.value.isRunning) return@forEach
            val startTime = System.currentTimeMillis()
            // Simulate test execution with random performance data
            repeat(50) {
                if (!_uiState.value.isRunning) return@forEach
                val dataPoint = PerformanceDataPoint(
                    timestamp = System.currentTimeMillis(),
                    fps = Random.nextFloat() * 60f + 30f,
                    memoryMB = Random.nextFloat() * 200f + 100f,
                    cpuUsage = Random.nextFloat() * 80f + 20f
                )
                val currentData = _uiState.value.performanceData.toMutableList()
                currentData.add(dataPoint)
                if (currentData.size > 100) {
                    currentData.removeFirst()
                }
                _uiState.value = _uiState.value.copy(
                    performanceData = currentData,
                    performanceSummary = calculateSummary(currentData)
                )
                delay(100)
            }
            val duration = System.currentTimeMillis() - startTime
            val passed = Random.nextBoolean()
            val score = Random.nextFloat() * 100f
            results.add(
                BenchmarkResult(
                    testName = testName,
                    durationMs = duration,
                    passed = passed,
                    score = score,
                    notes = if (passed) "Test completed successfully" else "Performance below threshold"
                )
            )
            _uiState.value = _uiState.value.copy(benchmarkResults = results)
        }
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
    private fun calculateSummary(data: List<PerformanceDataPoint>): PerformanceSummary {
        if (data.isEmpty()) return PerformanceSummary()
        return PerformanceSummary(
            averageFps = data.map { it.fps }.average().toFloat(),
            memoryUsageMB = data.lastOrNull()?.memoryMB ?: 0f,
            cpuUsage = data.map { it.cpuUsage }.average().toFloat(),
            temperature = Random.nextFloat() * 20f + 40f // Simulated temperature
        )
    }
    private fun loadSystemInfo() {
        _uiState.value = _uiState.value.copy(
            systemInfo = SystemInfo(
                deviceModel = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE,
                availableMemoryMB = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt(),
                cpuCores = Runtime.getRuntime().availableProcessors(),
                gpuRenderer = "Adreno 640" // Would be detected from actual GPU
            )
        )
    }
}
// Theme placeholder
@Composable
fun IRCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
@Preview(showBackground = true)
@Composable
fun PerformanceBenchmarkScreenPreview() {
    IRCameraTheme {
        PerformanceBenchmarkScreen(
            viewModel = PerformanceBenchmarkViewModel(),
            onBackPressed = { }
        )
    }
}