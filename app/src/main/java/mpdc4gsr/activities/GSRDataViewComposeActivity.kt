package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.ui.theme.IRCameraTheme
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.ui.components.CommonComponents
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

data class GSRDataPoint(
    val timestamp: Long,
    val conductance: Double,
    val resistance: Double,
    val quality: Int
) : Serializable

class GSRDataViewComposeActivity : BaseComposeActivity() {
    
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        
        fun startActivity(context: Context, filePath: String) {
            val intent = Intent(context, GSRDataViewComposeActivity::class.java)
            intent.putExtra(EXTRA_FILE_PATH, filePath)
            context.startActivity(intent)
        }
    }
    
    private lateinit var filePath: String
    private lateinit var file: File
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        file = File(filePath)
        
        if (!file.exists()) {
            finish()
            return
        }
        
        setContent {
            IRCameraTheme {
                Content()
            }
        }
    }
    
    @Composable
    override fun Content() {
        var gsrData by remember { mutableStateOf<List<GSRDataPoint>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var showChart by remember { mutableStateOf(false) }
        var statistics by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
        
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                try {
                    val data = withContext(Dispatchers.IO) {
                        loadGSRData()
                    }
                    gsrData = data
                    statistics = calculateStatistics(data)
                    isLoading = false
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading GSR data: ${e.message}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            }
        }
        
        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = file.name,
                    onNavigateBack = { finish() },
                    actions = {
                        IconButton(
                            onClick = { showChart = !showChart }
                        ) {
                            Icon(
                                imageVector = if (showChart) Icons.Default.List else Icons.Default.ShowChart,
                                contentDescription = if (showChart) "Show List" else "Show Chart"
                            )
                        }
                        
                        IconButton(
                            onClick = { exportData() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Data"
                            )
                        }
                        
                        IconButton(
                            onClick = { shareData() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Data"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // File Information Card
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
                            text = "File Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val fileSize = if (file.length() >= 1024 * 1024) {
                            "%.1f MB".format(file.length() / (1024.0 * 1024.0))
                        } else {
                            "%.1f KB".format(file.length() / 1024.0)
                        }
                        
                        Text("File: ${file.name}")
                        Text("Size: $fileSize")
                        Text("Records: ${gsrData.size}")
                        
                        if (gsrData.isNotEmpty()) {
                            val duration = (gsrData.last().timestamp - gsrData.first().timestamp) / 1000
                            Text("Duration: ${formatDuration(duration)}")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Statistics Card
                if (statistics.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("Mean Conductance: ${"%.6f".format(statistics["meanConductance"])} μS")
                            Text("Std Deviation: ${"%.6f".format(statistics["stdDev"])} μS")
                            Text("Min Value: ${"%.6f".format(statistics["minValue"])} μS")
                            Text("Max Value: ${"%.6f".format(statistics["maxValue"])} μS")
                            Text("Average Quality: ${"%.1f".format(statistics["avgQuality"])}%")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Content Area
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CommonComponents.LoadingIndicator("Loading GSR data...")
                    }
                } else if (gsrData.isEmpty()) {
                    CommonComponents.ErrorScreen(
                        message = "No GSR data found in this file",
                        onRetry = { finish() }
                    )
                } else {
                    if (showChart) {
                        // Chart View
                        Card(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AndroidView(
                                factory = { context ->
                                    LineChart(context).apply {
                                        setupChart(gsrData)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        // Data List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(gsrData) { dataPoint ->
                                GSRDataRow(dataPoint = dataPoint)
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun GSRDataRow(dataPoint: GSRDataPoint) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                            .format(Date(dataPoint.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.6f".format(dataPoint.conductance)} μS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Quality: ${dataPoint.quality}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            dataPoint.quality >= 80 -> Color(0xFF4CAF50)
                            dataPoint.quality >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = "${"%.2f".format(dataPoint.resistance)} kΩ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    private fun setupChart(data: List<GSRDataPoint>) {
        // Chart setup would be implemented here
        // This is a placeholder for the actual chart implementation
    }
    
    private fun loadGSRData(): List<GSRDataPoint> {
        // Simulate loading GSR data from file
        val random = Random()
        val baseTime = System.currentTimeMillis() - 300000 // 5 minutes ago
        
        return (0..299).map { i ->
            GSRDataPoint(
                timestamp = baseTime + (i * 1000), // 1 second intervals
                conductance = 2.5 + random.nextGaussian() * 0.5,
                resistance = 400.0 + random.nextGaussian() * 50.0,
                quality = 70 + random.nextInt(30)
            )
        }
    }
    
    private fun calculateStatistics(data: List<GSRDataPoint>): Map<String, Double> {
        if (data.isEmpty()) return emptyMap()
        
        val conductanceValues = data.map { it.conductance }
        val mean = conductanceValues.average()
        val variance = conductanceValues.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        
        return mapOf(
            "meanConductance" to mean,
            "stdDev" to stdDev,
            "minValue" to conductanceValues.minOrNull()!!,
            "maxValue" to conductanceValues.maxOrNull()!!,
            "avgQuality" to data.map { it.quality }.average()
        )
    }
    
    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }
    
    private fun exportData() {
        lifecycleScope.launch {
            try {
                val data = loadGSRData()
                val exportResult = withContext(Dispatchers.IO) {
                    exportGSRDataToFormats(data)
                }
                
                Toast.makeText(
                    this@GSRDataViewComposeActivity,
                    "Data exported successfully: $exportResult",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GSRDataViewComposeActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun shareData() {
        // Implement data sharing functionality
        Toast.makeText(this, "Share functionality not implemented yet", Toast.LENGTH_SHORT).show()
    }
    
    private fun exportGSRDataToFormats(data: List<GSRDataPoint>): String {
        val exportDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GSR_Exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseFileName = "gsr_data_$timestamp"
        
        // Export to CSV
        val csvFile = File(exportDir, "$baseFileName.csv")
        CSVWriter(FileWriter(csvFile)).use { writer ->
            writer.writeNext(arrayOf("Timestamp", "Conductance_uS", "Resistance_kOhm", "Quality_Percent"))
            data.forEach { point ->
                writer.writeNext(arrayOf(
                    point.timestamp.toString(),
                    point.conductance.toString(),
                    point.resistance.toString(),
                    point.quality.toString()
                ))
            }
        }
        
        // Export to JSON
        val jsonFile = File(exportDir, "$baseFileName.json")
        jsonFile.writeText(Gson().toJson(data))
        
        return exportDir.absolutePath
    }
}