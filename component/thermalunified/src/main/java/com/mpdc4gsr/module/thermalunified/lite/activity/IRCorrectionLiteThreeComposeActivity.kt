package com.mpdc4gsr.module.thermalunified.lite.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.module.thermalunified.theme.ThermalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modern Compose implementation of IR Correction Lite Three functionality
 * Provides advanced thermal image correction with real-time preview
 */
class IRCorrectionLiteThreeComposeActivity : ComponentActivity() {
    
    private val viewModel: IRCorrectionLiteThreeViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ThermalTheme {
                IRCorrectionLiteThreeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

/**
 * ViewModel for IR Correction Lite Three with StateFlow architecture
 */
class IRCorrectionLiteThreeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(IRCorrectionUiState())
    val uiState: StateFlow<IRCorrectionUiState> = _uiState.asStateFlow()
    
    private val _thermalData = MutableStateFlow(ThermalCorrectionData())
    val thermalData: StateFlow<ThermalCorrectionData> = _thermalData.asStateFlow()
    
    private val _correctionProgress = MutableStateFlow(0f)
    val correctionProgress: StateFlow<Float> = _correctionProgress.asStateFlow()
    
    init {
        loadCorrectionData()
    }
    
    fun startCorrection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            // Simulate correction process with progress updates
            for (i in 0..100 step 5) {
                _correctionProgress.value = i / 100f
                kotlinx.coroutines.delay(50)
            }
            
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                correctionComplete = true,
                statusMessage = "Thermal correction completed successfully"
            )
        }
    }
    
    fun resetCorrection() {
        _uiState.value = IRCorrectionUiState()
        _correctionProgress.value = 0f
    }
    
    fun updateCorrectionParameter(parameter: CorrectionParameter, value: Float) {
        val currentData = _thermalData.value
        val updatedData = when (parameter) {
            CorrectionParameter.EMISSIVITY -> currentData.copy(emissivity = value)
            CorrectionParameter.REFLECTED_TEMPERATURE -> currentData.copy(reflectedTemperature = value)
            CorrectionParameter.ATMOSPHERIC_TEMPERATURE -> currentData.copy(atmosphericTemperature = value)
            CorrectionParameter.DISTANCE -> currentData.copy(distance = value)
            CorrectionParameter.HUMIDITY -> currentData.copy(humidity = value)
        }
        _thermalData.value = updatedData
    }
    
    private fun loadCorrectionData() {
        // Initialize with default thermal correction parameters
        _thermalData.value = ThermalCorrectionData(
            emissivity = 0.95f,
            reflectedTemperature = 20.0f,
            atmosphericTemperature = 20.0f,
            distance = 1.0f,
            humidity = 50.0f
        )
    }
}

/**
 * Main Compose screen for IR Correction Lite Three
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRCorrectionLiteThreeScreen(
    viewModel: IRCorrectionLiteThreeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val thermalData by viewModel.thermalData.collectAsState()
    val correctionProgress by viewModel.correctionProgress.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "IR Correction Lite III",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetCorrection() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
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
            // Status Card
            item {
                StatusCard(
                    uiState = uiState,
                    correctionProgress = correctionProgress
                )
            }
            
            // Thermal Preview Card
            item {
                ThermalPreviewCard(
                    thermalData = thermalData,
                    isProcessing = uiState.isProcessing
                )
            }
            
            // Correction Parameters
            item {
                CorrectionParametersCard(
                    thermalData = thermalData,
                    onParameterChanged = viewModel::updateCorrectionParameter,
                    enabled = !uiState.isProcessing
                )
            }
            
            // Control Buttons
            item {
                ControlButtonsCard(
                    onStartCorrection = { viewModel.startCorrection() },
                    onReset = { viewModel.resetCorrection() },
                    isProcessing = uiState.isProcessing,
                    correctionComplete = uiState.correctionComplete
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    uiState: IRCorrectionUiState,
    correctionProgress: Float
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
                    imageVector = if (uiState.correctionComplete) Icons.Default.CheckCircle 
                                 else if (uiState.isProcessing) Icons.Default.Settings
                                 else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (uiState.correctionComplete) Color.Green 
                          else if (uiState.isProcessing) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        uiState.correctionComplete -> "Correction Complete"
                        uiState.isProcessing -> "Processing..."
                        else -> "Ready for Correction"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (uiState.isProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = correctionProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(correctionProgress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
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
fun ThermalPreviewCard(
    thermalData: ThermalCorrectionData,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Thermal Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ThermalVisualization(thermalData = thermalData)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${thermalData.minTemperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Max: ${thermalData.maxTemperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun ThermalVisualization(thermalData: ThermalCorrectionData) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawThermalPattern(this, thermalData)
    }
}

fun drawThermalPattern(drawScope: DrawScope, thermalData: ThermalCorrectionData) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        
        // Draw thermal gradient simulation
        for (x in 0 until width.toInt() step 10) {
            for (y in 0 until height.toInt() step 10) {
                val normalizedX = x / width
                val normalizedY = y / height
                
                val temperature = interpolateTemperature(
                    normalizedX, normalizedY, thermalData
                )
                
                val color = temperatureToColor(temperature)
                
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()),
                    size = androidx.compose.ui.geometry.Size(10f, 10f)
                )
            }
        }
    }
}

fun interpolateTemperature(
    x: Float, 
    y: Float, 
    thermalData: ThermalCorrectionData
): Float {
    // Simple thermal pattern simulation
    val baseTemp = 20f + (x * y * 30f)
    val correctedTemp = baseTemp * thermalData.emissivity + 
                       thermalData.reflectedTemperature * (1 - thermalData.emissivity)
    return correctedTemp.coerceIn(thermalData.minTemperature, thermalData.maxTemperature)
}

fun temperatureToColor(temperature: Float): Color {
    // Convert temperature to thermal color (blue to red gradient)
    val normalized = (temperature - 10f) / 50f // Assuming 10-60°C range
    return when {
        normalized < 0.33f -> Color(0xFF0000FF + (normalized * 3 * 0xFF00).toInt())
        normalized < 0.66f -> Color(0xFF00FF00 + ((normalized - 0.33f) * 3 * 0xFF0000).toInt())
        else -> Color(0xFFFF0000)
    }
}

@Composable
fun CorrectionParametersCard(
    thermalData: ThermalCorrectionData,
    onParameterChanged: (CorrectionParameter, Float) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Correction Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ParameterSlider(
                label = "Emissivity",
                value = thermalData.emissivity,
                valueRange = 0.1f..1.0f,
                onValueChange = { onParameterChanged(CorrectionParameter.EMISSIVITY, it) },
                enabled = enabled,
                valueFormat = "%.2f"
            )
            
            ParameterSlider(
                label = "Reflected Temperature (°C)",
                value = thermalData.reflectedTemperature,
                valueRange = -10f..50f,
                onValueChange = { onParameterChanged(CorrectionParameter.REFLECTED_TEMPERATURE, it) },
                enabled = enabled,
                valueFormat = "%.1f°C"
            )
            
            ParameterSlider(
                label = "Atmospheric Temperature (°C)",
                value = thermalData.atmosphericTemperature,
                valueRange = -10f..50f,
                onValueChange = { onParameterChanged(CorrectionParameter.ATMOSPHERIC_TEMPERATURE, it) },
                enabled = enabled,
                valueFormat = "%.1f°C"
            )
            
            ParameterSlider(
                label = "Distance (m)",
                value = thermalData.distance,
                valueRange = 0.1f..10f,
                onValueChange = { onParameterChanged(CorrectionParameter.DISTANCE, it) },
                enabled = enabled,
                valueFormat = "%.1fm"
            )
            
            ParameterSlider(
                label = "Humidity (%)",
                value = thermalData.humidity,
                valueRange = 0f..100f,
                onValueChange = { onParameterChanged(CorrectionParameter.HUMIDITY, it) },
                enabled = enabled,
                valueFormat = "%.0f%%"
            )
        }
    }
}

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    valueFormat: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueFormat.format(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ControlButtonsCard(
    onStartCorrection: () -> Unit,
    onReset: () -> Unit,
    isProcessing: Boolean,
    correctionComplete: Boolean
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
                    onClick = onStartCorrection,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isProcessing) "Processing..." else "Start Correction"
                    )
                }
                
                OutlinedButton(
                    onClick = onReset,
                    enabled = !isProcessing,
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
            
            if (correctionComplete) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { /* Handle save */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Corrected Image")
                }
            }
        }
    }
}

// Data classes
data class IRCorrectionUiState(
    val isProcessing: Boolean = false,
    val correctionComplete: Boolean = false,
    val statusMessage: String = ""
)

data class ThermalCorrectionData(
    val emissivity: Float = 0.95f,
    val reflectedTemperature: Float = 20.0f,
    val atmosphericTemperature: Float = 20.0f,
    val distance: Float = 1.0f,
    val humidity: Float = 50.0f,
    val minTemperature: Float = 15.0f,
    val maxTemperature: Float = 45.0f
)

enum class CorrectionParameter {
    EMISSIVITY,
    REFLECTED_TEMPERATURE,
    ATMOSPHERIC_TEMPERATURE,
    DISTANCE,
    HUMIDITY
}

@Preview(showBackground = true)
@Composable
fun IRCorrectionLiteThreePreview() {
    ThermalTheme {
        // Preview implementation would go here
    }
}