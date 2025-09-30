package com.mpdc4gsr.module.thermalunified.lite.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modern Compose version of ImagePickIRLiteActivity
 * Provides comprehensive image selection and management for thermal imaging,
 * with gallery browsing, camera capture, and image processing capabilities
 */
class ImagePickIRLiteComposeActivity : ComponentActivity() {

    private lateinit var viewModel: ImagePickIRLiteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ImagePickIRLiteViewModel::class.java]

        setContent {
            LibUnifiedTheme {
                ImagePickIRLiteScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onImageSelected = { imageUri ->
                        val resultIntent = Intent().apply {
                            data = imageUri
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }

        viewModel.loadImages()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickIRLiteScreen(
    viewModel: ImagePickIRLiteViewModel,
    onNavigateBack: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Image picker launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.processCameraBitmap(it) { uri ->
                onImageSelected(uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Thermal Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }

                    IconButton(onClick = { viewModel.refreshImages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { cameraLauncher.launch(null) },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                }

                FloatingActionButton(
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "From Gallery")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            ImageFilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading images...")
                    }
                }
            } else if (uiState.filteredImages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No Images",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No images found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the camera or gallery button to add images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (uiState.isGridView) {
                    ImageGridView(
                        images = uiState.filteredImages,
                        onImageClick = onImageSelected,
                        onImageLongClick = { viewModel.showImageDetails(it) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    ImageListView(
                        images = uiState.filteredImages,
                        onImageClick = onImageSelected,
                        onImageLongClick = { viewModel.showImageDetails(it) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Image details dialog
        if (uiState.selectedImageDetails != null) {
            ImageDetailsDialog(
                imageDetails = uiState.selectedImageDetails!!,
                onDismiss = { viewModel.hideImageDetails() },
                onSelect = { onImageSelected(it.uri) },
                onDelete = { viewModel.deleteImage(it) }
            )
        }
    }
}

@Composable
fun ImageFilterTabs(
    selectedFilter: ImageFilter,
    onFilterSelected: (ImageFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        ImageFilter.ALL to "All",
        ImageFilter.THERMAL to "Thermal",
        ImageFilter.REGULAR to "Regular",
        ImageFilter.RECENT to "Recent"
    )

    ScrollableTabRow(
        selectedTabIndex = filters.indexOfFirst { it.first == selectedFilter },
        modifier = modifier
    ) {
        filters.forEachIndexed { index, (filter, title) ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
fun ImageGridView(
    images: List<ThermalImageInfo>,
    onImageClick: (Uri) -> Unit,
    onImageLongClick: (ThermalImageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(images) { image ->
            ImageGridItem(
                image = image,
                onClick = { onImageClick(image.uri) },
                onLongClick = { onImageLongClick(image) }
            )
        }
    }
}

@Composable
fun ImageGridItem(
    image: ThermalImageInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = image.uri,
                contentDescription = "Thermal Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay for thermal images
            if (image.isThermal) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            Color.Red.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "IR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Date overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.7f)
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = image.dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ImageListView(
    images: List<ThermalImageInfo>,
    onImageClick: (Uri) -> Unit,
    onImageLongClick: (ThermalImageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(images) { image ->
            ImageListItem(
                image = image,
                onClick = { onImageClick(image.uri) },
                onLongClick = { onImageLongClick(image) }
            )
        }
    }
}

@Composable
fun ImageListItem(
    image: ThermalImageInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
            AsyncImage(
                model = image.uri,
                contentDescription = "Thermal Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = image.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (image.isThermal) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("Thermal") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.Red.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = image.sizeFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = image.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onLongClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }
        }
    }
}

@Composable
fun ImageDetailsDialog(
    imageDetails: ThermalImageInfo,
    onDismiss: () -> Unit,
    onSelect: (ThermalImageInfo) -> Unit,
    onDelete: (ThermalImageInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Details") },
        text = {
            Column {
                AsyncImage(
                    model = imageDetails.uri,
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetailRow("Name", imageDetails.name)
                DetailRow("Size", imageDetails.sizeFormatted)
                DetailRow("Date", imageDetails.dateFormatted)
                DetailRow("Type", if (imageDetails.isThermal) "Thermal Image" else "Regular Image")

                if (imageDetails.isThermal) {
                    DetailRow("Temperature Range", "${imageDetails.minTemp}°C - ${imageDetails.maxTemp}°C")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(imageDetails) }) {
                Text("Select")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onDelete(imageDetails) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

/**
 * ViewModel for Image Pick IR Lite Compose Activity
 */
class ImagePickIRLiteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImagePickUiState())
    val uiState: StateFlow<ImagePickUiState> = _uiState.asStateFlow()

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Simulate loading images from storage
            kotlinx.coroutines.delay(1000)

            val sampleImages = generateSampleImages()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allImages = sampleImages,
                filteredImages = sampleImages
            )
        }
    }

    fun refreshImages() {
        loadImages()
    }

    fun setFilter(filter: ImageFilter) {
        val filteredImages = when (filter) {
            ImageFilter.ALL -> _uiState.value.allImages
            ImageFilter.THERMAL -> _uiState.value.allImages.filter { it.isThermal }
            ImageFilter.REGULAR -> _uiState.value.allImages.filter { !it.isThermal }
            ImageFilter.RECENT -> _uiState.value.allImages.sortedByDescending { it.dateModified }.take(20)
        }

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredImages = filteredImages
        )
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            isGridView = !_uiState.value.isGridView
        )
    }

    fun showImageDetails(image: ThermalImageInfo) {
        _uiState.value = _uiState.value.copy(
            selectedImageDetails = image
        )
    }

    fun hideImageDetails() {
        _uiState.value = _uiState.value.copy(
            selectedImageDetails = null
        )
    }

    fun deleteImage(image: ThermalImageInfo) {
        val updatedImages = _uiState.value.allImages.filter { it.uri != image.uri }
        _uiState.value = _uiState.value.copy(
            allImages = updatedImages,
            filteredImages = updatedImages,
            selectedImageDetails = null
        )
    }

    fun processCameraBitmap(bitmap: Bitmap, onResult: (Uri) -> Unit) {
        viewModelScope.launch {
            // Process the camera bitmap and save it
            // This would typically involve saving to storage and returning the URI
            // For now, we'll simulate this
            kotlinx.coroutines.delay(500)

            // In a real implementation, you would save the bitmap and return the URI
            val mockUri = Uri.parse("content://media/external/images/media/12345")
            onResult(mockUri)
        }
    }

    private fun generateSampleImages(): List<ThermalImageInfo> {
        return listOf(
            ThermalImageInfo(
                uri = Uri.parse("content://media/external/images/media/1"),
                name = "thermal_001.jpg",
                size = 2048576,
                dateModified = System.currentTimeMillis() - 3600000,
                isThermal = true,
                minTemp = 18.5f,
                maxTemp = 45.2f
            ),
            ThermalImageInfo(
                uri = Uri.parse("content://media/external/images/media/2"),
                name = "regular_photo.jpg",
                size = 1536000,
                dateModified = System.currentTimeMillis() - 7200000,
                isThermal = false
            ),
            ThermalImageInfo(
                uri = Uri.parse("content://media/external/images/media/3"),
                name = "thermal_002.jpg",
                size = 2234567,
                dateModified = System.currentTimeMillis() - 10800000,
                isThermal = true,
                minTemp = 22.1f,
                maxTemp = 52.8f
            ),
            ThermalImageInfo(
                uri = Uri.parse("content://media/external/images/media/4"),
                name = "thermal_003.jpg",
                size = 1987654,
                dateModified = System.currentTimeMillis() - 14400000,
                isThermal = true,
                minTemp = 15.3f,
                maxTemp = 38.9f
            )
        )
    }
}

data class ImagePickUiState(
    val isLoading: Boolean = false,
    val isGridView: Boolean = true,
    val selectedFilter: ImageFilter = ImageFilter.ALL,
    val allImages: List<ThermalImageInfo> = emptyList(),
    val filteredImages: List<ThermalImageInfo> = emptyList(),
    val selectedImageDetails: ThermalImageInfo? = null
)

data class ThermalImageInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val isThermal: Boolean,
    val minTemp: Float = 0f,
    val maxTemp: Float = 0f
) {
    val sizeFormatted: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }

    val dateFormatted: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(dateModified))
}

enum class ImageFilter {
    ALL, THERMAL, REGULAR, RECENT
}