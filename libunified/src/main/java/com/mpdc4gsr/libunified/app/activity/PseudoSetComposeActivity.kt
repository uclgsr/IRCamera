package com.mpdc4gsr.libunified.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PseudoSetComposeActivity : ComponentActivity() {
    private val viewModel: PseudoSetViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                PseudoSetScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PseudoSetComposeActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PseudoSetScreen(
    viewModel: PseudoSetViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pseudo Color Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Temperature Range Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Temperature Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min: ${uiState.minTemp}°C")
                        Slider(
                            value = uiState.minTemp,
                            onValueChange = { viewModel.updateMinTemp(it) },
                            valueRange = -40f..120f,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max: ${uiState.maxTemp}°C")
                        Slider(
                            value = uiState.maxTemp,
                            onValueChange = { viewModel.updateMaxTemp(it) },
                            valueRange = -40f..120f,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            // Color Palette Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Color Palette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availablePalettes) { palette ->
                            PaletteItem(
                                palette = palette,
                                isSelected = palette == uiState.selectedPalette,
                                onSelect = { viewModel.selectPalette(palette) }
                            )
                        }
                    }
                }
            }
            // Advanced Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Range")
                        Switch(
                            checked = uiState.autoRange,
                            onCheckedChange = { viewModel.toggleAutoRange(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lock Range")
                        Switch(
                            checked = uiState.lockRange,
                            onCheckedChange = { viewModel.toggleLockRange(it) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { viewModel.applySettings() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun PaletteItem(
    palette: ThermalPalette,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color gradient preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                palette.colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(30.dp)
                            .background(color)
                    )
                }
            }
            Text(
                text = palette.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Data Classes
data class ThermalPalette(
    val name: String,
    val colors: List<Color>
)

data class PseudoSetUiState(
    val minTemp: Float = -20f,
    val maxTemp: Float = 120f,
    val selectedPalette: ThermalPalette = ThermalPalette(
        "Iron",
        listOf(Color.Blue, Color.Cyan, Color.Yellow, Color.Red)
    ),
    val availablePalettes: List<ThermalPalette> = listOf(
        ThermalPalette("Iron", listOf(Color.Blue, Color.Cyan, Color.Yellow, Color.Red)),
        ThermalPalette("Rainbow", listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red, Color.Magenta)),
        ThermalPalette("Gray", listOf(Color.Black, Color.Gray, Color.White)),
        ThermalPalette("Hot", listOf(Color.Black, Color.Red, Color.Yellow, Color.White)),
        ThermalPalette("Medical", listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow))
    ),
    val autoRange: Boolean = false,
    val lockRange: Boolean = false,
    val isLoading: Boolean = false
)

// ViewModel
class PseudoSetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PseudoSetUiState())
    val uiState: StateFlow<PseudoSetUiState> = _uiState.asStateFlow()
    fun updateMinTemp(temp: Float) {
        _uiState.value = _uiState.value.copy(minTemp = temp)
    }

    fun updateMaxTemp(temp: Float) {
        _uiState.value = _uiState.value.copy(maxTemp = temp)
    }

    fun selectPalette(palette: ThermalPalette) {
        _uiState.value = _uiState.value.copy(selectedPalette = palette)
    }

    fun toggleAutoRange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoRange = enabled)
    }

    fun toggleLockRange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(lockRange = enabled)
    }

    fun resetToDefaults() {
        _uiState.value = PseudoSetUiState()
    }

    fun applySettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Apply settings to thermal camera
            // Implementation would depend on specific thermal camera API
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

// Theme placeholder (would be imported from actual theme)
@Composable
fun IRCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Preview(showBackground = true)
@Composable
fun PseudoSetScreenPreview() {
    IRCameraTheme {
        PseudoSetScreen(
            viewModel = PseudoSetViewModel(),
            onBackPressed = { }
        )
    }
}