package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
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
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class IRGalleryDetail04ComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Gallery Detail",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
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
                            IconButton(onClick = { /* TODO: Implement share functionality
                     *   - Create share intent with data
                     *   - Show system share sheet
                     *   - Handle share completion
                     */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = { /* TODO: Implement more options
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                GalleryDetailContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun GalleryDetailContent(
        modifier: Modifier = Modifier
    ) {
        var showAnalysis by remember { mutableStateOf(true) }
        var showAnnotations by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Display Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ThermostatAuto,
                            contentDescription = "Thermal Image",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Thermal Image Analysis",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (showAnalysis) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Max: 42.5°C", color = Color(0xFFFF6B35), fontSize = 14.sp)
                                    Text("Min: 18.2°C", color = Color(0xFF4A90E2), fontSize = 14.sp)
                                    Text("Avg: 28.7°C", color = Color(0xFF7D8590), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Analysis Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Analysis Tools",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnalysisButton(Icons.Default.CropFree, "Crop", false)
                        AnalysisButton(Icons.AutoMirrored.Filled.RotateLeft, "Rotate", false)
                        AnalysisButton(Icons.Default.Analytics, "Analyze", showAnalysis) {
                            showAnalysis = !showAnalysis
                        }
                        AnalysisButton(Icons.Default.Straighten, "Measure", false)
                        AnalysisButton(Icons.Default.FilterAlt, "Filter", false)
                    }
                }
            }

            // Image Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Image Information",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow("Resolution", "384 x 288")
                    InfoRow("File Size", "2.1 MB")
                    InfoRow("Date", "2024-01-15 14:30")
                    InfoRow("Temperature Range", "-10°C to 50°C")
                    InfoRow("Emissivity", "0.95")
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement export
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }

                Button(
                    onClick = { /* TODO: Implement generate report
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Report")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report")
                }

                IconButton(
                    onClick = { /* TODO: Implement delete
                     *   - Show confirmation dialog
                     *   - Call viewModel.delete(item)
                     *   - Show deletion confirmation
                     */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    }

    @Composable
    private fun AnalysisButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        isActive: Boolean,
        onClick: () -> Unit = {}
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.then(
                if (isActive) Modifier else Modifier
            )
        ) {
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isActive) Color(0xFFFF6B35) else Color(0xFF161B22),
                    contentColor = if (isActive) Color.White else Color(0xFF7D8590)
                )
            ) {
                Icon(icon, contentDescription = label)
            }

            Text(
                label,
                color = if (isActive) Color(0xFFFF6B35) else Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }

    @Composable
    private fun InfoRow(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )

            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}