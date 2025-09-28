package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.IRThermalFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.thermalunified.fragment.AbilityFragment
import com.mpdc4gsr.module.thermalunified.fragment.PDFListFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMainActivityViewModel
import com.mpdc4gsr.module.user.fragment.MoreFragment
import kotlinx.coroutines.launch

/**
 * Modern Compose implementation of thermal main hub activity
 * Preserves the 5-tab ViewPager structure with enhanced Material 3 UI
 */
class IRMainComposeActivity : BaseComposeActivity<IRMainActivityViewModel>() {

    override fun createViewModel(): IRMainActivityViewModel {
        return viewModels<IRMainActivityViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMainActivityViewModel) {
        val pagerState = rememberPagerState(pageCount = { 5 })
        val scope = rememberCoroutineScope()
        
        LibUnifiedTheme {
            Scaffold(
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Main ViewPager content (85% of screen)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.85f)
                    ) { page ->
                        when (page) {
                            0 -> ThermalTabContent()
                            1 -> GalleryTabContent()
                            2 -> AbilityTabContent()
                            3 -> PDFTabContent()
                            4 -> MoreTabContent()
                        }
                    }
                    
                    // Bottom navigation (15% of screen)
                    ThermalBottomNavigation(
                        selectedPage = pagerState.currentPage,
                        onPageSelected = { page ->
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalTabContent() {
    // Embed existing thermal fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = IRThermalFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GalleryTabContent() {
    // Embed existing gallery fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = IRGalleryTabFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun AbilityTabContent() {
    // Embed existing ability fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = AbilityFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_2
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PDFTabContent() {
    // Embed existing PDF fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = PDFListFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_3
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MoreTabContent() {
    // Embed existing more fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = MoreFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_4
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ThermalBottomNavigation(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = getThermalTabs()
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                ThermalTabButton(
                    tab = tab,
                    isSelected = selectedPage == index,
                    onClick = { onPageSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThermalTabButton(
    tab: ThermalTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFFFF6B35) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) Color.White else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Text(
            tab.title,
            color = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Data class for tab configuration
data class ThermalTab(
    val title: String,
    val icon: ImageVector
)

private fun getThermalTabs(): List<ThermalTab> {
    return listOf(
        ThermalTab("Thermal", Icons.Default.Settings),
        ThermalTab("Gallery", Icons.Default.Photo),
        ThermalTab("Ability", Icons.Default.Build),
        ThermalTab("PDF", Icons.Default.Description),
        ThermalTab("More", Icons.Default.MoreVert)
    )
}