package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryActivityViewModel
import kotlinx.coroutines.launch

class GalleryComposeActivity : BaseComposeActivity<GalleryActivityViewModel>() {

    override fun createViewModel(): GalleryActivityViewModel {
        return viewModels<GalleryActivityViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryActivityViewModel) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        var selectedTab by remember { mutableIntStateOf(0) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Gallery",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Tab row
                    GalleryTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            scope.launch {
                                pagerState.animateScrollToPage(tab)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> GalleryPictureTab()
                            1 -> GalleryVideoTab()
                        }
                    }
                }
            }
        }

        // Sync pager with tabs
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
    }
}

@Composable
private fun GalleryTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryTab(
                text = "Pictures",
                icon = Icons.Default.Photo,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )

            GalleryTab(
                text = "Videos",
                icon = Icons.Default.VideoLibrary,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GalleryTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color(0xFF7D8590)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GalleryPictureTab() {
    // Embed existing picture fragment using AndroidView wrapper
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GalleryVideoTab() {
    // Embed existing video fragment using AndroidView wrapper
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}