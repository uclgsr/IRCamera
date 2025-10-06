package com.mpdc4gsr.module.thermalunified.activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.AbilityComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.PDFListComposeFragment
import com.mpdc4gsr.module.user.compose.MoreComposeFragment
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel
import kotlinx.coroutines.launch
class IRMainComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                MainContent()
            }
        }
    }
    @Composable
    private fun MainContent() {
        val pagerState = rememberPagerState(pageCount = { 5 })
        val scope = rememberCoroutineScope()
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
@Composable
private fun ThermalTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing thermal fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        update = { view ->
            activity?.let {
                val fragment = IRThermalComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
private fun GalleryTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing gallery fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        update = { view ->
            activity?.let {
                val fragment = IRGalleryTabComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
private fun AbilityTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing ability fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_2
            }
        },
        update = { view ->
            activity?.let {
                val fragment = AbilityComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
private fun PDFTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing PDF fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_3
            }
        },
        update = { view ->
            activity?.let {
                val fragment = PDFListComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
private fun MoreTabContent() {
    val viewModel: MoreComposeFragmentViewModel = viewModel()
    MoreComposeFragment(
        viewModel = viewModel,
        isTC007 = false,
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
    tab: MainThermalTab,
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
internal data class MainThermalTab(
    val title: String,
    val icon: ImageVector
)
private fun getThermalTabs(): List<MainThermalTab> {
    return listOf(
        MainThermalTab("Thermal", Icons.Default.Videocam),
        MainThermalTab("Gallery", Icons.Default.Photo),
        MainThermalTab("Ability", Icons.Default.Build),
        MainThermalTab("PDF", Icons.Default.Description),
        MainThermalTab("More", Icons.Default.MoreVert)
    )
}