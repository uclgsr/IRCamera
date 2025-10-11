package com.mpdc4gsr.component.thermal.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.component.shared.app.utils.SharedMathUtils
import kotlin.math.roundToInt

@Composable
fun TemperatureIndicator(
    temperature: Float,
    unit: String = "°C",
    minTemp: Float = 0f,
    maxTemp: Float = 100f,
    coldColor: Color = Color.Blue,
    hotColor: Color = Color.Red,
    modifier: Modifier = Modifier,
) {
    val normalizedTemp = ((temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
    val backgroundColor =
        Color(
            SharedMathUtils.lerp(coldColor.red, hotColor.red, normalizedTemp),
            SharedMathUtils.lerp(coldColor.green, hotColor.green, normalizedTemp),
            SharedMathUtils.lerp(coldColor.blue, hotColor.blue, normalizedTemp),
            SharedMathUtils.lerp(coldColor.alpha, hotColor.alpha, normalizedTemp),
        )
    val textColor = if (normalizedTemp > 0.5f) Color.White else Color.Black
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = "${temperature.roundToInt()}$unit",
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ThermalGradientBar(
    minTemp: Float,
    maxTemp: Float,
    unit: String = "°C",
    coldColor: Color = Color.Blue,
    hotColor: Color = Color.Red,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Gradient bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(coldColor, hotColor),
                        ),
                    ).border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(10.dp),
                    ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Temperature labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${minTemp.roundToInt()}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${maxTemp.roundToInt()}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ThermalStatusIndicator(
    level: ThermalStatusLevel,
    message: String,
    modifier: Modifier = Modifier,
) {
    val statusColor =
        when (level) {
            ThermalStatusLevel.NORMAL -> Color.Green
            ThermalStatusLevel.WARNING -> Color(0xFFFF9800) // Orange
            ThermalStatusLevel.CRITICAL -> Color.Red
        }
    val statusIcon =
        when (level) {
            ThermalStatusLevel.NORMAL -> Icons.Default.CheckCircle
            ThermalStatusLevel.WARNING -> Icons.Default.Warning
            ThermalStatusLevel.CRITICAL -> Icons.Default.Error
        }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor),
        )
        Icon(
            imageVector = statusIcon,
            contentDescription = message,
            tint = statusColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun MeasurementPoint(
    label: String,
    temperature: Float,
    unit: String = "°C",
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (isActive) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${temperature.roundToInt()}$unit",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

@Composable
fun ThermalToolbar(
    selectedTool: ThermalTool?,
    onToolSelected: (ThermalTool?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThermalTool.entries.forEach { tool ->
            val isSelected = selectedTool == tool
            FilterChip(
                selected = isSelected,
                onClick = {
                    onToolSelected(if (isSelected) null else tool)
                },
                label = {
                    Text(
                        text = tool.displayName,
                        fontSize = 12.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.displayName,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

// Data classes and enums for the utilities
enum class ThermalTool(
    val displayName: String,
    val icon: ImageVector,
) {
    POINT("Point", Icons.Default.Place),
    LINE("Line", Icons.Default.Timeline),
    RECTANGLE("Rect", Icons.Default.CropFree),
    CIRCLE("Circle", Icons.Default.RadioButtonUnchecked),
    AREA("Area", Icons.Default.CropFree),
}

@Composable
fun ThermalUtilsComposePreview() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Temperature indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TemperatureIndicator(
                temperature = 25f,
                modifier = Modifier.weight(1f),
            )
            TemperatureIndicator(
                temperature = 75f,
                modifier = Modifier.weight(1f),
            )
        }
        // Gradient bar
        ThermalGradientBar(
            minTemp = 0f,
            maxTemp = 100f,
        )
        // Status indicators
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ThermalStatusIndicator(
                level = ThermalStatusLevel.NORMAL,
                message = "Connected",
            )
            ThermalStatusIndicator(
                level = ThermalStatusLevel.WARNING,
                message = "High Temperature",
            )
            ThermalStatusIndicator(
                level = ThermalStatusLevel.CRITICAL,
                message = "Critical Temperature",
            )
        }
        // Measurement points
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MeasurementPoint(
                label = "P1",
                temperature = 36.5f,
                isActive = true,
                modifier = Modifier.weight(1f),
            )
            MeasurementPoint(
                label = "P2",
                temperature = 42.1f,
                modifier = Modifier.weight(1f),
            )
        }
        // Toolbar
        var selectedTool by remember { mutableStateOf<ThermalTool?>(null) }
        ThermalToolbar(
            selectedTool = selectedTool,
            onToolSelected = { selectedTool = it },
        )
    }
}




