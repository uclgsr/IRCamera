package mpdc4gsr.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme
import java.util.Calendar

/**
 * Compose version of an About Activity demonstrating app information display.
 * Shows how to handle app details, credits, and external links in Compose.
 */
class AboutComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            AboutScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AboutScreen() {
        val context = LocalContext.current
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "About",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // App Info Header
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // App Icon (placeholder)
                            Card(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF6B35FF)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = "App Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "IRCamera",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Multi-Modal Sensor Platform",
                                color = Color(0x80FFFFFF),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Version 1.0.0 (Build 123)",
                                color = Color(0xFF6B35FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // App Description
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "About IRCamera",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "IRCamera is a comprehensive multi-modal sensor platform designed for research and data collection. It integrates thermal imaging, GSR sensors, RGB cameras, and network connectivity to provide synchronized data recording across multiple sensor modalities.",
                                color = Color(0xCCFFFFFF),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Features Section
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Key Features",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val features = listOf(
                                "Thermal imaging with TC001/TS004 cameras",
                                "GSR sensor integration with Shimmer3",
                                "Synchronized multi-modal recording",
                                "PC controller network integration",
                                "Fault-tolerant recording system",
                                "Real-time data visualization",
                                "Session management and export"
                            )

                            features.forEach { feature ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Feature",
                                        tint = Color(0xFF6B35FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feature,
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Technical Info
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Technical Information",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            InfoRow("Platform", "Android")
                            InfoRow("Minimum SDK", "API 24 (Android 7.0)")
                            InfoRow("Target SDK", "API 34 (Android 14)")
                            InfoRow("Architecture", "MVVM with Jetpack Compose")
                            InfoRow("Data Format", "CSV, JSON, Binary")
                            InfoRow("Network Protocol", "TCP/UDP, WebSocket")
                        }
                    }
                }

                // Developer Team
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Development Team",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val teamMembers = listOf(
                                "Research Team - UCL GSR Project",
                                "Thermal Imaging Integration",
                                "GSR Sensor Development",
                                "Network Architecture",
                                "UI/UX Design"
                            )

                            teamMembers.forEach { member ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Team Member",
                                        tint = Color(0xFF6B35FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = member,
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // External Links
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Links & Resources",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val links = listOf(
                                LinkItem(Icons.Default.Web, "Project Website", "https://ucl-gsr.github.io"),
                                LinkItem(Icons.Default.Code, "Source Code", "https://github.com/uclgsr/IRCamera"),
                                LinkItem(Icons.Default.Description, "Documentation", "https://docs.ucl-gsr.org"),
                                LinkItem(Icons.Default.Email, "Support", "mailto:support@ucl-gsr.org"),
                                LinkItem(Icons.Default.BugReport, "Report Issues", "https://github.com/uclgsr/IRCamera/issues")
                            )

                            links.forEach { link ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            openLink(context, link.url)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = link.icon,
                                        contentDescription = link.title,
                                        tint = Color(0xFF6B35FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = link.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open",
                                        tint = Color(0x80FFFFFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Legal Information
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Legal Information",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "© 2023-$currentYear UCL GSR Research Project. All rights reserved.",
                                color = Color(0x80FFFFFF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "This software is licensed under the MIT License. See the LICENSE file for details.",
                                color = Color(0x80FFFFFF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Third-party libraries and their licenses are listed in the THIRD_PARTY_NOTICES file.",
                                color = Color(0x80FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }

    private fun openLink(context: android.content.Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link: $url", Toast.LENGTH_SHORT).show()
        }
    }

    private data class LinkItem(
        val icon: ImageVector,
        val title: String,
        val url: String
    )
}