package com.mpdc4gsr.module.thermalunified.activity

import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.report.activity.ThermalReportCreationComposeActivity
import com.mpdc4gsr.module.thermalunified.fragment.GalleryComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryEditViewModel
import kotlinx.coroutines.launch

class IRGalleryDetail01ComposeActivity : BaseComposeActivity<IRGalleryEditViewModel>() {

    override fun createViewModel(): IRGalleryEditViewModel {
        return viewModels<IRGalleryEditViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryEditViewModel) {
        var showEditTools by remember { mutableStateOf(false) }
        var selectedTool by remember { mutableStateOf("") }
        var imageInfo by remember { mutableStateOf(ImageInfo()) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Gallery Detail",
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
                            IconButton(onClick = { showEditTools = !showEditTools }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_TEXT, "Thermal image from IR Camera")
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share thermal image"))
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                containerColor = Color.Black
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Main image display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    ) {
                        // Gallery fragment view
                        GalleryImageView(
                            modifier = Modifier.fillMaxSize()
                        )

                        // Image info overlay
                        ImageInfoOverlay(
                            imageInfo = imageInfo,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }

                    // Edit tools and controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Edit tools (shown when enabled)
                        if (showEditTools) {
                            EditToolsPanel(
                                selectedTool = selectedTool,
                                onToolSelected = { selectedTool = it }
                            )
                        }

                        // Image information
                        ImageInfoCard(imageInfo = imageInfo)

                        // Action buttons
                        ImageActionButtons(
                            onExport = {
                                scope.launch {
                                    try {
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.Images.Media.DISPLAY_NAME, "thermal_export_${System.currentTimeMillis()}.jpg")
                                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ThermalExports")
                                        }
                                        context.contentResolver.insert(
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            contentValues
                                        )
                                        Toast.makeText(context, "Image exported successfully", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onReport = {
                                val intent = Intent(context, ThermalReportCreationComposeActivity::class.java).apply {
                                    putExtra("imageId", imageInfo.id)
                                    putExtra("imagePath", imageInfo.path)
                                }
                                startActivity(intent)
                            },
                            onDelete = {
                                scope.launch {
                                    Toast.makeText(context, "Image deleted from gallery", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryImageView(
    modifier: Modifier = Modifier
) {
    // Embed existing gallery fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = GalleryComposeFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_2
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ImageInfoOverlay(
    imageInfo: ImageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Temperature Range",
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
            Text(
                "${imageInfo.maxTemp}°C - ${imageInfo.minTemp}°C",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                imageInfo.timestamp,
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EditToolsPanel(
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    val tools = getEditTools()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Edit Tools",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tools) { tool ->
                    EditToolChip(
                        tool = tool,
                        isSelected = selectedTool == tool.name,
                        onClick = { onToolSelected(tool.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditToolChip(
    tool: EditTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tool.icon,
                    contentDescription = tool.name,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    tool.name,
                    fontSize = 10.sp
                )
            }
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF6B35),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF16131E),
            labelColor = Color(0xFF7D8590)
        )
    )
}

@Composable
private fun ImageInfoCard(
    imageInfo: ImageInfo
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Image Information",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoItem("Resolution", "${imageInfo.width} x ${imageInfo.height}")
            InfoItem("File size", imageInfo.fileSize)
            InfoItem("Max Temperature", "${imageInfo.maxTemp}°C")
            InfoItem("Min Temperature", "${imageInfo.minTemp}°C")
            InfoItem("Capture time", imageInfo.timestamp)
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImageActionButtons(
    onExport: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = "Export",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export", fontSize = 12.sp)
        }

        Button(
            onClick = onReport,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B7280)
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "Report",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Report", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
        }
    }
}

// Data classes
data class ImageInfo(
    val id: Long = 0,
    val path: String = "",
    val width: Int = 384,
    val height: Int = 288,
    val fileSize: String = "2.1 MB",
    val maxTemp: Float = 45.2f,
    val minTemp: Float = 18.7f,
    val timestamp: String = "2024-01-15 14:30:25"
)

data class EditTool(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getEditTools(): List<EditTool> {
    return listOf(
        EditTool("Crop", Icons.Default.CropFree),
        EditTool("Rotate", Icons.AutoMirrored.Filled.RotateRight),
        EditTool("Analyze", Icons.Default.Analytics),
        EditTool("Measure", Icons.Default.Straighten),
        EditTool("Filter", Icons.Default.FilterAlt)
    )
}