package com.mpdc4gsr.module.thermalunified.compose
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun EmissivityCompose(
    textList: List<String>,
    isAlignTop: Boolean = false,
    drawTopLine: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 0.5.dp.toPx() }
    val lineColor = Color(0xff5b5961)
    if (textList.isEmpty()) {
        return
    }
    Box(modifier = modifier) {
        // Background canvas for custom borders
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            // Draw top line if needed
            if (drawTopLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, strokeWidth / 2),
                    end = Offset(width, strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            // Draw bottom line
            drawLine(
                color = lineColor,
                start = Offset(0f, height - strokeWidth / 2),
                end = Offset(width, height - strokeWidth / 2),
                strokeWidth = strokeWidth
            )
            // Draw left line
            drawLine(
                color = lineColor,
                start = Offset(strokeWidth / 2, 0f),
                end = Offset(strokeWidth / 2, height),
                strokeWidth = strokeWidth
            )
            // Draw vertical separators
            if (textList.size > 1) {
                val firstColumnWidth = width * 135f / 335f
                val remainingWidth = width - firstColumnWidth
                val columnWidth = remainingWidth / 2f
                var x = firstColumnWidth
                repeat(textList.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x - strokeWidth / 2, 0f),
                        end = Offset(x - strokeWidth / 2, height),
                        strokeWidth = strokeWidth
                    )
                    x += columnWidth
                }
            }
        }
        // Content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = if (isAlignTop) Alignment.Top else Alignment.CenterVertically
        ) {
            textList.forEachIndexed { index, text ->
                val weight = if (textList.size == 1) {
                    1f
                } else {
                    if (index == 0) 135f / 335f else (200f / 335f) / 2f
                }
                Text(
                    text = text,
                    modifier = Modifier
                        .weight(weight)
                        .padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center,
                    fontSize = if (textList.size == 1) 12.sp else 11.sp,
                    color = if (textList.size == 1) Color.White else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
@Composable
fun EmissivityComposePreview() {
    Column(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Single item
        EmissivityCompose(
            textList = listOf("Single Value: 0.95"),
            modifier = Modifier.height(40.dp)
        )
        // Multiple items
        EmissivityCompose(
            textList = listOf("Label", "Value 1", "Value 2"),
            drawTopLine = true,
            modifier = Modifier.height(40.dp)
        )
        // Aligned top
        EmissivityCompose(
            textList = listOf("Long Label Text", "Short", "Medium Value"),
            isAlignTop = true,
            modifier = Modifier.height(60.dp)
        )
    }
}