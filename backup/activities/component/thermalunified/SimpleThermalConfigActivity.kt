package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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

/**
 * Simplified thermal configuration activity demonstrating scalable patterns
 * Shows how to quickly implement thermal settings with minimal complexity
 */
class SimpleThermalConfigActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var environmentTemp by remember { mutableFloatStateOf(25.0f) }
        var distance by remember { mutableFloatStateOf(1.0f) }
        var emissivity by remember { mutableFloatStateOf(0.95f) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Settings",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {

                    // Temperature Setting
                    item {
                        SettingCard(
                            title = "Environment Temperature",
                            value = "${environmentTemp.toInt()}°C",
                            icon = Icons.Default.Settings,
                            onClick = { /* Handle temperature change */ }
                        )
                    }

                    // Distance Setting
                    item {
                        SettingCard(
                            title = "Measurement Distance",
                            value = "${distance}m",
                            icon = Icons.Default.Settings,
                            onClick = { /* Handle distance change */ }
                        )
                    }

                    // Emissivity Setting
                    item {
                        SettingCard(
                            title = "Emissivity",
                            value = String.format("%.2f", emissivity),
                            icon = Icons.Default.Build,
                            onClick = { /* Handle emissivity change */ }
                        )
                    }

                    // Material Presets
                    item {
                        Text(
                            "Quick Presets",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        MaterialPresetCard(
                            name = "Human Skin",
                            emissivity = 0.98f,
                            icon = Icons.Default.Person,
                            isSelected = emissivity == 0.98f,
                            onClick = { emissivity = 0.98f }
                        )
                    }

                    item {
                        MaterialPresetCard(
                            name = "Metal",
                            emissivity = 0.85f,
                            icon = Icons.Default.Build,
                            isSelected = emissivity == 0.85f,
                            onClick = { emissivity = 0.85f }
                        )
                    }

                    item {
                        MaterialPresetCard(
                            name = "Concrete",
                            emissivity = 0.95f,
                            icon = Icons.Default.Home,
                            isSelected = emissivity == 0.95f,
                            onClick = { emissivity = 0.95f }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                value,
                color = Color(0xFFFF6B35),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MaterialPresetCard(
    name: String,
    emissivity: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D1B69) else Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B35))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "ε = $emissivity",
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}