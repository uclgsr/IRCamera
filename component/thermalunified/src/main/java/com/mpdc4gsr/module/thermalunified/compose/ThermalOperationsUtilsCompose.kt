package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thermal operations and utilities in Compose
 * Modern replacements for thermal utility functions and configurations
 */

/**
 * IR command and configuration utilities
 */
object ThermalOperationsCompose {
    
    /**
     * IR configuration panel
     */
    @Composable
    fun IRConfigurationPanel(
        currentConfig: IRConfig,
        onConfigChange: (IRConfig) -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "IR Camera Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display number configuration
                ConfigSliderRow(
                    label = "Display Number",
                    value = currentConfig.displayNumber.toFloat(),
                    valueRange = 10f..60f,
                    onValueChange = { 
                        onConfigChange(currentConfig.copy(displayNumber = it.toInt()))
                    },
                    valueFormatter = { "${it.toInt()}" }
                )
                
                // Fusion type selection
                ConfigDropdownRow(
                    label = "Fusion Type",
                    options = FusionType.values().map { it.displayName },
                    selectedIndex = currentConfig.fusionType.ordinal,
                    onSelectionChange = { index ->
                        onConfigChange(currentConfig.copy(fusionType = FusionType.values()[index]))
                    }
                )
                
                // Camera parameters
                ConfigSliderRow(
                    label = "VL Camera Width",
                    value = currentConfig.vlCameraWidth.toFloat(),
                    valueRange = 640f..1920f,
                    onValueChange = { 
                        onConfigChange(currentConfig.copy(vlCameraWidth = it.toInt()))
                    },
                    valueFormatter = { "${it.toInt()}px" }
                )
                
                ConfigSliderRow(
                    label = "VL Camera Height", 
                    value = currentConfig.vlCameraHeight.toFloat(),
                    valueRange = 480f..1080f,
                    onValueChange = { 
                        onConfigChange(currentConfig.copy(vlCameraHeight = it.toInt()))
                    },
                    valueFormatter = { "${it.toInt()}px" }
                )
                
                // IR processing options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use IR ISP",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = currentConfig.useIRISP,
                        onCheckedChange = { 
                            onConfigChange(currentConfig.copy(useIRISP = it))
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Calibration data display
     */
    @Composable
    fun CalibrationDataPanel(
        calibrationData: CalibrationData,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Calibration Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                CalibrationInfoRow("Product Type", calibrationData.productType)
                CalibrationInfoRow("Serial Number", calibrationData.serialNumber)
                CalibrationInfoRow("Calibration Status", 
                    if (calibrationData.isCalibrated) "Calibrated" else "Not Calibrated")
                CalibrationInfoRow("Temperature Range", 
                    "${calibrationData.minTemp}°C - ${calibrationData.maxTemp}°C")
                
                if (calibrationData.isCalibrated) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Green
                        )
                        Text(
                            text = "Calibration Valid",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Thermal data processing controls
     */
    @Composable
    fun ThermalProcessingControls(
        processingState: ProcessingState,
        onProcessingChange: (ProcessingState) -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Thermal Processing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Temperature range
                Text(
                    text = "Temperature Range",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                RangeSlider(
                    value = processingState.tempRange,
                    onValueChange = { 
                        onProcessingChange(processingState.copy(tempRange = it))
                    },
                    valueRange = -40f..200f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "${String.format("%.1f", processingState.tempRange.start)}°C - ${String.format("%.1f", processingState.tempRange.endInclusive)}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Palette selection
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyColumn(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ThermalPalette.values()) { palette ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onProcessingChange(processingState.copy(palette = palette))
                                }
                                .background(
                                    if (processingState.palette == palette) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Palette preview (simplified)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        palette.primaryColor,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = palette.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { it.toString() },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ConfigDropdownRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = options.getOrNull(selectedIndex) ?: "",
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelectionChange(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun CalibrationInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    
    Spacer(modifier = Modifier.height(4.dp))
}

// Data classes for thermal operations

data class IRConfig(
    val displayNumber: Int = 30,
    val fusionType: FusionType = FusionType.IR_ONLY,
    val vlCameraWidth: Int = 1280,
    val vlCameraHeight: Int = 720,
    val useIRISP: Boolean = false
)

enum class FusionType(val displayName: String) {
    IR_ONLY("IR Only"),
    VISIBLE_ONLY("Visible Only"),
    FUSION_50_50("Fusion 50/50"),
    FUSION_70_30("Fusion 70/30"),
    PICTURE_IN_PICTURE("Picture in Picture")
}

data class CalibrationData(
    val productType: String = "TC001",
    val serialNumber: String = "",
    val isCalibrated: Boolean = false,
    val minTemp: Float = -40f,
    val maxTemp: Float = 200f,
    val calibrationDate: String = ""
)

data class ProcessingState(
    val tempRange: ClosedFloatingPointRange<Float> = 0f..100f,
    val palette: ThermalPalette = ThermalPalette.RAINBOW,
    val autoRange: Boolean = true
)

enum class ThermalPalette(val displayName: String, val primaryColor: Color) {
    RAINBOW("Rainbow", Color(0xFF8E24AA)),
    IRON("Iron", Color(0xFFD32F2F)),
    GREY("Greyscale", Color(0xFF616161)),
    HOT("Hot", Color(0xFFFF5722)),
    COLD("Cold", Color(0xFF2196F3))
}

/**
 * Preview helper
 */
@Composable
fun ThermalOperationsPreview() {
    var config by remember { 
        mutableStateOf(IRConfig())
    }
    
    var processingState by remember {
        mutableStateOf(ProcessingState())
    }
    
    val sampleCalibration = CalibrationData(
        productType = "TC001",
        serialNumber = "SN123456789",
        isCalibrated = true,
        minTemp = -10f,
        maxTemp = 150f,
        calibrationDate = "2024-01-15"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Operations",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        ThermalOperationsCompose.IRConfigurationPanel(
            currentConfig = config,
            onConfigChange = { config = it }
        )
        
        ThermalOperationsCompose.CalibrationDataPanel(
            calibrationData = sampleCalibration
        )
        
        ThermalOperationsCompose.ThermalProcessingControls(
            processingState = processingState,
            onProcessingChange = { processingState = it }
        )
    }
}