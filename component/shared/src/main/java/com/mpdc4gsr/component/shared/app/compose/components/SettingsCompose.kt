package com.mpdc4gsr.component.shared.app.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingItem(
    text: String,
    @DrawableRes iconRes: Int? = null,
    icon: ImageVector? = null,
    showIcon: Boolean = true,
    showMoreArrow: Boolean = true,
    showLine: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (showIcon) {
                    when {
                        icon != null -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = text,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Gray,
                            )
                        }

                        iconRes != null -> {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = text,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal,
                )
            }
            if (showMoreArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "More",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (showLine) {
            HorizontalDivider(
                color = Color.LightGray,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String? = null,
    items: List<SettingItemData>,
    onItemClick: (Int) -> Unit = {},
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            items.forEachIndexed { index, item ->
                SettingItem(
                    text = item.text,
                    iconRes = item.iconRes,
                    icon = item.icon,
                    showIcon = item.showIcon,
                    showMoreArrow = item.showMoreArrow,
                    showLine = index < items.size - 1,
                    onClick = { onItemClick(index) },
                )
            }
        }
    }
}

data class SettingItemData(
    val text: String,
    @DrawableRes val iconRes: Int? = null,
    val icon: ImageVector? = null,
    val showIcon: Boolean = true,
    val showMoreArrow: Boolean = true,
)


