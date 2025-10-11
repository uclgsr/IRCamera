package com.mpdc4gsr.component.thermal.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.shared.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.component.thermal.viewmodel.IRGalleryTabViewModel
import kotlinx.coroutines.launch

class IRGalleryTabComposeFragment : BaseComposeFragment<IRGalleryTabViewModel>() {
    override fun createViewModel(): IRGalleryTabViewModel = viewModels<IRGalleryTabViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryTabViewModel) {
        // Observe ViewModel state
        val currentDirType by viewModel.currentDirType.collectAsStateWithLifecycle()
        val canSwitchDir by viewModel.canSwitchDir.collectAsStateWithLifecycle()
        val hasBackIcon by viewModel.hasBackIcon.collectAsStateWithLifecycle()
        // Handle UI events from ViewModel
        LaunchedEffect(Unit) {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel.UiEvent.NavigateBack -> {
                        requireActivity().finish()
                    }

                    else -> {} // Handle other events if needed
                }
            }
        }
        // Tab configuration
        val tabTitles = listOf("Pictures", "Videos", "Reports")
        val pagerState = rememberPagerState(pageCount = { tabTitles.size })
        val coroutineScope = rememberCoroutineScope()
        LibSharedTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top app bar with directory switcher
                GalleryTopBar(
                    viewModel = viewModel,
                    currentDirType = currentDirType,
                    canSwitchDir = canSwitchDir,
                    hasBackIcon = hasBackIcon,
                    onDirectoryChange = { dirType ->
                        viewModel.changeDirType(dirType)
                    },
                    onBackClick = { viewModel.navigateBack() },
                )
                // Tab row
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight =
                                        if (pagerState.currentPage == index) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                )
                            },
                        )
                    }
                }
                // Tab content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 ->
                            GalleryPictureTab(
                                dirType = currentDirType,
                                modifier = Modifier.fillMaxSize(),
                            )

                        1 ->
                            GalleryVideoTab(
                                dirType = currentDirType,
                                modifier = Modifier.fillMaxSize(),
                            )

                        2 ->
                            GalleryReportsTab(
                                dirType = currentDirType,
                                modifier = Modifier.fillMaxSize(),
                            )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GalleryTopBar(
        viewModel: IRGalleryTabViewModel,
        currentDirType: DirType,
        canSwitchDir: Boolean,
        hasBackIcon: Boolean,
        onDirectoryChange: (DirType) -> Unit,
        onBackClick: () -> Unit,
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Gallery",
                        fontWeight = FontWeight.Bold,
                    )
                    if (canSwitchDir) {
                        DirectorySwitcher(
                            currentDirType = currentDirType,
                            onDirectoryChange = onDirectoryChange,
                        )
                    }
                }
            },
            navigationIcon = {
                if (hasBackIcon) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { viewModel.showMoreOptions() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
        )
    }

    @Composable
    private fun DirectorySwitcher(
        currentDirType: DirType,
        onDirectoryChange: (DirType) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                onClick = { expanded = true },
                label = {
                    Text(
                        text = getDirTypeDisplayName(currentDirType),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                selected = false,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Directory",
                    )
                },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DirType.values().forEach { dirType ->
                    DropdownMenuItem(
                        text = {
                            Text(getDirTypeDisplayName(dirType))
                        },
                        onClick = {
                            onDirectoryChange(dirType)
                            expanded = false
                        },
                        leadingIcon = {
                            if (dirType == currentDirType) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun GalleryPictureTab(
        dirType: DirType,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            // This would embed the actual GalleryPictureComposeFragment
            // For now, showing a placeholder that will be replaced with the actual implementation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Pictures",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Picture Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Integration with GalleryPictureComposeFragment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun GalleryVideoTab(
        dirType: DirType,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "Videos",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Video Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Thermal video recordings and playback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun GalleryReportsTab(
        dirType: DirType,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = "Reports",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Analysis Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "PDF reports and thermal analysis data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private fun getDirTypeDisplayName(dirType: DirType): String =
        when (dirType) {
            DirType.LINE -> "LINE Device"
            DirType.TS004_LOCALE -> "TS004 Local"
            DirType.TS004_REMOTE -> "TS004 Remote"
            DirType.TC007 -> "TC007 Device"
        }
}




