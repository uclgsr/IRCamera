// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose' directory and its subdirectories.
// Total files: 6 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_base_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeActivity.kt =====

package com.mpdc4gsr.libunified.app.compose.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseComposeActivity<VM : BaseViewModel> : ComponentActivity() {
    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)
    protected open fun onDeviceConnected() {}
    protected open fun onDeviceDisconnected() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibUnifiedTheme {
                val viewModel = createViewModel()
                Content(viewModel)
                HandleConnectionEvents(viewModel)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH
            )
        )
    }

    @Composable
    private fun HandleConnectionEvents(viewModel: VM) {
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                DeviceEventManager.deviceConnectionState.collectLatest { state ->
                    state?.let {
                        if (it.isConnected) {
                            onDeviceConnected()
                        } else {
                            onDeviceDisconnected()
                        }
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeFragment.kt =====

package com.mpdc4gsr.libunified.app.compose.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

abstract class BaseComposeFragment<VM : ViewModel> : Fragment() {
    abstract fun createViewModel(): VM

    @Composable
    abstract fun Content(viewModel: VM)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Use ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            // to ensure proper cleanup when fragment is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LibUnifiedTheme {
                    Content(createViewModel())
                }
            }
        }
    }

    open fun onFragmentCreated() {
        // Default implementation does nothing
        // Override in subclasses for specific initialization
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }
}

abstract class EnhancedBaseComposeFragment<VM : ViewModel> : BaseComposeFragment<VM>() {
    open val handlesBackPress: Boolean = false
    open fun onBackPressed(): Boolean {
        return false
    }

    open fun onFragmentDestroyed() {
        // Default implementation does nothing
        // Override in subclasses for specific cleanup
    }

    override fun onDestroyView() {
        onFragmentDestroyed()
        super.onDestroyView()
    }
}

abstract class BaseThermalComposeFragment<VM : ViewModel> : EnhancedBaseComposeFragment<VM>() {
    open fun onThermalFragmentCreated() {
        // Thermal-specific initialization
    }

    open fun onThermalDeviceStateChanged(connected: Boolean) {
        // Default implementation does nothing
    }

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        onThermalFragmentCreated()
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\components\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_components_all.kt =====

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


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_dialogs_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs' subtree
// Files: 10; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ComplexDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.compose.components.TargetColorPicker

@Composable
fun TargetColorDialog(
    title: String = "Select Target Color",
    selectedColor: Int = ObserveBean.TYPE_TARGET_COLOR_GREEN,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableStateOf(selectedColor) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.9f else 0.35f
    Dialog(
        onDismissRequest = {
            onColorSelected(currentColor)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = {
                        onColorSelected(currentColor)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                TargetColorPicker(
                    selectedColor = currentColor,
                    onColorSelected = { color ->
                        currentColor = color
                        onColorSelected(color)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

data class CarDetectItem(
    val title: String,
    val children: List<CarDetectChildItem>,
    val isExpanded: Boolean = false
)

data class CarDetectChildItem(
    val name: String,
    val value: String,
    val isSelected: Boolean = false
)

@Composable
fun CarDetectDialog(
    title: String = "Car Detection",
    items: List<CarDetectItem>,
    onItemSelected: (CarDetectChildItem) -> Unit,
    onDismiss: () -> Unit
) {
    val expandedStates = remember {
        androidx.compose.runtime.snapshots.SnapshotStateList<Boolean>().apply {
            addAll(items.map { it.isExpanded })
        }
    }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.9f else 0.6f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        CarDetectSection(
                            item = item,
                            isExpanded = expandedStates.getOrElse(index) { false },
                            onToggle = {
                                expandedStates[index] = !expandedStates[index]
                            },
                            onChildSelected = { child ->
                                onItemSelected(child)
                                onDismiss()
                            }
                        )
                        if (index < items.size - 1) {
                            HorizontalDivider(
                                color = Color.LightGray,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarDetectSection(
    item: CarDetectItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onChildSelected: (CarDetectChildItem) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isExpanded) "â–¼" else "â–¶",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        if (isExpanded) {
            item.children.forEach { child ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChildSelected(child) }
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = child.name,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    if (child.isSelected) {
                        Text(
                            text = "âœ“",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraProgressDialog(
    title: String = "Camera Progress",
    progress: Float = 0f,
    currentStep: String = "",
    totalSteps: Int = 0,
    currentStepNumber: Int = 0,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (totalSteps > 0) {
                    Text(
                        text = "Step $currentStepNumber of $totalSteps",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (currentStep.isNotEmpty()) {
                    Text(
                        text = currentStep,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Cancel", fontSize = 16.sp)
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ComposeDialogHelper.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

class ComposeDialogWrapper(
    context: Context,
    private val content: @Composable () -> Unit
) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val composeView = ComposeView(context).apply {
            setContent {
                LibUnifiedTheme {
                    content()
                }
            }
        }
        setContentView(composeView)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}

class LoadingDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    fun show(message: String = "") {
        dismiss()
        messageState.value = message
        dialog = ComposeDialogWrapper(context) {
            LoadingDialog(
                message = messageState.value,
                onDismissRequest = {}
            )
        }.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun setMessage(message: String) {
        messageState.value = message
    }
}

class ConfirmDialogState(private val context: Context) {
    fun show(
        title: String,
        message: String = "",
        showIcon: Boolean = true,
        showCancel: Boolean = true,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        showCheckbox: Boolean = false,
        checkboxLabel: String = "",
        onConfirm: (isChecked: Boolean) -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            ConfirmDialog(
                title = title,
                message = message,
                showIcon = showIcon,
                showCancel = showCancel,
                confirmText = confirmText,
                cancelText = cancelText,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                onConfirm = {
                    onConfirm(it)
                },
                onDismiss = {}
            )
        }
        dialog.show()
    }
}

class ProgressDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    private val progressState = mutableStateOf(-1f)
    fun show(message: String = "", progress: Float = -1f, cancelable: Boolean = true) {
        dismiss()
        messageState.value = message
        progressState.value = progress
        dialog = ComposeDialogWrapper(context) {
            ProgressDialog(
                message = messageState.value,
                progress = progressState.value,
                cancelable = cancelable,
                onDismiss = { dismiss() }
            )
        }.apply {
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            show()
        }
    }

    fun updateProgress(progress: Float) {
        progressState.value = progress
    }

    fun updateMessage(message: String) {
        messageState.value = message
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}

class MessageDialogState(private val context: Context) {
    fun showLongText(
        title: String,
        content: String,
        buttonText: String = "I Know",
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            LongTextDialog(
                title = title,
                content = content,
                buttonText = buttonText,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }

    fun showNotification(
        message: String,
        showCheckbox: Boolean = true,
        checkboxLabel: String = "Don't show again",
        buttonText: String = "I Know",
        onConfirm: (dontShowAgain: Boolean) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            NotificationDialog(
                message = message,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                buttonText = buttonText,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}

class FirmwareDialogState(private val context: Context) {
    fun show(
        title: String,
        size: String = "",
        content: String,
        showRestartTips: Boolean = false,
        restartTipsText: String = "Device will restart after update",
        showCancel: Boolean = true,
        cancelText: String = "Cancel",
        confirmText: String = "Confirm",
        onCancel: () -> Unit = {},
        onConfirm: () -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            FirmwareUpdateDialog(
                title = title,
                size = size,
                content = content,
                showRestartTips = showRestartTips,
                restartTipsText = restartTipsText,
                showCancel = showCancel,
                cancelText = cancelText,
                confirmText = confirmText,
                onCancel = {
                    onCancel()
                },
                onConfirm = {
                    onConfirm()
                }
            )
        }
        dialog.show()
    }
}

class TipDialogState(private val context: Context) {
    fun show(
        title: String = "",
        message: String,
        positiveText: String = "Confirm",
        negativeText: String = "Cancel",
        showCancel: Boolean = true,
        showRestartTips: Boolean = false,
        restartTipsText: String = "Device will restart",
        cancelable: Boolean = false,
        onPositive: () -> Unit,
        onNegative: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            TipDialog(
                title = title,
                message = message,
                positiveText = positiveText,
                negativeText = negativeText,
                showCancel = showCancel,
                showRestartTips = showRestartTips,
                restartTipsText = restartTipsText,
                cancelable = cancelable,
                onPositive = onPositive,
                onNegative = onNegative,
                onDismiss = {}
            )
        }
        dialog.show()
    }
}

class SimpleMessageDialogState(private val context: Context) {
    fun show(
        iconRes: Int? = null,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            MessageDialog(
                iconRes = iconRes,
                message = message,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}

class EmissivityDialogState(private val context: Context) {
    fun show(
        title: String = "Emissivity Settings",
        currentValue: Float,
        minValue: Float = 0.1f,
        maxValue: Float = 1.0f,
        onValueChange: (Float) -> Unit = {},
        onConfirm: (Float) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            EmissivityDialog(
                title = title,
                currentValue = currentValue,
                minValue = minValue,
                maxValue = maxValue,
                onValueChange = onValueChange,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ConfirmDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmDialog(
    title: String,
    message: String = "",
    showCancel: Boolean = true,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    showCheckbox: Boolean = false,
    checkboxLabel: String = "",
    onConfirm: (isChecked: Boolean) -> Unit,
    onDismiss: () -> Unit = {},
    showIcon: Boolean
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.4f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = { onConfirm(isChecked) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\FirmwareUpdateDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FirmwareUpdateDialog(
    title: String,
    sizeInfo: String = "",
    content: String = "",
    showRestartTips: Boolean = false,
    showCancel: Boolean = true,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.4f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sizeInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = sizeInfo,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device will restart after update",
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onCancel()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\LoadingDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LoadingDialog(
    message: String = "",
    onDismissRequest: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.3f else 0.15f
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\MessageDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LongTextDialog(
    title: String,
    content: String,
    buttonText: String = "I Know",
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.74f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDialog(
    message: String,
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    buttonText: String = "I Know",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.73f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onConfirm(isChecked) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FirmwareUpdateDialog(
    title: String,
    size: String = "",
    content: String,
    showRestartTips: Boolean = false,
    restartTipsText: String = "Device will restart after update",
    showCancel: Boolean = true,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (size.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = size,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restartTipsText,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\PopupDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmissivityTipPopup(
    title: String = "",
    materialText: String = "",
    environmentTemp: Float,
    distance: Float,
    emissivity: Float,
    environmentLabel: String = "Environment",
    distanceLabel: String = "Distance",
    emissivityLabel: String = "Emissivity",
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.85f else 0.55f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (materialText.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = materialText,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    EmissivityInfoRow(
                        label = "$environmentLabel:",
                        value = String.format("%.1fÂ°C", environmentTemp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$distanceLabel:",
                        value = String.format("%.1fm", distance)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$emissivityLabel:",
                        value = String.format("%.2f", emissivity)
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(isChecked)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmissivityInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8F8F8),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ProgressDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProgressDialog(
    message: String = "",
    progress: Float = -1f,
    cancelable: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.52f else 0.35f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (cancelable) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .width(screenWidthDp - 36.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Color",
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val commonColors = listOf(
                    Color.Red, Color.Green, Color.Blue, Color.Yellow,
                    Color.Cyan, Color.Magenta, Color.White, Color.Gray,
                    Color.Black, Color(0xFFFFA500), Color(0xFF800080), Color(0xFFFFC0CB)
                )
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commonColors.size) { index ->
                        val color = commonColors[index]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = color,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    selectedColor = color.toArgb()
                                }
                                .then(
                                    if (selectedColor == color.toArgb()) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                    } else Modifier
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\SpecializedTipDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ObserveDialog(
    title: String = "Observation Mode",
    message: String,
    @DrawableRes iconRes: Int? = null,
    confirmText: String = "Start Observing",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = cancelText, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = confirmText, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShutterDialog(
    title: String = "Shutter Calibration",
    message: String,
    isCalibrating: Boolean = false,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (!isCalibrating) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = !isCalibrating,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isCalibrating) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCancel()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Cancel", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                onConfirm()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Start", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtgDialog(
    title: String = "OTG Connection",
    message: String,
    @DrawableRes iconRes: Int? = null,
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        onConfirm(isChecked)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "OK", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun WaterMarkDialog(
    title: String = "Watermark Settings",
    enableWatermark: Boolean,
    enableDateTime: Boolean,
    onWatermarkChange: (Boolean) -> Unit,
    onDateTimeChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    var watermarkEnabled by remember { mutableStateOf(enableWatermark) }
    var dateTimeEnabled by remember { mutableStateOf(enableDateTime) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Watermark",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Switch(
                        checked = watermarkEnabled,
                        onCheckedChange = {
                            watermarkEnabled = it
                            onWatermarkChange(it)
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show Date & Time",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Switch(
                        checked = dateTimeEnabled,
                        onCheckedChange = {
                            dateTimeEnabled = it
                            onDateTimeChange(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Save", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeDeviceDialog(
    title: String = "Change Device",
    currentDevice: String,
    availableDevices: List<String>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var selectedDevice by remember { mutableStateOf(currentDevice) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDevice == device,
                            onClick = { selectedDevice = device }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = device,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onDeviceSelected(selectedDevice)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\TipDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TipDialog(
    title: String = "",
    message: String,
    positiveText: String = "Confirm",
    negativeText: String = "Cancel",
    showCancel: Boolean = true,
    showRestartTips: Boolean = false,
    restartTipsText: String = "Device will restart",
    cancelable: Boolean = false,
    onPositive: () -> Unit,
    onNegative: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (cancelable) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restartTipsText,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onNegative()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = negativeText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onPositive()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = positiveText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageDialog(
    @DrawableRes iconRes: Int? = null,
    message: String,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.7f else 0.45f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (iconRes != null) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Message icon",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun EmissivityDialog(
    title: String = "Emissivity Settings",
    currentValue: Float,
    minValue: Float = 0.1f,
    maxValue: Float = 1.0f,
    onValueChange: (Float) -> Unit,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentValue) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = String.format("%.2f", sliderValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = minValue..maxValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%.1f", minValue),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("%.1f", maxValue),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(sliderValue)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose' subtree
// Files: 21; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeActivity.kt =====

package com.mpdc4gsr.libunified.app.compose.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseComposeActivity<VM : BaseViewModel> : ComponentActivity() {
    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)
    protected open fun onDeviceConnected() {}
    protected open fun onDeviceDisconnected() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibUnifiedTheme {
                val viewModel = createViewModel()
                Content(viewModel)
                HandleConnectionEvents(viewModel)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH
            )
        )
    }

    @Composable
    private fun HandleConnectionEvents(viewModel: VM) {
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                DeviceEventManager.deviceConnectionState.collectLatest { state ->
                    state?.let {
                        if (it.isConnected) {
                            onDeviceConnected()
                        } else {
                            onDeviceDisconnected()
                        }
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeFragment.kt =====

package com.mpdc4gsr.libunified.app.compose.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

abstract class BaseComposeFragment<VM : ViewModel> : Fragment() {
    abstract fun createViewModel(): VM

    @Composable
    abstract fun Content(viewModel: VM)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Use ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            // to ensure proper cleanup when fragment is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LibUnifiedTheme {
                    Content(createViewModel())
                }
            }
        }
    }

    open fun onFragmentCreated() {
        // Default implementation does nothing
        // Override in subclasses for specific initialization
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }
}

abstract class EnhancedBaseComposeFragment<VM : ViewModel> : BaseComposeFragment<VM>() {
    open val handlesBackPress: Boolean = false
    open fun onBackPressed(): Boolean {
        return false
    }

    open fun onFragmentDestroyed() {
        // Default implementation does nothing
        // Override in subclasses for specific cleanup
    }

    override fun onDestroyView() {
        onFragmentDestroyed()
        super.onDestroyView()
    }
}

abstract class BaseThermalComposeFragment<VM : ViewModel> : EnhancedBaseComposeFragment<VM>() {
    open fun onThermalFragmentCreated() {
        // Thermal-specific initialization
    }

    open fun onThermalDeviceStateChanged(connected: Boolean) {
        // Default implementation does nothing
    }

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        onThermalFragmentCreated()
    }
}


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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ComplexDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.compose.components.TargetColorPicker

@Composable
fun TargetColorDialog(
    title: String = "Select Target Color",
    selectedColor: Int = ObserveBean.TYPE_TARGET_COLOR_GREEN,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableStateOf(selectedColor) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.9f else 0.35f
    Dialog(
        onDismissRequest = {
            onColorSelected(currentColor)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = {
                        onColorSelected(currentColor)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                TargetColorPicker(
                    selectedColor = currentColor,
                    onColorSelected = { color ->
                        currentColor = color
                        onColorSelected(color)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

data class CarDetectItem(
    val title: String,
    val children: List<CarDetectChildItem>,
    val isExpanded: Boolean = false
)

data class CarDetectChildItem(
    val name: String,
    val value: String,
    val isSelected: Boolean = false
)

@Composable
fun CarDetectDialog(
    title: String = "Car Detection",
    items: List<CarDetectItem>,
    onItemSelected: (CarDetectChildItem) -> Unit,
    onDismiss: () -> Unit
) {
    val expandedStates = remember {
        androidx.compose.runtime.snapshots.SnapshotStateList<Boolean>().apply {
            addAll(items.map { it.isExpanded })
        }
    }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.9f else 0.6f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        CarDetectSection(
                            item = item,
                            isExpanded = expandedStates.getOrElse(index) { false },
                            onToggle = {
                                expandedStates[index] = !expandedStates[index]
                            },
                            onChildSelected = { child ->
                                onItemSelected(child)
                                onDismiss()
                            }
                        )
                        if (index < items.size - 1) {
                            HorizontalDivider(
                                color = Color.LightGray,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarDetectSection(
    item: CarDetectItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onChildSelected: (CarDetectChildItem) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isExpanded) "â–¼" else "â–¶",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        if (isExpanded) {
            item.children.forEach { child ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChildSelected(child) }
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = child.name,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    if (child.isSelected) {
                        Text(
                            text = "âœ“",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraProgressDialog(
    title: String = "Camera Progress",
    progress: Float = 0f,
    currentStep: String = "",
    totalSteps: Int = 0,
    currentStepNumber: Int = 0,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (totalSteps > 0) {
                    Text(
                        text = "Step $currentStepNumber of $totalSteps",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (currentStep.isNotEmpty()) {
                    Text(
                        text = currentStep,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Cancel", fontSize = 16.sp)
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ComposeDialogHelper.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

class ComposeDialogWrapper(
    context: Context,
    private val content: @Composable () -> Unit
) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val composeView = ComposeView(context).apply {
            setContent {
                LibUnifiedTheme {
                    content()
                }
            }
        }
        setContentView(composeView)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}

class LoadingDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    fun show(message: String = "") {
        dismiss()
        messageState.value = message
        dialog = ComposeDialogWrapper(context) {
            LoadingDialog(
                message = messageState.value,
                onDismissRequest = {}
            )
        }.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun setMessage(message: String) {
        messageState.value = message
    }
}

class ConfirmDialogState(private val context: Context) {
    fun show(
        title: String,
        message: String = "",
        showIcon: Boolean = true,
        showCancel: Boolean = true,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        showCheckbox: Boolean = false,
        checkboxLabel: String = "",
        onConfirm: (isChecked: Boolean) -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            ConfirmDialog(
                title = title,
                message = message,
                showIcon = showIcon,
                showCancel = showCancel,
                confirmText = confirmText,
                cancelText = cancelText,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                onConfirm = {
                    onConfirm(it)
                },
                onDismiss = {}
            )
        }
        dialog.show()
    }
}

class ProgressDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    private val progressState = mutableStateOf(-1f)
    fun show(message: String = "", progress: Float = -1f, cancelable: Boolean = true) {
        dismiss()
        messageState.value = message
        progressState.value = progress
        dialog = ComposeDialogWrapper(context) {
            ProgressDialog(
                message = messageState.value,
                progress = progressState.value,
                cancelable = cancelable,
                onDismiss = { dismiss() }
            )
        }.apply {
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            show()
        }
    }

    fun updateProgress(progress: Float) {
        progressState.value = progress
    }

    fun updateMessage(message: String) {
        messageState.value = message
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}

class MessageDialogState(private val context: Context) {
    fun showLongText(
        title: String,
        content: String,
        buttonText: String = "I Know",
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            LongTextDialog(
                title = title,
                content = content,
                buttonText = buttonText,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }

    fun showNotification(
        message: String,
        showCheckbox: Boolean = true,
        checkboxLabel: String = "Don't show again",
        buttonText: String = "I Know",
        onConfirm: (dontShowAgain: Boolean) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            NotificationDialog(
                message = message,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                buttonText = buttonText,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}

class FirmwareDialogState(private val context: Context) {
    fun show(
        title: String,
        size: String = "",
        content: String,
        showRestartTips: Boolean = false,
        restartTipsText: String = "Device will restart after update",
        showCancel: Boolean = true,
        cancelText: String = "Cancel",
        confirmText: String = "Confirm",
        onCancel: () -> Unit = {},
        onConfirm: () -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            FirmwareUpdateDialog(
                title = title,
                size = size,
                content = content,
                showRestartTips = showRestartTips,
                restartTipsText = restartTipsText,
                showCancel = showCancel,
                cancelText = cancelText,
                confirmText = confirmText,
                onCancel = {
                    onCancel()
                },
                onConfirm = {
                    onConfirm()
                }
            )
        }
        dialog.show()
    }
}

class TipDialogState(private val context: Context) {
    fun show(
        title: String = "",
        message: String,
        positiveText: String = "Confirm",
        negativeText: String = "Cancel",
        showCancel: Boolean = true,
        showRestartTips: Boolean = false,
        restartTipsText: String = "Device will restart",
        cancelable: Boolean = false,
        onPositive: () -> Unit,
        onNegative: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            TipDialog(
                title = title,
                message = message,
                positiveText = positiveText,
                negativeText = negativeText,
                showCancel = showCancel,
                showRestartTips = showRestartTips,
                restartTipsText = restartTipsText,
                cancelable = cancelable,
                onPositive = onPositive,
                onNegative = onNegative,
                onDismiss = {}
            )
        }
        dialog.show()
    }
}

class SimpleMessageDialogState(private val context: Context) {
    fun show(
        iconRes: Int? = null,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            MessageDialog(
                iconRes = iconRes,
                message = message,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}

class EmissivityDialogState(private val context: Context) {
    fun show(
        title: String = "Emissivity Settings",
        currentValue: Float,
        minValue: Float = 0.1f,
        maxValue: Float = 1.0f,
        onValueChange: (Float) -> Unit = {},
        onConfirm: (Float) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            EmissivityDialog(
                title = title,
                currentValue = currentValue,
                minValue = minValue,
                maxValue = maxValue,
                onValueChange = onValueChange,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ConfirmDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmDialog(
    title: String,
    message: String = "",
    showCancel: Boolean = true,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    showCheckbox: Boolean = false,
    checkboxLabel: String = "",
    onConfirm: (isChecked: Boolean) -> Unit,
    onDismiss: () -> Unit = {},
    showIcon: Boolean
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.4f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = { onConfirm(isChecked) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\FirmwareUpdateDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FirmwareUpdateDialog(
    title: String,
    sizeInfo: String = "",
    content: String = "",
    showRestartTips: Boolean = false,
    showCancel: Boolean = true,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.4f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sizeInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = sizeInfo,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device will restart after update",
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onCancel()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\LoadingDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LoadingDialog(
    message: String = "",
    onDismissRequest: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.3f else 0.15f
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\MessageDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LongTextDialog(
    title: String,
    content: String,
    buttonText: String = "I Know",
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.74f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDialog(
    message: String,
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    buttonText: String = "I Know",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.73f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onConfirm(isChecked) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FirmwareUpdateDialog(
    title: String,
    size: String = "",
    content: String,
    showRestartTips: Boolean = false,
    restartTipsText: String = "Device will restart after update",
    showCancel: Boolean = true,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (size.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = size,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restartTipsText,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\PopupDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmissivityTipPopup(
    title: String = "",
    materialText: String = "",
    environmentTemp: Float,
    distance: Float,
    emissivity: Float,
    environmentLabel: String = "Environment",
    distanceLabel: String = "Distance",
    emissivityLabel: String = "Emissivity",
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.85f else 0.55f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (materialText.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = materialText,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    EmissivityInfoRow(
                        label = "$environmentLabel:",
                        value = String.format("%.1fÂ°C", environmentTemp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$distanceLabel:",
                        value = String.format("%.1fm", distance)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$emissivityLabel:",
                        value = String.format("%.2f", emissivity)
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(isChecked)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmissivityInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8F8F8),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\ProgressDialogCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProgressDialog(
    message: String = "",
    progress: Float = -1f,
    cancelable: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.52f else 0.35f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (cancelable) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .width(screenWidthDp - 36.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Color",
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val commonColors = listOf(
                    Color.Red, Color.Green, Color.Blue, Color.Yellow,
                    Color.Cyan, Color.Magenta, Color.White, Color.Gray,
                    Color.Black, Color(0xFFFFA500), Color(0xFF800080), Color(0xFFFFC0CB)
                )
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commonColors.size) { index ->
                        val color = commonColors[index]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = color,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    selectedColor = color.toArgb()
                                }
                                .then(
                                    if (selectedColor == color.toArgb()) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                    } else Modifier
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\SpecializedTipDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ObserveDialog(
    title: String = "Observation Mode",
    message: String,
    @DrawableRes iconRes: Int? = null,
    confirmText: String = "Start Observing",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = cancelText, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = confirmText, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShutterDialog(
    title: String = "Shutter Calibration",
    message: String,
    isCalibrating: Boolean = false,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (!isCalibrating) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = !isCalibrating,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isCalibrating) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCancel()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Cancel", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                onConfirm()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Start", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtgDialog(
    title: String = "OTG Connection",
    message: String,
    @DrawableRes iconRes: Int? = null,
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        onConfirm(isChecked)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "OK", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun WaterMarkDialog(
    title: String = "Watermark Settings",
    enableWatermark: Boolean,
    enableDateTime: Boolean,
    onWatermarkChange: (Boolean) -> Unit,
    onDateTimeChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    var watermarkEnabled by remember { mutableStateOf(enableWatermark) }
    var dateTimeEnabled by remember { mutableStateOf(enableDateTime) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Watermark",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Switch(
                        checked = watermarkEnabled,
                        onCheckedChange = {
                            watermarkEnabled = it
                            onWatermarkChange(it)
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show Date & Time",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Switch(
                        checked = dateTimeEnabled,
                        onCheckedChange = {
                            dateTimeEnabled = it
                            onDateTimeChange(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Save", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeDeviceDialog(
    title: String = "Change Device",
    currentDevice: String,
    availableDevices: List<String>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var selectedDevice by remember { mutableStateOf(currentDevice) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.75f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDevice == device,
                            onClick = { selectedDevice = device }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = device,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onDeviceSelected(selectedDevice)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\dialogs\TipDialogsCompose.kt =====

package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TipDialog(
    title: String = "",
    message: String,
    positiveText: String = "Confirm",
    negativeText: String = "Cancel",
    showCancel: Boolean = true,
    showRestartTips: Boolean = false,
    restartTipsText: String = "Device will restart",
    cancelable: Boolean = false,
    onPositive: () -> Unit,
    onNegative: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest = (if (cancelable) onDismiss else {
        }) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restartTipsText,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onNegative()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = negativeText,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onPositive()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = positiveText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageDialog(
    @DrawableRes iconRes: Int? = null,
    message: String,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.7f else 0.45f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (iconRes != null) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Message icon",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun EmissivityDialog(
    title: String = "Emissivity Settings",
    currentValue: Float,
    minValue: Float = 0.1f,
    maxValue: Float = 1.0f,
    onValueChange: (Float) -> Unit,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentValue) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = String.format("%.2f", sliderValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = minValue..maxValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%.1f", minValue),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("%.1f", maxValue),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(sliderValue)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\LibTheme.kt =====

package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shared theme colors that can be used across modules
// These colors are now public for use in Compose components
val ThermalOrange = Color(0xFFFF6B35)
val ThermalRed = Color(0xFFE63946)
val ThermalBlue = Color(0xFF457B9D)
val ThermalDark = Color(0xFF1D3557)
private val LightColorScheme = lightColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)
private val DarkColorScheme = darkColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = ThermalDark,
    surface = ThermalDark
)

@Composable
fun LibUnifiedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\Spacing.kt =====

package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val touchTarget: Dp = 48.dp
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils\SafeRippleModifier.kt =====

package com.mpdc4gsr.libunified.app.compose.utils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
private fun rememberCancellableInteractionSource(): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    var press: PressInteraction.Press? by remember { mutableStateOf(null) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press = interaction
                is PressInteraction.Release, is PressInteraction.Cancel -> press = null
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            press?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
    return interactionSource
}

@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = onClick
    )
}

fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(bounded = bounded, radius = radius, color = color),
        onClick = onClick
    )
}

@Composable
fun Modifier.safeClickableNoRipple(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {
            scope.launch {
                withFrameNanos { }
                onClick()
            }
        }
    )
}

@Composable
fun deferAction(action: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            withFrameNanos { }
            action()
        }
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_theme_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\LibTheme.kt =====

package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shared theme colors that can be used across modules
// These colors are now public for use in Compose components
val ThermalOrange = Color(0xFFFF6B35)
val ThermalRed = Color(0xFFE63946)
val ThermalBlue = Color(0xFF457B9D)
val ThermalDark = Color(0xFF1D3557)
private val LightColorScheme = lightColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)
private val DarkColorScheme = darkColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = ThermalDark,
    surface = ThermalDark
)

@Composable
fun LibUnifiedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\Spacing.kt =====

package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val touchTarget: Dp = 48.dp
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_utils_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils\SafeRippleModifier.kt =====

package com.mpdc4gsr.libunified.app.compose.utils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
private fun rememberCancellableInteractionSource(): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    var press: PressInteraction.Press? by remember { mutableStateOf(null) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press = interaction
                is PressInteraction.Release, is PressInteraction.Cancel -> press = null
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            press?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
    return interactionSource
}

@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = onClick
    )
}

fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(bounded = bounded, radius = radius, color = color),
        onClick = onClick
    )
}

@Composable
fun Modifier.safeClickableNoRipple(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {
            scope.launch {
                withFrameNanos { }
                onClick()
            }
        }
    )
}

@Composable
fun deferAction(action: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            withFrameNanos { }
            action()
        }
    }
}