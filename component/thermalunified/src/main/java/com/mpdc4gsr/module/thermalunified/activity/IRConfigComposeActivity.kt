package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.viewmodel.IRConfigViewModel

class IRConfigComposeActivity : BaseComposeActivity<IRConfigViewModel>() {

    private var isTC007 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        super.onCreate(savedInstanceState)
    }

    override fun createViewModel(): IRConfigViewModel {
        return viewModels<IRConfigViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRConfigViewModel) {
        var useDefaultModel by remember { mutableStateOf(true) }
        var environmentTemp by remember { mutableFloatStateOf(25.0f) }
        var distance by remember { mutableFloatStateOf(1.0f) }
        var emissivity by remember { mutableFloatStateOf(0.95f) }
        var selectedMaterial by remember { mutableStateOf<MaterialPreset?>(null) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Model Configuration",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Default Model Selection
                    item {
                        DefaultModelCard(
                            isSelected = useDefaultModel,
                            onSelectionChange = { useDefaultModel = it }
                        )
                    }

                    // Environment Temperature
                    item {
                        ConfigurationCard(
                            title = "Environment Temperature",
                            subtitle = "${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}",
                            value = "${environmentTemp.toInt()}°${UnitTools.showUnit()}",
                            icon = Icons.Default.Thermostat,
                            onClick = { /* TODO: Implement show temperature dialog
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }
                        )
                    }

                    // Distance Configuration  
                    item {
                        ConfigurationCard(
                            title = "Measurement Distance",
                            subtitle = "(0.2~${if (isTC007) 4 else 5}m)",
                            value = "${distance}m",
                            icon = Icons.Default.Straighten,
                            onClick = { /* TODO: Implement show distance dialog
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }
                        )
                    }

                    // Emissivity Configuration
                    item {
                        ConfigurationCard(
                            title = "Emissivity",
                            subtitle = "(${if (isTC007) "0.1" else "0.01"}~1.00)",
                            value = String.format("%.2f", emissivity),
                            icon = Icons.Default.Tune,
                            onClick = { /* TODO: Implement show emissivity dialog
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }
                        )
                    }

                    // Material Presets Section
                    item {
                        Text(
                            "Material Presets",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(getMaterialPresets()) { material ->
                        MaterialPresetCard(
                            material = material,
                            isSelected = selectedMaterial?.name == material.name,
                            onClick = {
                                selectedMaterial = material
                                emissivity = material.emissivity
                                useDefaultModel = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultModelCard(
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
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
            Column {
                Text(
                    "Default Model",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Use system default thermal settings",
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = { onSelectionChange(!isSelected) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFFF6B35),
                    unselectedColor = Color(0xFF7D8590)
                )
            )
        }
    }
}

@Composable
private fun ConfigurationCard(
    title: String,
    subtitle: String,
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
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
    material: MaterialPreset,
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
                    material.icon,
                    contentDescription = material.name,
                    tint = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        material.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "ε = ${material.emissivity}",
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Data classes and helper functions
data class MaterialPreset(
    val name: String,
    val emissivity: Float,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getMaterialPresets(): List<MaterialPreset> {
    return listOf(
        MaterialPreset("Human Skin", 0.98f, Icons.Default.Person),
        MaterialPreset("Concrete", 0.95f, Icons.Default.Home),
        MaterialPreset("Metal (Polished)", 0.07f, Icons.Default.Build),
        MaterialPreset("Metal (Oxidized)", 0.85f, Icons.Default.Build),
        MaterialPreset("Glass", 0.90f, Icons.Default.Home),
        MaterialPreset("Water", 0.95f, Icons.Default.Home),
        MaterialPreset("Wood", 0.90f, Icons.Default.Home),
        MaterialPreset("Plastic", 0.94f, Icons.Default.Build),
        MaterialPreset("Paper", 0.92f, Icons.Default.Home),
        MaterialPreset("Ceramic", 0.90f, Icons.Default.Home)
    )
}