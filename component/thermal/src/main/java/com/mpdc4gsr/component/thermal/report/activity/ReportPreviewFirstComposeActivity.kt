package com.mpdc4gsr.component.thermal.report.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.viewmodel.ReportPreviewViewModel

class ReportPreviewFirstComposeActivity : BaseComposeActivity<ReportPreviewViewModel>() {
    override val requiredPermissionAreas: Set<FeaturePermissionArea> = setOf(FeaturePermissionArea.MEDIA_REVIEW)

    override fun createViewModel(): ReportPreviewViewModel = viewModels<ReportPreviewViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPreviewViewModel) {
        var selectedLayout by remember { mutableIntStateOf(0) }
        var showConfirmDialog by remember { mutableStateOf(false) }
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Report Preview",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    // TODO: Proceed to next report creation step
                                    android.widget.Toast
                                        .makeText(
                                            this@ReportPreviewFirstComposeActivity,
                                            "Proceeding to next step...",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                },
                            ) {
                                Text("Next", color = Color.White)
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF16131E),
                            ),
                    )
                },
                containerColor = Color(0xFF16131E),
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Preview area
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A),
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "Report Preview",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.3f),
                                )
                                Text(
                                    "Report Preview",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Layout ${selectedLayout + 1}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                )
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(0.8f)
                                            .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(16.dp)
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp)),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(0.9f)
                                                .height(16.dp)
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp)),
                                    )
                                }
                            }
                        }
                    }
                    // Layout selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A),
                            ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Select Layout",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(3) { index ->
                                    LayoutOption(
                                        index = index,
                                        selected = selectedLayout == index,
                                        onClick = { selectedLayout = index },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    // Options
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A),
                            ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Preview Options",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            var showImages by remember { mutableStateOf(true) }
                            var showMetadata by remember { mutableStateOf(true) }
                            var showWatermark by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Show Images", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showImages,
                                    onCheckedChange = { showImages = it },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Show Metadata", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showMetadata,
                                    onCheckedChange = { showMetadata = it },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Add Watermark", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showWatermark,
                                    onCheckedChange = { showWatermark = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LayoutOption(
        index: Int,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Card(
            modifier = modifier,
            onClick = onClick,
            colors =
                CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A),
                ),
            border =
                if (selected) {
                    null
                } else {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.2f),
                    )
                },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "Layout ${index + 1}",
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                )
                Text(
                    "Layout ${index + 1}",
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}



