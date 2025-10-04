package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ReportPickImgComposeActivity : BaseComposeActivity<ReportPickImgViewModel>() {

    override fun createViewModel(): ReportPickImgViewModel {
        return ReportPickImgViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPickImgViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Select Report Images",
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
                            IconButton(onClick = { /* TODO: Implement search
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                            IconButton(onClick = { /* TODO: Implement filter
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPickImgContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ReportPickImgContent(
        viewModel: ReportPickImgViewModel,
        modifier: Modifier = Modifier
    ) {
        var selectedImages by remember { mutableStateOf(setOf<Int>()) }
        var filterCriteria by remember { mutableStateOf("All Images") }
        var showAIRecommendations by remember { mutableStateOf(true) }

        val thermalImages = remember {
            (1..20).map { index ->
                ReportThermalImage(
                    id = index,
                    name = "Thermal_IMG_$index.jpg",
                    temperature = (25.0 + index * 2.5),
                    quality = (70 + index * 2).coerceAtMost(100),
                    isRecommended = index % 3 == 0,
                    timestamp = "2024-01-${(index % 30) + 1}"
                )
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selection summary and AI recommendations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Image Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${selectedImages.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Recommendations")
                        Switch(
                            checked = showAIRecommendations,
                            onCheckedChange = { showAIRecommendations = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF1976D2),
                                checkedTrackColor = Color(0xFF1976D2).copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Filter options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Filter Criteria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All Images", "High Quality", "High Temp", "Recommended")
                        filters.forEach { filter ->
                            FilterChip(
                                onClick = { filterCriteria = filter },
                                label = { Text(filter) },
                                selected = filterCriteria == filter,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1976D2),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Image grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(thermalImages.filter { image ->
                    when (filterCriteria) {
                        "High Quality" -> image.quality >= 90
                        "High Temp" -> image.temperature >= 40.0
                        "Recommended" -> image.isRecommended
                        else -> true
                    }
                }) { image ->
                    ThermalImageCard(
                        image = image,
                        isSelected = selectedImages.contains(image.id),
                        showRecommendation = showAIRecommendations,
                        onSelectionChange = { isSelected ->
                            selectedImages = if (isSelected) {
                                selectedImages + image.id
                            } else {
                                selectedImages - image.id
                            }
                        }
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedImages = setOf() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = {
                        // Auto-select recommended images
                        selectedImages = thermalImages
                            .filter { it.isRecommended }
                            .map { it.id }
                            .toSet()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto Select")
                }

                Button(
                    onClick = { /* TODO: Implement add to report
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    enabled = selectedImages.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Report")
                }
            }
        }
    }

    @Composable
    private fun ThermalImageCard(
        image: ReportThermalImage,
        isSelected: Boolean,
        showRecommendation: Boolean,
        onSelectionChange: (Boolean) -> Unit
    ) {
        Card(
            onClick = { onSelectionChange(!isSelected) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
            ),
            border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Image placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "${image.temperature.toInt()}°C",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Thermal Image",
                                color = Color(0xFF9E9E9E),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Recommendation badge
                    if (showRecommendation && image.isRecommended) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Recommended",
                                modifier = Modifier.padding(4.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Selection indicator
                    if (isSelected) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.padding(4.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Image info
                Text(
                    image.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Quality: ${image.quality}% | ${image.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

private data class ReportThermalImage(
    val id: Int,
    val name: String,
    val temperature: Double,
    val quality: Int,
    val isRecommended: Boolean,
    val timestamp: String
)

class ReportPickImgViewModel : BaseViewModel()