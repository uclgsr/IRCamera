package com.mpdc4gsr.component.shared.app.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MenuTabItem(
    @DrawableRes val iconRes: Int? = null,
    val icon: ImageVector? = null,
    val label: String = "",
    val isSelected: Boolean = false,
) {
    init {
        require(iconRes != null || icon != null) {
            "Either iconRes or icon must be provided"
        }
    }
}

@Composable
fun MenuTabBar(
    items: List<MenuTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    backgroundColor: Color = Color(0xFF3B3E44),
) {
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        itemsIndexed(items) { index, item ->
            MenuTabItem(
                iconRes = item.iconRes,
                icon = item.icon,
                label = item.label,
                isSelected = index == selectedIndex,
                showLabel = showLabels,
                onClick = { onTabSelected(index) },
            )
        }
    }
}

@Composable
private fun MenuTabItem(
    @DrawableRes iconRes: Int? = null,
    icon: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) Color.White else Color.Gray,
                    )
                }

                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        colorFilter =
                            if (isSelected) {
                                ColorFilter.tint(Color.White)
                            } else {
                                ColorFilter.tint(Color.Gray)
                            },
                    )
                }
            }
        }
        if (showLabel && label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun MenuFirstTab(
    selectedIndex: Int,
    isObserveMode: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSelected by remember(selectedIndex) { mutableStateOf(selectedIndex) }
    val menuItems =
        remember(isObserveMode) {
            if (isObserveMode) {
                listOf(
                    MenuTabItem(iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_1, label = "Menu 1"),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_observe_2,
                        label = "Observe 2",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_4_3,
                        label = "Menu 4-3",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_observe_4,
                        label = "Observe 4",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_2_5,
                        label = "Menu 2-5",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_5_6,
                        label = "Menu 5-6",
                    ),
                )
            } else {
                listOf(
                    MenuTabItem(iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_1, label = "Menu 1"),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_2_5,
                        label = "Menu 2-5",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_normal_3,
                        label = "Normal 3",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_4_3,
                        label = "Menu 4-3",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_5_6,
                        label = "Menu 5-6",
                    ),
                    MenuTabItem(
                        iconRes = com.mpdc4gsr.component.shared.R.drawable.selector_menu_first_normal_6,
                        label = "Normal 6",
                    ),
                )
            }
        }
    MenuTabBar(
        items = menuItems,
        selectedIndex = currentSelected,
        onTabSelected = { index ->
            currentSelected = index
            onTabSelected(index)
        },
        modifier = modifier,
        showLabels = false,
    )
}

@Composable
fun MenuSecondTab(
    selectedIndex: Int,
    menuItems: List<MenuTabItem>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MenuTabBar(
        items = menuItems,
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        showLabels = true,
    )
}


