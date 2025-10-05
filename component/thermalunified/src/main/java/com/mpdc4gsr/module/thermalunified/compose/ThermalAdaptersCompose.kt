package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mpdc4gsr.module.thermalunified.model.AlbumItem

@Composable
fun CameraItemListCompose(
    items: List<CameraItem>,
    onItemClick: (Int, CameraItem) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            CameraItemCard(
                item = item,
                onClick = { onItemClick(index, item) }
            )
        }
    }
}

@Composable
private fun CameraItemCard(
    item: CameraItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (item.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (item.type) {
                CameraItemType.DELAY -> {
                    if (item.delayTime == 0) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "No Delay",
                            modifier = Modifier.size(32.dp),
                            tint = if (item.isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    } else {
                        TimeDownCompose(
                            initialSeconds = item.delayTime,
                            onFinish = {},
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                CameraItemType.AUTO_FOCUS -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.CenterFocusStrong else Icons.Default.CenterFocusWeak,
                        contentDescription = "Auto Focus",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                CameraItemType.FLASH -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                CameraItemType.HDR -> {
                    Icon(
                        imageVector = if (item.isSelected) Icons.Default.WbSunny else Icons.Default.WbCloudy,
                        contentDescription = "HDR",
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MeasureItemGridCompose(
    items: List<MeasureItem>,
    selectedIndex: Int = -1,
    onItemClick: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            MeasureItemCard(
                item = item,
                isSelected = selectedIndex == index,
                onClick = { onItemClick(index, item.code) }
            )
        }
    }
}

@Composable
private fun MeasureItemCard(
    item: MeasureItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            if (item.description.isNotEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GalleryAlbumListCompose(
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit = {},
    onDeleteAlbum: (AlbumItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(albums) { album ->
            GalleryAlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onDelete = { onDeleteAlbum(album) }
            )
        }
    }
}

@Composable
private fun GalleryAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album thumbnail
            AsyncImage(
                model = album.imagePath,
                contentDescription = album.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Album info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (album.description.isNotEmpty()) {
                    Text(
                        text = album.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${album.imageCount} images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Album",
                    tint = Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTabBarCompose(
    tabs: List<MenuTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = tab.title,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selectedIndex == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun SettingOptionsListCompose(
    options: List<SettingOption>,
    selectedOptions: Set<Int> = emptySet(),
    onOptionToggle: (Int, SettingOption) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(options) { index, option ->
            SettingOptionCard(
                option = option,
                isSelected = selectedOptions.contains(index),
                onToggle = { onOptionToggle(index, option) }
            )
        }
    }
}

@Composable
private fun SettingOptionCard(
    option: SettingOption,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                if (option.description.isNotEmpty()) {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (option.type == SettingOptionType.CHECKBOX) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            } else if (option.type == SettingOptionType.SWITCH) {
                Switch(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

// Data classes for adapters

data class CameraItem(
    val type: CameraItemType,
    val delayTime: Int = 0,
    val isSelected: Boolean = false
)

enum class CameraItemType {
    DELAY, AUTO_FOCUS, FLASH, HDR
}

data class MeasureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val code: Int
)

data class MenuTab(
    val title: String,
    val icon: ImageVector
)

data class SettingOption(
    val title: String,
    val description: String = "",
    val icon: ImageVector,
    val type: SettingOptionType = SettingOptionType.SIMPLE
)

enum class SettingOptionType {
    SIMPLE, CHECKBOX, SWITCH
}

@Composable
fun ThermalAdaptersPreview() {
    val sampleCameraItems = listOf(
        CameraItem(CameraItemType.DELAY, delayTime = 0, isSelected = true),
        CameraItem(CameraItemType.AUTO_FOCUS, isSelected = false),
        CameraItem(CameraItemType.FLASH, isSelected = false),
        CameraItem(CameraItemType.HDR, isSelected = false)
    )

    val sampleMeasureItems = listOf(
        MeasureItem("Person", "1.8m", Icons.Default.Person, 1001),
        MeasureItem("Animal", "1.0m", Icons.Default.Pets, 1002),
        MeasureItem("Object", "0.5m", Icons.Default.Category, 1003),
        MeasureItem("Small", "0.2m", Icons.Default.Circle, 1004)
    )

    val sampleTabs = listOf(
        MenuTab("Camera", Icons.Default.CameraAlt),
        MenuTab("Gallery", Icons.Default.PhotoLibrary),
        MenuTab("Settings", Icons.Default.Settings)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Adapters Preview",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Camera items
        Text("Camera Controls:", style = MaterialTheme.typography.titleMedium)
        CameraItemListCompose(
            items = sampleCameraItems,
            onItemClick = { index, item -> }
        )

        // Menu tabs
        Text("Menu Tabs:", style = MaterialTheme.typography.titleMedium)
        MenuTabBarCompose(
            tabs = sampleTabs,
            selectedIndex = 0,
            onTabSelected = { }
        )

        // Measure items
        Text("Measure Items:", style = MaterialTheme.typography.titleMedium)
        MeasureItemGridCompose(
            items = sampleMeasureItems,
            selectedIndex = 0,
            onItemClick = { _, _ -> },
            modifier = Modifier.height(200.dp)
        )
    }
}