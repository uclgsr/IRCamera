package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class IRCameraSettingComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedPalette by remember { mutableStateOf("Iron") }
        var frameRate by remember { mutableIntStateOf(9) }
        var autoShutter by remember { mutableStateOf(true) }
        var imageCorrection by remember { mutableStateOf(true) }
        var temperatureUnit by remember { mutableStateOf("Celsius") }
        var resolution by remember { mutableStateOf("384x288") }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Camera Settings",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image Settings
                    item {
                        SettingsCategoryCard(
                            title = "Image Settings",
                            icon = Icons.Default.Image
                        ) {
                            SettingsDropdownItem(
                                title = "Color Palette",
                                selectedValue = selectedPalette,
                                options = listOf("Iron", "Rainbow", "Grayscale", "Hot", "Cool"),
                                onValueChange = { selectedPalette = it }
                            )

                            SettingsSliderItem(
                                title = "Frame Rate",
                                value = frameRate,
                                valueRange = 1f..25f,
                                unit = "fps",
                                onValueChange = { frameRate = it.toInt() }
                            )

                            SettingsDropdownItem(
                                title = "Resolution",
                                selectedValue = resolution,
                                options = listOf("384x288", "640x480", "160x120"),
                                onValueChange = { resolution = it }
                            )
                        }
                    }

                    // Camera Features
                    item {
                        SettingsCategoryCard(
                            title = "Camera Features",
                            icon = Icons.Default.CameraAlt
                        ) {
                            SettingsSwitchItem(
                                title = "Auto Shutter",
                                description = "Automatic shutter calibration",
                                checked = autoShutter,
                                onCheckedChange = { autoShutter = it }
                            )

                            SettingsSwitchItem(
                                title = "Image Correction",
                                description = "Automatic image enhancement",
                                checked = imageCorrection,
                                onCheckedChange = { imageCorrection = it }
                            )
                        }
                    }

                    // Temperature Settings
                    item {
                        SettingsCategoryCard(
                            title = "Temperature Settings",
                            icon = Icons.Default.Thermostat
                        ) {
                            SettingsDropdownItem(
                                title = "Temperature Unit",
                                selectedValue = temperatureUnit,
                                options = listOf("Celsius", "Fahrenheit", "Kelvin"),
                                onValueChange = { temperatureUnit = it }
                            )
                        }
                    }

                    // Advanced Settings
                    item {
                        SettingsCategoryCard(
                            title = "Advanced Settings",
                            icon = Icons.Default.Tune
                        ) {
                            AdvancedSettingItem(
                                title = "Calibration",
                                description = "Manual camera calibration",
                                onClick = { }
                            )

                            AdvancedSettingItem(
                                title = "Firmware Update",
                                description = "Check for camera firmware updates",
                                onClick = { }
                            )

                            AdvancedSettingItem(
                                title = "Factory Reset",
                                description = "Reset camera to default settings",
                                onClick = { }
                            )
                        }
                    }

                    // Save/Reset buttons
                    item {
                        SaveResetButtons(
                            onSave = { },
                            onReset = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6B35),
                    unfocusedBorderColor = Color(0xFF7D8590),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSliderItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$value $unit",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF6B35),
                activeTrackColor = Color(0xFFFF6B35),
                inactiveTrackColor = Color(0xFF7D8590)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF6B35),
                uncheckedThumbColor = Color(0xFF7D8590),
                uncheckedTrackColor = Color(0xFF21262D)
            )
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AdvancedSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SaveResetButtons(
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF7D8590)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
        ) {
            Text("Reset", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold)
        }
    }
}