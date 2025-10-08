// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\activity' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\activity\BaseConfigComposeActivity.kt =====

package com.mpdc4gsr.libunified.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BaseConfigComposeActivity : ComponentActivity() {
    private val viewModel: BaseConfigViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                BaseConfigScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BaseConfigComposeActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseConfigScreen(
    viewModel: BaseConfigViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Base Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportConfig() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Configuration
            item {
                ConfigSection(
                    title = "System Configuration",
                    items = uiState.systemConfigs,
                    onItemChange = { key, value -> viewModel.updateSystemConfig(key, value) }
                )
            }
            // Network Configuration
            item {
                ConfigSection(
                    title = "Network Configuration",
                    items = uiState.networkConfigs,
                    onItemChange = { key, value -> viewModel.updateNetworkConfig(key, value) }
                )
            }
            // Camera Configuration
            item {
                ConfigSection(
                    title = "Camera Configuration",
                    items = uiState.cameraConfigs,
                    onItemChange = { key, value -> viewModel.updateCameraConfig(key, value) }
                )
            }
            // Sensor Configuration
            item {
                ConfigSection(
                    title = "Sensor Configuration",
                    items = uiState.sensorConfigs,
                    onItemChange = { key, value -> viewModel.updateSensorConfig(key, value) }
                )
            }
            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset All")
                    }
                    OutlinedButton(
                        onClick = { viewModel.importConfig() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import")
                    }
                    Button(
                        onClick = { viewModel.saveConfiguration() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ConfigSection(
    title: String,
    items: List<ConfigItem>,
    onItemChange: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            items.forEach { item ->
                ConfigItemRow(
                    item = item,
                    onValueChange = { value -> onItemChange(item.key, value) }
                )
            }
        }
    }
}

@Composable
fun ConfigItemRow(
    item: ConfigItem,
    onValueChange: (Any) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (item.description.isNotEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when (item.type) {
            ConfigType.BOOLEAN -> {
                Switch(
                    checked = item.value as Boolean,
                    onCheckedChange = onValueChange
                )
            }

            ConfigType.INTEGER -> {
                OutlinedTextField(
                    value = (item.value as Int).toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { value -> onValueChange(value) }
                    },
                    modifier = Modifier.width(100.dp)
                )
            }

            ConfigType.STRING -> {
                OutlinedTextField(
                    value = item.value as String,
                    onValueChange = onValueChange,
                    modifier = Modifier.width(150.dp)
                )
            }

            ConfigType.FLOAT -> {
                OutlinedTextField(
                    value = (item.value as Float).toString(),
                    onValueChange = {
                        it.toFloatOrNull()?.let { value -> onValueChange(value) }
                    },
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}

// Data Classes
data class ConfigItem(
    val key: String,
    val displayName: String,
    val description: String = "",
    val value: Any,
    val type: ConfigType,
    val defaultValue: Any
)

enum class ConfigType {
    BOOLEAN, INTEGER, STRING, FLOAT
}

data class BaseConfigUiState(
    val systemConfigs: List<ConfigItem> = listOf(
        ConfigItem("debug_mode", "Debug Mode", "Enable debug logging", false, ConfigType.BOOLEAN, false),
        ConfigItem("auto_save", "Auto Save", "Automatically save data", true, ConfigType.BOOLEAN, true),
        ConfigItem("save_interval", "Save Interval (s)", "Seconds between auto saves", 30, ConfigType.INTEGER, 30),
        ConfigItem("max_memory", "Max Memory (MB)", "Maximum memory usage", 512, ConfigType.INTEGER, 512)
    ),
    val networkConfigs: List<ConfigItem> = listOf(
        ConfigItem("wifi_timeout", "WiFi Timeout (s)", "WiFi connection timeout", 10, ConfigType.INTEGER, 10),
        ConfigItem("enable_hotspot", "Enable Hotspot", "Allow device hotspot mode", false, ConfigType.BOOLEAN, false),
        ConfigItem("network_encryption", "Encryption", "Network encryption type", "WPA2", ConfigType.STRING, "WPA2"),
        ConfigItem("max_clients", "Max Clients", "Maximum connected clients", 4, ConfigType.INTEGER, 4)
    ),
    val cameraConfigs: List<ConfigItem> = listOf(
        ConfigItem("auto_focus", "Auto Focus", "Enable automatic focus", true, ConfigType.BOOLEAN, true),
        ConfigItem("resolution", "Resolution", "Camera resolution", "1920x1080", ConfigType.STRING, "1920x1080"),
        ConfigItem("frame_rate", "Frame Rate", "Frames per second", 30, ConfigType.INTEGER, 30),
        ConfigItem("exposure_time", "Exposure Time", "Auto exposure time limit", 0.1f, ConfigType.FLOAT, 0.1f)
    ),
    val sensorConfigs: List<ConfigItem> = listOf(
        ConfigItem("sample_rate", "Sample Rate (Hz)", "Sensor sampling frequency", 100, ConfigType.INTEGER, 100),
        ConfigItem("enable_filtering", "Enable Filtering", "Apply signal filtering", true, ConfigType.BOOLEAN, true),
        ConfigItem(
            "calibration_mode",
            "Calibration Mode",
            "Sensor calibration method",
            "AUTO",
            ConfigType.STRING,
            "AUTO"
        ),
        ConfigItem("sensitivity", "Sensitivity", "Sensor sensitivity level", 1.0f, ConfigType.FLOAT, 1.0f)
    ),
    val isLoading: Boolean = false
)

// ViewModel
class BaseConfigViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BaseConfigUiState())
    val uiState: StateFlow<BaseConfigUiState> = _uiState.asStateFlow()
    fun updateSystemConfig(key: String, value: Any) {
        updateConfigList("system", key, value)
    }

    fun updateNetworkConfig(key: String, value: Any) {
        updateConfigList("network", key, value)
    }

    fun updateCameraConfig(key: String, value: Any) {
        updateConfigList("camera", key, value)
    }

    fun updateSensorConfig(key: String, value: Any) {
        updateConfigList("sensor", key, value)
    }

    private fun updateConfigList(category: String, key: String, value: Any) {
        val currentState = _uiState.value
        val updatedState = when (category) {
            "system" -> currentState.copy(
                systemConfigs = currentState.systemConfigs.map {
                    if (it.key == key) it.copy(value = value) else it
                }
            )

            "network" -> currentState.copy(
                networkConfigs = currentState.networkConfigs.map {
                    if (it.key == key) it.copy(value = value) else it
                }
            )

            "camera" -> currentState.copy(
                cameraConfigs = currentState.cameraConfigs.map {
                    if (it.key == key) it.copy(value = value) else it
                }
            )

            "sensor" -> currentState.copy(
                sensorConfigs = currentState.sensorConfigs.map {
                    if (it.key == key) it.copy(value = value) else it
                }
            )

            else -> currentState
        }
        _uiState.value = updatedState
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Reset all configurations to defaults
            _uiState.value = BaseConfigUiState()
        }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Save configuration to persistent storage
            // Implementation would depend on specific storage mechanism
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun exportConfig() {
        viewModelScope.launch {
            // Export configuration as JSON or XML
            // Implementation would depend on specific export mechanism
        }
    }

    fun importConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Import configuration from file
            // Implementation would depend on specific import mechanism
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BaseConfigScreenPreview() {
    IRCameraTheme {
        BaseConfigScreen(
            viewModel = BaseConfigViewModel(),
            onBackPressed = { }
        )
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\activity\PseudoSetComposeActivity.kt =====

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
                        Text("Min: ${uiState.minTemp}Â°C")
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
                        Text("Max: ${uiState.maxTemp}Â°C")
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