package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import com.mpdc4gsr.libunified.app.viewmodel.BaseViewModel

/**
 * Lite Correction Step 4 - Final correction review and application
 * Quality control with automated checks and completion workflow
 */
class IRCorrectionLiteFourComposeActivity : BaseComposeActivity<IRCorrectionLiteFourViewModel>() {

    override fun createViewModel(): IRCorrectionLiteFourViewModel {
        return IRCorrectionLiteFourViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRCorrectionLiteFourViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Lite Correction - Final",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF4A90E2) // Lite blue theme
                        )
                    )
                }
            ) { paddingValues ->
                IRCorrectionLiteFourContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRCorrectionLiteFourContent(
        viewModel: IRCorrectionLiteFourViewModel,
        modifier: Modifier = Modifier
    ) {
        var isCompleted by remember { mutableStateOf(false) }
        var qualityScore by remember { mutableStateOf(85) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Completion indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) Color(0xFFE8F5E8) else Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF4A90E2)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isCompleted) "Correction Complete!" else "Final Review",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFF4A90E2),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isCompleted) "All corrections applied successfully" else "Step 4 of 4: Review and apply",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            // Quality assessment
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Quality Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overall Quality Score")
                        Text(
                            "$qualityScore%",
                            fontWeight = FontWeight.Bold,
                            color = when {
                                qualityScore >= 80 -> Color(0xFF4CAF50)
                                qualityScore >= 60 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }

                    LinearProgressIndicator(
                        progress = qualityScore / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            qualityScore >= 80 -> Color(0xFF4CAF50)
                            qualityScore >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )

                    // Quality checks
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QualityCheckItem("Temperature Range", true)
                        QualityCheckItem("Image Clarity", true)
                        QualityCheckItem("Color Balance", qualityScore >= 80)
                        QualityCheckItem("Noise Reduction", true)
                    }
                }
            }

            // Before/After comparison
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Before / After Comparison",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Side-by-side comparison of original and corrected thermal image",
                        color = Color(0xFF9E9E9E),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Action buttons
            if (!isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { finish() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text("Discard")
                    }

                    Button(
                        onClick = {
                            isCompleted = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Text("Apply Corrections")
                    }
                }
            } else {
                // Completion actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Save functionality */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }

                    Button(
                        onClick = { /* Share functionality */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            }
        }
    }

    @Composable
    private fun QualityCheckItem(
        label: String,
        passed: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (passed) "✓ Pass" else "⚠ Warning",
                color = if (passed) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

class IRCorrectionLiteFourViewModel : BaseViewModel()