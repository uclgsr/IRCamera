package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ImageColorViewModel

class ImageColorComposeActivity : BaseComposeActivity<ImageColorViewModel>() {
    override fun createViewModel(): ImageColorViewModel = viewModels<ImageColorViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ImageColorViewModel) {
        val timestamp by viewModel.timestamp.collectAsState()
        val showData by viewModel.showData.collectAsState()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Image Color",
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
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                            ),
                    )
                },
                containerColor = Color.Black,
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                ) {
                    // Horizontal scrollable image comparison
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .background(Color.Black),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // First image
                        Card(
                            modifier =
                                Modifier
                                    .width(250.dp)
                                    .fillMaxHeight(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A1A),
                                ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Thermal Image 1",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        // Second image
                        Card(
                            modifier =
                                Modifier
                                    .width(250.dp)
                                    .fillMaxHeight(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A1A),
                                ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Thermal Image 2",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                    // Timestamp display
                    if (timestamp.isNotEmpty()) {
                        Text(
                            text = timestamp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    // ARGB image display
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A),
                            ),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "ARGB Image",
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    // Control buttons
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Button(
                            onClick = { viewModel.toggleDataDisplay() },
                            modifier = Modifier.width(120.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Text(if (showData) "Hide Data" else "Show Data")
                        }
                        Button(
                            onClick = { },
                            modifier = Modifier.width(120.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                ),
                        ) {
                            Text("Process U4")
                        }
                    }
                }
            }
        }
    }
}
