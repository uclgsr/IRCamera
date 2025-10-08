package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModel
import java.io.File

@AndroidEntryPoint
class GSRRawImageViewComposeActivity : BaseComposeActivity<GSRRawImageViewViewModel>() {
    override fun createViewModel(): GSRRawImageViewViewModel =
        viewModels<GSRRawImageViewViewModel>().value

    @Composable
    override fun Content(viewModel: GSRRawImageViewViewModel) {
        IRCameraTheme {
            GSRRawImageViewScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRRawImageViewScreen(
    viewModel: GSRRawImageViewViewModel = viewModel(
        factory = GSRRawImageViewViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.imageViewState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("GSR Raw Images") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        // Content Area
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading GSR images...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                ErrorContent(
                    error = errorMessage,
                    onRetry = { viewModel.loadImages() }
                )
            }

            uiState.imageFiles.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                ImageListContent(
                    imageFiles = uiState.imageFiles,
                    onImageClick = { file -> viewModel.openImage(file) }
                )
            }
        }
    }
}

@Composable
private fun ImageListContent(
    imageFiles: List<File>,
    onImageClick: (File) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(imageFiles) { imageFile ->
            GSRImageCard(
                imageFile = imageFile,
                onClick = { onImageClick(imageFile) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRImageCard(
    imageFile: File,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Thumbnail
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "GSR image thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Image Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = imageFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${formatFileSize(imageFile.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Modified: ${formatDate(imageFile.lastModified())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading images",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No images",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No GSR images found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "GSR images will appear here when available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Utility functions
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
}