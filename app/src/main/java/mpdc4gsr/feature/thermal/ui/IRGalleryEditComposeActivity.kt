package mpdc4gsr.feature.thermal.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class EditTool(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    TEMPERATURE("Temperature", Icons.Default.Thermostat, "Add temperature markers"),
    ANNOTATION("Annotation", Icons.Default.Edit, "Add text annotations"),
    MEASUREMENT("Measurement", Icons.Default.Straighten, "Measure distances"),
    CROP("Crop", Icons.Default.Crop, "Crop image area"),
    FILTER("Filter", Icons.Default.FilterAlt, "Apply thermal filters"),
    EXPORT("Export", Icons.Default.FileDownload, "Export processed image")
}

enum class IRGalleryThermalPalette(
    val displayName: String,
    val colors: List<Color>
) {
    IRON("Iron", listOf(Color.Black, Color.Red, Color.Yellow, Color.White)),
    RAINBOW("Rainbow", listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red)),
    GRAYSCALE("Grayscale", listOf(Color.Black, Color.Gray, Color.White)),
    HOT("Hot", listOf(Color.Black, Color.Red, Color(0xFFFFA500), Color.Yellow))
}

data class ImageEditState(
    val isImageLoaded: Boolean = false,
    val selectedTool: EditTool? = null,
    val selectedPalette: IRGalleryThermalPalette = IRGalleryThermalPalette.IRON,
    val hasUnsavedChanges: Boolean = false,
    val temperatureRange: Pair<Float, Float> = 20f to 40f,
    val annotations: List<String> = emptyList()
)

class IRGalleryEditViewModel : AppBaseViewModel() {
    private val _editState = mutableStateOf(ImageEditState())
    val editState: State<ImageEditState> = _editState
    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing
    private val _statusMessage = mutableStateOf("Image editor ready")
    val statusMessage: State<String> = _statusMessage
    fun loadImage(imagePath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Loading thermal image..."
            delay(2000) // Simulate image loading
            _editState.value = _editState.value.copy(isImageLoaded = true)
            _statusMessage.value = "Image loaded successfully"
            _isProcessing.value = false
        }
    }

    fun selectTool(tool: EditTool) {
        _editState.value = _editState.value.copy(selectedTool = tool)
        _statusMessage.value = "Selected tool: ${tool.displayName}"
    }

    fun selectPalette(palette: IRGalleryThermalPalette) {
        _editState.value = _editState.value.copy(
            selectedPalette = palette,
            hasUnsavedChanges = true
        )
        _statusMessage.value = "Applied ${palette.displayName} palette"
    }

    fun updateTemperatureRange(min: Float, max: Float) {
        _editState.value = _editState.value.copy(
            temperatureRange = min to max,
            hasUnsavedChanges = true
        )
        _statusMessage.value = "Temperature range: ${min}°C - ${max}°C"
    }

    fun addAnnotation(text: String) {
        val currentAnnotations = _editState.value.annotations
        _editState.value = _editState.value.copy(
            annotations = currentAnnotations + text,
            hasUnsavedChanges = true
        )
        _statusMessage.value = "Added annotation: $text"
    }

    fun saveImage() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Saving image..."
            delay(3000) // Simulate saving
            _editState.value = _editState.value.copy(hasUnsavedChanges = false)
            _statusMessage.value = "Image saved successfully"
            _isProcessing.value = false
        }
    }

    fun exportImage() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Exporting image..."
            delay(2500) // Simulate export
            _statusMessage.value = "Image exported to gallery"
            _isProcessing.value = false
        }
    }
}

class IRGalleryEditComposeActivity : BaseComposeActivity<IRGalleryEditViewModel>() {
    private val viewModelInstance: IRGalleryEditViewModel by viewModels()
    override fun createViewModel(): IRGalleryEditViewModel = viewModelInstance
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imagePath = intent?.getStringExtra("image_path") ?: "sample_thermal_image.jpg"
        viewModelInstance.loadImage(imagePath)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryEditViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val editState by viewModel.editState
            val isProcessing by viewModel.isProcessing
            val statusMessage by viewModel.statusMessage
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Thermal Image Editor",
                    onBackClick = { finish() },
                    actions = {
                        if (editState.hasUnsavedChanges) {
                            IconButton(onClick = { viewModel.saveImage() }) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Status bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isProcessing)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Image preview area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editState.isImageLoaded) {
                                // Thermal image preview placeholder
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                editState.selectedPalette.colors
                                            )
                                        )
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = "Thermal Image",
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Thermal Image Preview",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${editState.temperatureRange.first}°C - ${editState.temperatureRange.second}°C",
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Image Placeholder",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isProcessing) "Loading image..." else "No image loaded",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    // Tool selection
                    if (editState.isImageLoaded) {
                        Text(
                            text = "Editing Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EditTool.values().take(3).forEach { tool ->
                                EditToolButton(
                                    tool = tool,
                                    isSelected = editState.selectedTool == tool,
                                    onClick = { viewModel.selectTool(tool) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EditTool.values().drop(3).forEach { tool ->
                                EditToolButton(
                                    tool = tool,
                                    isSelected = editState.selectedTool == tool,
                                    onClick = { viewModel.selectTool(tool) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // Thermal palette selection
                        Text(
                            text = "Thermal Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IRGalleryThermalPalette.values().forEach { palette ->
                                PaletteButton(
                                    palette = palette,
                                    isSelected = editState.selectedPalette == palette,
                                    onClick = { viewModel.selectPalette(palette) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // Temperature range control
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Temperature Range",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Min: ${editState.temperatureRange.first}°C",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = editState.temperatureRange.first,
                                    onValueChange = { newMin ->
                                        viewModel.updateTemperatureRange(
                                            newMin,
                                            editState.temperatureRange.second
                                        )
                                    },
                                    valueRange = -10f..50f,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Max: ${editState.temperatureRange.second}°C",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = editState.temperatureRange.second,
                                    onValueChange = { newMax ->
                                        viewModel.updateTemperatureRange(
                                            editState.temperatureRange.first,
                                            newMax
                                        )
                                    },
                                    valueRange = 10f..100f
                                )
                            }
                        }
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveImage() },
                                modifier = Modifier.weight(1f),
                                enabled = editState.hasUnsavedChanges && !isProcessing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Image",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save")
                            }
                            Button(
                                onClick = { viewModel.exportImage() },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export Image",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditToolButton(
    tool: EditTool,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.name,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PaletteButton(
    palette: IRGalleryThermalPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(palette.colors)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = palette.displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}