package com.mpdc4gsr.module.thermalunified.feature.ui

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.module.thermalunified.feature.presentation.ThermalFeatureCoordinator
import com.mpdc4gsr.module.thermalunified.feature.presentation.ThermalPresentationState
import com.mpdc4gsr.module.thermalunified.feature.ui.components.ThermalStatusBanner

@Composable
fun ThermalFeatureScaffold(
    coordinator: ThermalFeatureCoordinator,
    modifier: Modifier = Modifier,
    topContent: @Composable (() -> Unit)? = null,
) {
    val state by coordinator.state.collectAsStateWithLifecycle()
    ThermalFeatureScaffoldContent(
        state = state,
        onTabSelected = coordinator::selectFeature,
        modifier = modifier,
        topContent = topContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThermalFeatureScaffoldContent(
    state: ThermalPresentationState,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    topContent: @Composable (() -> Unit)? = null,
) {
    val activeId = state.activeFeatureId
    val features = state.features
    val pagerState =
        rememberPagerState(
            initialPage = features.indexOfFirst { it.id == activeId }.coerceAtLeast(0),
            pageCount = { features.size },
        )
    LaunchedEffect(activeId) {
        val targetIndex = features.indexOfFirst { it.id == activeId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            topContent?.invoke()
            ThermalStatusBanner(
                status = state.deviceStatus,
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp),
            )
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) { page ->
                val descriptor = features.getOrNull(page)
                if (descriptor != null) {
                    FeatureContainer(descriptor)
                }
            }
            ThermalBottomTabs(
                state = state,
                selectedId = activeId,
                onTabSelected = {
                    onTabSelected(it)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .navigationBarsPadding()
                        .background(Color(0x1114181F)),
            )
        }
    }
}

@Composable
private fun FeatureContainer(descriptor: com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureDescriptor) {
    when (val content = descriptor.content) {
        is com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureContent.Compose -> {
            content.body()
        }

        is com.mpdc4gsr.module.thermalunified.feature.navigation.ThermalFeatureContent.FragmentHost -> {
            FragmentFeatureContainer(
                id = descriptor.id,
                factory = content.factory,
            )
        }
    }
}

@Composable
private fun ThermalBottomTabs(
    state: ThermalPresentationState,
    selectedId: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.features.forEach { descriptor ->
            val isSelected = descriptor.id == selectedId
            IconButton(onClick = { onTabSelected(descriptor.id) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = descriptor.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = descriptor.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FragmentFeatureContainer(
    id: String,
    factory: () -> Fragment,
) {
    val context = LocalContext.current
    val fragmentActivity = remember(context) { context as? FragmentActivity }
    val fragmentManager = fragmentActivity?.supportFragmentManager
    val tag = remember(id) { "thermal_feature_$id" }
    val fragmentFactory = rememberUpdatedState(factory)
    if (fragmentManager == null) {
        Text(
            text = "Fragment host unavailable",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    val containerId = remember { View.generateViewId() }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { viewContext ->
            FragmentContainerView(viewContext).also { container ->
                container.id = containerId
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            val currentFragment = fragmentManager.findFragmentByTag(tag)
            if (currentFragment == null || !currentFragment.isAdded) {
                val newFragment = fragmentFactory.value.invoke()
                fragmentManager.commit {
                    replace(view.id, newFragment, tag)
                }
            }
        },
        onRelease = {
            fragmentManager.findFragmentByTag(tag)?.let { fragment ->
                fragmentManager.commit(allowStateLoss = true) {
                    remove(fragment)
                }
            }
        },
    )
}
