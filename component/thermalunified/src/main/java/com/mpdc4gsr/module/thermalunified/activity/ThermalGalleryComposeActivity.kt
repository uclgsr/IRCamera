package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalGalleryComposeActivity : BaseComposeActivity<ThermalGalleryViewModel>() {

    override fun createViewModel(): ThermalGalleryViewModel {
        return ThermalGalleryViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalGalleryViewModel) {
        ThermalGalleryScreen(
            onBackClick = { finish() }
        )
    }
}

class ThermalGalleryViewModel : BaseViewModel() {
    // ViewModel implementation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThermalGalleryScreen(
    viewModel: ThermalGalleryViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Thermal Gallery",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White
            ),
            navigationIcon = {
                IconButton(onClick = { onBackClick?.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                }) {
                    Icon(
                        if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = Color(0xFFFF6B35)
                    )
                }
                IconButton(onClick = {  }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFFF6B35)
                    )
                }
            }
        )

        // Filter Bar
        ThermalFilterBar(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // Gallery Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (viewMode) {
                ViewMode.GRID -> ThermalGridView()
                ViewMode.LIST -> ThermalListView()
            }
        }
    }
}

@Composable
private fun ThermalFilterBar(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterType.values().forEach { filter ->
                    FilterChip(
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                filter.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedFilter == filter,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF21262D),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalGridView() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(generateSampleThermalImages()) { image ->
            ThermalImageCard(image = image)
        }
    }
}

@Composable
private fun ThermalListView() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(generateSampleThermalImages()) { image ->
            ThermalImageListItem(image = image)
        }
    }
}

@Composable
private fun ThermalImageCard(image: GalleryThermalImage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Image preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Image info
            Text(
                image.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                "${image.temperature}°C",
                color = Color(0xFFFF6B35),
                fontSize = 10.sp
            )
            Text(
                image.date,
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ThermalImageListItem(image: GalleryThermalImage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Image details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    image.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${image.temperature}°C • ${image.date}",
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
                Text(
                    "${image.resolution} • ${image.size}",
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
            }

            // Actions
            IconButton(onClick = {  }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFF7D8590)
                )
            }
        }
    }
}

private fun generateSampleThermalImages(): List<GalleryThermalImage> {
    return (1..20).map { index ->
        GalleryThermalImage(
            id = index,
            name = "thermal_image_$index.tiff",
            temperature = (20..80).random(),
            date = "2024-01-${(10..28).random()}",
            resolution = "640x480",
            size = "${(100..500).random()}KB"
        )
    }
}

private data class GalleryThermalImage(
    val id: Int,
    val name: String,
    val temperature: Int,
    val date: String,
    val resolution: String,
    val size: String
)

private enum class ViewMode {
    GRID, LIST
}

private enum class FilterType(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    HIGH_TEMP("High Temp"),
    LOW_TEMP("Low Temp")
}