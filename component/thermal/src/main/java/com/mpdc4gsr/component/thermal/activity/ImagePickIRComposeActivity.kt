package com.mpdc4gsr.component.thermal.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.fragment.IRMonitorThermalComposeFragment
import com.mpdc4gsr.component.thermal.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ImagePickIRComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override val requiredPermissionAreas: Set<FeaturePermissionArea> = setOf(FeaturePermissionArea.MEDIA_REVIEW)

    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var captureMode by remember { mutableStateOf("single") }
        var selectedImages by remember { mutableStateOf(setOf<String>()) }
        var isCapturing by remember { mutableStateOf(false) }
        val recentImages = remember { getRecentImages() }
        val coroutineScope = rememberCoroutineScope()
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Pick Thermal Image",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        actions = {
                            if (selectedImages.isNotEmpty()) {
                                TextButton(
                                    onClick = { finish() },
                                ) {
                                    Text(
                                        "Select (${selectedImages.size})",
                                        color = Color(0xFFFF6B35),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                            ),
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            isCapturing = true
                            // Simulate capture
                            coroutineScope.launch {
                                delay(2000L)
                                isCapturing = false
                            }
                        },
                        containerColor = Color(0xFFFF6B35),
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White,
                            )
                        }
                    }
                },
                containerColor = Color.Black,
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color.Black),
                ) {
                    // Live thermal preview
                    ThermalPreviewSection(
                        isCapturing = isCapturing,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(0.6f),
                    )
                    // Capture mode selector
                    CaptureModeSelector(
                        selectedMode = captureMode,
                        onModeSelected = { captureMode = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Recent images gallery
                    RecentImagesSection(
                        images = recentImages,
                        selectedImages = selectedImages,
                        onImageSelected = { imageId ->
                            selectedImages =
                                if (captureMode == "single") {
                                    setOf(imageId)
                                } else {
                                    if (selectedImages.contains(imageId)) {
                                        selectedImages - imageId
                                    } else {
                                        selectedImages + imageId
                                    }
                                }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalPreviewSection(
    isCapturing: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF0D1117),
            ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Thermal fragment preview (using AndroidView)
            AndroidView(
                factory = { context ->
                    val fragment = IRMonitorThermalComposeFragment()
                    androidx.fragment.app.FragmentContainerView(context).apply {
                        id = androidx.core.R.id.accessibility_custom_action_3
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            // Capture overlay
            if (isCapturing) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.8f),
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6B35),
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                "Capturing thermal image...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            // Temperature overlay
            ThermalInfoOverlay(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ThermalInfoOverlay(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Live Thermal Feed",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Max: 42.5°C",
                color = Color(0xFFFF4444),
                fontSize = 11.sp,
            )
            Text(
                "Min: 18.2°C",
                color = Color(0xFF4444FF),
                fontSize = 11.sp,
            )
            Text(
                "Avg: 28.7°C",
                color = Color(0xFFFFAA00),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun CaptureModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Mode:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            val modes =
                listOf(
                    "single" to "Single Select",
                    "multiple" to "Multiple Select",
                    "burst" to "Burst Mode",
                )
            modes.forEach { (mode, label) ->
                FilterChip(
                    onClick = { onModeSelected(mode) },
                    label = { Text(label, fontSize = 12.sp) },
                    selected = selectedMode == mode,
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590),
                        ),
                )
            }
        }
    }
}

@Composable
private fun RecentImagesSection(
    images: List<RecentImage>,
    selectedImages: Set<String>,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Text(
                "Recent Thermal Images",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(images) { image ->
                    RecentImageItem(
                        image = image,
                        isSelected = selectedImages.contains(image.id),
                        onClick = { onImageSelected(image.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentImageItem(
    image: RecentImage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFFF6B35) else Color(0xFF16131E),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFF0D1117),
                            RoundedCornerShape(6.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Image",
                    tint = if (isSelected) Color.White else Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp),
                )
            }
            // Image info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    image.name,
                    color = if (isSelected) Color.White else Color(0xFF7D8590),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    image.timestamp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF7D8590),
                    fontSize = 10.sp,
                )
                Text(
                    "Max: ${image.maxTemp}°C",
                    color = if (isSelected) Color.White else Color(0xFFFF6B35),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .background(
                                Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ),
                )
            }
        }
    }
}

// Data classes
data class RecentImage(
    val id: String,
    val name: String,
    val timestamp: String,
    val maxTemp: Float,
)

private fun getRecentImages(): List<RecentImage> =
    listOf(
        RecentImage("1", "thermal_capture_001.jpg", "14:30:25", 45.2f),
        RecentImage("2", "thermal_capture_002.jpg", "14:28:15", 38.7f),
        RecentImage("3", "thermal_capture_003.jpg", "14:25:45", 52.1f),
        RecentImage("4", "thermal_capture_004.jpg", "14:22:30", 41.5f),
        RecentImage("5", "thermal_capture_005.jpg", "14:18:12", 47.8f),
        RecentImage("6", "thermal_capture_006.jpg", "14:15:55", 35.2f),
    )



