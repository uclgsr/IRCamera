package com.mpdc4gsr.module.thermalunified.activity
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
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
class IREmissivityComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedEmissivity by remember { mutableFloatStateOf(0.95f) }
        var selectedCategory by remember { mutableStateOf("Common Materials") }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Emissivity Selection",
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Current selection display
                    CurrentSelectionCard(
                        selectedEmissivity = selectedEmissivity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    // Material categories and list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category sections
                        getEmissivityCategories().forEach { category ->
                            item {
                                EmissivityCategorySection(
                                    category = category,
                                    selectedEmissivity = selectedEmissivity,
                                    onEmissivitySelected = { emissivity ->
                                        selectedEmissivity = emissivity
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CurrentSelectionCard(
    selectedEmissivity: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    "Current Emissivity",
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    String.format("ε = %.3f", selectedEmissivity),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Visual indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFF6B35),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Emissivity",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@Composable
private fun EmissivityCategorySection(
    category: EmissivityCategory,
    selectedEmissivity: Float,
    onEmissivitySelected: (Float) -> Unit
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
            // Category header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    category.icon,
                    contentDescription = category.name,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    category.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Materials in this category
            category.materials.forEach { material ->
                EmissivityMaterialItem(
                    material = material,
                    isSelected = selectedEmissivity == material.emissivity,
                    onClick = { onEmissivitySelected(material.emissivity) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
private fun EmissivityMaterialItem(
    material: EmissivityMaterial,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D1B69) else Color(0xFF16131E)
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
            Column {
                Text(
                    material.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (material.description.isNotEmpty()) {
                    Text(
                        material.description,
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    String.format("%.3f", material.emissivity),
                    color = if (isSelected) Color(0xFFFF6B35) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
// Data classes
data class EmissivityCategory(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val materials: List<EmissivityMaterial>
)
data class EmissivityMaterial(
    val name: String,
    val emissivity: Float,
    val description: String = ""
)
private fun getEmissivityCategories(): List<EmissivityCategory> {
    return listOf(
        EmissivityCategory(
            name = "Common Materials",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Human Skin", 0.980f, "Body temperature measurement"),
                EmissivityMaterial("Water", 0.950f, "Clean water surface"),
                EmissivityMaterial("Concrete", 0.950f, "Rough concrete surface"),
                EmissivityMaterial("Asphalt", 0.930f, "Road surface"),
                EmissivityMaterial("Wood", 0.900f, "Natural wood surface"),
                EmissivityMaterial("Paper", 0.920f, "White paper"),
                EmissivityMaterial("Fabric", 0.900f, "Cotton/polyester cloth")
            )
        ),
        EmissivityCategory(
            name = "Metals",
            icon = Icons.Default.Build,
            materials = listOf(
                EmissivityMaterial("Aluminum (polished)", 0.050f, "Mirror finish"),
                EmissivityMaterial("Aluminum (oxidized)", 0.300f, "Weathered surface"),
                EmissivityMaterial("Copper (polished)", 0.070f, "Bright copper"),
                EmissivityMaterial("Copper (oxidized)", 0.780f, "Green patina"),
                EmissivityMaterial("Steel (polished)", 0.080f, "Stainless steel"),
                EmissivityMaterial("Steel (oxidized)", 0.850f, "Rusty steel"),
                EmissivityMaterial("Iron (cast)", 0.810f, "Cast iron surface"),
                EmissivityMaterial("Brass (polished)", 0.060f, "Bright brass")
            )
        ),
        EmissivityCategory(
            name = "Building Materials",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Brick", 0.930f, "Red clay brick"),
                EmissivityMaterial("Glass", 0.900f, "Window glass"),
                EmissivityMaterial("Plaster", 0.920f, "Wall plaster"),
                EmissivityMaterial("Tiles (ceramic)", 0.900f, "Glazed ceramic"),
                EmissivityMaterial("Paint", 0.900f, "Most paint colors"),
                EmissivityMaterial("Roofing (shingles)", 0.910f, "Asphalt shingles"),
                EmissivityMaterial("Insulation", 0.950f, "Foam insulation")
            )
        ),
        EmissivityCategory(
            name = "Plastics & Polymers",
            icon = Icons.Default.Build,
            materials = listOf(
                EmissivityMaterial("PVC", 0.940f, "Polyvinyl chloride"),
                EmissivityMaterial("Polyethylene", 0.940f, "PE plastic"),
                EmissivityMaterial("Polystyrene", 0.950f, "Foam cups/packaging"),
                EmissivityMaterial("Teflon", 0.850f, "PTFE coating"),
                EmissivityMaterial("Rubber", 0.950f, "Natural rubber"),
                EmissivityMaterial("Nylon", 0.900f, "Synthetic fabric")
            )
        ),
        EmissivityCategory(
            name = "Food & Organic",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Ice", 0.980f, "Frozen water"),
                EmissivityMaterial("Snow", 0.900f, "Fresh snow"),
                EmissivityMaterial("Vegetation", 0.950f, "Green leaves"),
                EmissivityMaterial("Soil (dry)", 0.900f, "Dry earth"),
                EmissivityMaterial("Soil (wet)", 0.950f, "Moist soil"),
                EmissivityMaterial("Food (cooked)", 0.950f, "Most cooked foods")
            )
        )
    )
}