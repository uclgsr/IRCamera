package com.mpdc4gsr.libunified.app.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.ObserveBean

data class TargetColor(
    val drawableRes: Int,
    val code: Int,
    val name: String = "",
)

@Composable
fun TargetColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColors =
        remember {
            listOf(
                TargetColor(R.drawable.bg_target_color_green, ObserveBean.TYPE_TARGET_COLOR_GREEN, "Green"),
                TargetColor(R.drawable.bg_target_color_red, ObserveBean.TYPE_TARGET_COLOR_RED, "Red"),
                TargetColor(R.drawable.bg_target_color_blue, ObserveBean.TYPE_TARGET_COLOR_BLUE, "Blue"),
                TargetColor(R.drawable.bg_target_color_black, ObserveBean.TYPE_TARGET_COLOR_BLACK, "Black"),
                TargetColor(R.drawable.bg_target_color_white, ObserveBean.TYPE_TARGET_COLOR_WHITE, "White"),
            )
        }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val itemWidth = screenWidthDp / 5 * 0.78f
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color(0xFF3B3E44))
                .padding(1.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        items(targetColors) { targetColor ->
            TargetColorItem(
                targetColor = targetColor,
                isSelected = targetColor.code == selectedColor,
                itemWidth = itemWidth,
                onClick = { onColorSelected(targetColor.code) },
            )
        }
    }
}

@Composable
private fun TargetColorItem(
    targetColor: TargetColor,
    isSelected: Boolean,
    itemWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val imageSize = (screenWidthDp * 30 / 375).coerceAtLeast(24.dp)
    Box(
        modifier =
            Modifier
                .width(itemWidth)
                .wrapContentHeight()
                .clickable(onClick = onClick)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = targetColor.drawableRes),
                    contentDescription = targetColor.name,
                    modifier =
                        Modifier
                            .size(imageSize)
                            .padding(4.dp),
                )
                if (isSelected) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_target_color_stroke),
                        contentDescription = "Selected stroke",
                        modifier = Modifier.size(imageSize),
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
