// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components' subtree
// Files: 6; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\ComposeTextRenderer.kt =====

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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun rememberThemeAwarePaintColor(): Int {
    val color = MaterialTheme.colorScheme.onSurface
    return remember(color) { color.toArgb() }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\ComposeToast.kt =====

package com.mpdc4gsr.libunified.app.compose.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay

@Composable
fun ComposeToast(
    message: String,
    duration: Long = 2000L,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(duration)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 80.dp)
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xCC000000),
            shadowElevation = 8.dp
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
    }
}

object ComposeToastHelper {
    private var currentToast: android.app.Dialog? = null
    fun show(context: Context, message: String, duration: Long = 2000L) {
        dismiss()
        currentToast = android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar).apply {
            val composeView = ComposeView(context).apply {
                setContent {
                    LibUnifiedTheme {
                        ComposeToast(
                            message = message,
                            duration = duration,
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }
            setContentView(composeView)
            window?.apply {
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
                clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    fun show(context: Context, @StringRes resId: Int, duration: Long = 2000L) {
        show(context, context.getString(resId), duration)
    }

    fun dismiss() {
        currentToast?.dismiss()
        currentToast = null
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\MenuCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.components

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
    val isSelected: Boolean = false
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
    backgroundColor: Color = Color(0xFF3B3E44)
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        itemsIndexed(items) { index, item ->
            MenuTabItem(
                iconRes = item.iconRes,
                icon = item.icon,
                label = item.label,
                isSelected = index == selectedIndex,
                showLabel = showLabels,
                onClick = { onTabSelected(index) }
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) Color.White else Color.Gray
                    )
                }

                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        colorFilter = if (isSelected) {
                            ColorFilter.tint(Color.White)
                        } else {
                            ColorFilter.tint(Color.Gray)
                        }
                    )
                }
            }
        }
        if (showLabel && label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun MenuFirstTab(
    selectedIndex: Int,
    isObserveMode: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSelected by remember(selectedIndex) { mutableStateOf(selectedIndex) }
    val menuItems = remember(isObserveMode) {
        if (isObserveMode) {
            listOf(
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_1, label = "Menu 1"),
                MenuTabItem(
                    iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_observe_2,
                    label = "Observe 2"
                ),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_4_3, label = "Menu 4-3"),
                MenuTabItem(
                    iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_observe_4,
                    label = "Observe 4"
                ),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_2_5, label = "Menu 2-5"),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_5_6, label = "Menu 5-6")
            )
        } else {
            listOf(
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_1, label = "Menu 1"),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_2_5, label = "Menu 2-5"),
                MenuTabItem(
                    iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_normal_3,
                    label = "Normal 3"
                ),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_4_3, label = "Menu 4-3"),
                MenuTabItem(iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_5_6, label = "Menu 5-6"),
                MenuTabItem(
                    iconRes = com.mpdc4gsr.libunified.R.drawable.selector_menu_first_normal_6,
                    label = "Normal 6"
                )
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
        showLabels = false
    )
}

@Composable
fun MenuSecondTab(
    selectedIndex: Int,
    menuItems: List<MenuTabItem>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MenuTabBar(
        items = menuItems,
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        showLabels = true
    )
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\MenuViewsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuEditView(
    @DrawableRes menu1Icon: Int,
    @DrawableRes menu2Icon: Int,
    @DrawableRes menu3Icon: Int,
    @DrawableRes menu4Icon: Int,
    menu1Label: String = "Menu 1",
    menu2Label: String = "Menu 2",
    menu3Label: String = "Menu 3",
    menu4Label: String = "Bar",
    selectedPosition: Int = -1,
    isBarSelected: Boolean = false,
    onMenuItemClick: (Int) -> Unit = {},
    onBarClick: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2F33))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu 1
        MenuEditItem(
            iconRes = menu1Icon,
            label = menu1Label,
            isSelected = selectedPosition == 0,
            onClick = { onMenuItemClick(0) }
        )
        // Menu 2
        MenuEditItem(
            iconRes = menu2Icon,
            label = menu2Label,
            isSelected = selectedPosition == 1,
            onClick = { onMenuItemClick(1) }
        )
        // Menu 3
        MenuEditItem(
            iconRes = menu3Icon,
            label = menu3Label,
            isSelected = selectedPosition == 2,
            onClick = { onMenuItemClick(2) }
        )
        // Menu 4 (Bar Toggle)
        MenuEditItem(
            iconRes = menu4Icon,
            label = menu4Label,
            isSelected = isBarSelected,
            onClick = { onBarClick(!isBarSelected) }
        )
    }
}

@Composable
private fun MenuEditItem(
    @DrawableRes iconRes: Int? = null,
    icon: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) Color.White else Color.Gray
                    )
                }

                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(
                            if (isSelected) Color.White else Color.Gray
                        )
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CameraMenuView(
    @DrawableRes actionIcon: Int? = null,
    actionIconVector: ImageVector? = null,
    @DrawableRes galleryIcon: Int? = null,
    galleryIconVector: ImageVector? = null,
    @DrawableRes moreIcon: Int? = null,
    moreIconVector: ImageVector? = null,
    isVideoMode: Boolean = false,
    canSwitchMode: Boolean = true,
    onPhotoClick: () -> Unit = {},
    onVideoToggle: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onModeSwitch: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode selector (Photo/Video)
        if (canSwitchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photo",
                    fontSize = 16.sp,
                    color = if (!isVideoMode) Color.White else Color.Gray,
                    fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onModeSwitch(false) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color.Gray)
                )
                Text(
                    text = "Video",
                    fontSize = 16.sp,
                    color = if (isVideoMode) Color.White else Color.Gray,
                    fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onModeSwitch(true) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        // Camera controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGalleryClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    galleryIconVector != null -> {
                        Icon(
                            imageVector = galleryIconVector,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    galleryIcon != null -> {
                        Image(
                            painter = painterResource(id = galleryIcon),
                            contentDescription = "Gallery",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
            // Main action button (Photo or Video)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVideoMode) Color.Red else Color.White
                    )
                    .clickable {
                        if (isVideoMode) {
                            onVideoToggle()
                        } else {
                            onPhotoClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isVideoMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                }
            }
            // More button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMoreClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    moreIconVector != null -> {
                        Icon(
                            imageVector = moreIconVector,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    moreIcon != null -> {
                        Image(
                            painter = painterResource(id = moreIcon),
                            contentDescription = "More",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\SettingsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.components

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
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showIcon) {
                    when {
                        icon != null -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = text,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Gray
                            )
                        }

                        iconRes != null -> {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = text,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
            }
            if (showMoreArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "More",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (showLine) {
            HorizontalDivider(
                color = Color.LightGray,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String? = null,
    items: List<SettingItemData>,
    onItemClick: (Int) -> Unit = {}
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            items.forEachIndexed { index, item ->
                SettingItem(
                    text = item.text,
                    iconRes = item.iconRes,
                    icon = item.icon,
                    showIcon = item.showIcon,
                    showMoreArrow = item.showMoreArrow,
                    showLine = index < items.size - 1,
                    onClick = { onItemClick(index) }
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
    val showMoreArrow: Boolean = true
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\TargetColorPickerCompose.kt =====

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
    val name: String = ""
)

@Composable
fun TargetColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColors = remember {
        listOf(
            TargetColor(R.drawable.bg_target_color_green, ObserveBean.TYPE_TARGET_COLOR_GREEN, "Green"),
            TargetColor(R.drawable.bg_target_color_red, ObserveBean.TYPE_TARGET_COLOR_RED, "Red"),
            TargetColor(R.drawable.bg_target_color_blue, ObserveBean.TYPE_TARGET_COLOR_BLUE, "Blue"),
            TargetColor(R.drawable.bg_target_color_black, ObserveBean.TYPE_TARGET_COLOR_BLACK, "Black"),
            TargetColor(R.drawable.bg_target_color_white, ObserveBean.TYPE_TARGET_COLOR_WHITE, "White")
        )
    }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val itemWidth = screenWidthDp / 5 * 0.78f
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF3B3E44))
            .padding(1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        items(targetColors) { targetColor ->
            TargetColorItem(
                targetColor = targetColor,
                isSelected = targetColor.code == selectedColor,
                itemWidth = itemWidth,
                onClick = { onColorSelected(targetColor.code) }
            )
        }
    }
}

@Composable
private fun TargetColorItem(
    targetColor: TargetColor,
    isSelected: Boolean,
    itemWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val imageSize = (screenWidthDp * 30 / 375).coerceAtLeast(24.dp)
    Box(
        modifier = Modifier
            .width(itemWidth)
            .wrapContentHeight()
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = targetColor.drawableRes),
                    contentDescription = targetColor.name,
                    modifier = Modifier
                        .size(imageSize)
                        .padding(4.dp)
                )
                if (isSelected) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_target_color_stroke),
                        contentDescription = "Selected stroke",
                        modifier = Modifier.size(imageSize)
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}


