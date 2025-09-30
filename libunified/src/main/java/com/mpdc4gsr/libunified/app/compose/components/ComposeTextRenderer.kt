package com.mpdc4gsr.libunified.app.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Demonstrates the idiomatic Compose way to render text using TextMeasurer and drawText
 * instead of native Paint and Canvas.drawText
 *
 * This could replace native Paint-based text rendering in legacy components
 * when migrating to Compose-based implementations.
 */
@Composable
fun ComposeLegendTextDemo(
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    Canvas(
        modifier = modifier.size(200.dp, 100.dp)
    ) {
        drawComposeLegendText(
            textMeasurer = textMeasurer,
            textStyle = textStyle
        )
    }
}

/**
 * Example of how to draw text using Compose's TextMeasurer instead of native Paint
 */
private fun DrawScope.drawComposeLegendText(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
) {
    val legendItems = listOf(
        "GSR Signal",
        "Data Points",
        "Threshold"
    )

    legendItems.forEachIndexed { index, text ->
        val textLayoutResult = textMeasurer.measure(
            text = text,
            style = textStyle
        )

        val x = 20f
        val y = 20f + (index * 25f)

        // Using Compose's drawText instead of Canvas.drawText with Paint
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(x, y)
        )
    }
}

/**
 * For cases where legacy View components need to integrate with Compose theming,
 * this utility function can extract theme colors for use in Paint objects
 */
@Composable
fun rememberThemeAwarePaintColor(): Int {
    val color = MaterialTheme.colorScheme.onSurface
    return remember(color) { color.toArgb() }
}