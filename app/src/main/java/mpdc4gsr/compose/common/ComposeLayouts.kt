package mpdc4gsr.compose.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ComposeLayouts - Common layout patterns used across 109+ files
 *
 * This file consolidates the most frequently duplicated layout patterns
 * to reduce code duplication and ensure UI consistency.
 */

/**
 * Standard card with consistent styling (found in 109+ files)
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(ComposeDimens.cornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(ComposeDimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(ComposeDimens.spacingMedium),
            content = content
        )
    }
}

/**
 * Status chip component (found in 5+ files)
 */
@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(ComposeDimens.cornerRadius)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(
                horizontal = ComposeDimens.paddingSmall,
                vertical = 4.dp
            )
        )
    }
}

/**
 * Standard border stroke used in thermal components
 */
@Composable
fun getThermalBorderStroke(): BorderStroke {
    return BorderStroke(2.dp, ComposeColors.primaryVariant)
}

/**
 * Metric item component (found in 4+ files)
 */
@Composable
fun MetricItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * Standard row layout pattern (found in 69+ files)
 */
@Composable
fun StandardRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ComposeDimens.paddingMedium),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}

/**
 * Standard column layout with spacing (found in 62+ files)
 */
@Composable
fun StandardColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(ComposeDimens.spacingMedium),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(ComposeDimens.paddingMedium),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/**
 * Standard scaffold setup (found in 67+ files)
 */
@Composable
fun StandardScaffold(
    title: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigationClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions
            )
        },
        content = content
    )
}