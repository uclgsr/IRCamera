package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Profile Screen - User profile and research data management
 * Provides user account information and research template management
 */
@Composable
fun ProfileScreen(
    onBackClick: (() -> Unit)? = null,
    onNavigateToEditProfile: (() -> Unit)? = null,
    onNavigateToResearchTemplates: (() -> Unit)? = null,
    onExportData: (() -> Unit)? = null,
    onNavigateToPreferences: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar
        TitleBar(
            title = "Profile",
            showBackButton = true,
            onBackClick = onBackClick
        )

        // Profile content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User profile card
            UserProfileCard(
                onNavigateToEditProfile = onNavigateToEditProfile
            )

            // Research statistics
            ResearchStatsCard()

            // Recent activities
            RecentActivitiesCard()

            // Quick actions
            QuickActionsCard(
                onNavigateToResearchTemplates = onNavigateToResearchTemplates,
                onExportData = onExportData,
                onNavigateToPreferences = onNavigateToPreferences
            )
        }
    }
}

/**
 * User profile information card
 */
@Composable
private fun UserProfileCard(
    onNavigateToEditProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile avatar
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User information
            Text(
                text = "Research User",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "researcher@university.edu",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "University Research Lab",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Edit profile button
            Button(
                onClick = {
                    onNavigateToEditProfile?.invoke() ?: run {
                        android.widget.Toast.makeText(
                            context,
                            "Edit profile feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }
    }
}

/**
 * Research statistics card
 */
@Composable
private fun ResearchStatsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Research Statistics",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Sessions",
                    value = "47",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Hours Recorded",
                    value = "23.5",
                    color = Color.Green
                )
                StatItem(
                    label = "Data Exported",
                    value = "156MB",
                    color = Color.Cyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressItem(
                    label = "GSR Sessions",
                    current = 25,
                    total = 50,
                    color = Color.Cyan
                )
                ProgressItem(
                    label = "Thermal Images",
                    current = 134,
                    total = 200,
                    color = Color.Red
                )
                ProgressItem(
                    label = "Multi-Modal",
                    current = 12,
                    total = 30,
                    color = Color.Green
                )
            }
        }
    }
}

/**
 * Recent activities card
 */
@Composable
private fun RecentActivitiesCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recent Activities",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val activities = listOf(
                Activity("GSR Session recorded", "2 hours ago", Icons.Default.Sensors),
                Activity("Thermal calibration completed", "1 day ago", Icons.Default.Thermostat),
                Activity("Data exported to CSV", "2 days ago", Icons.Default.FileDownload),
                Activity("Multi-modal recording", "3 days ago", Icons.Default.VideoCall)
            )

            activities.forEach { activity ->
                ActivityItem(activity = activity)
                if (activity != activities.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Quick actions card
 */
@Composable
private fun QuickActionsCard(
    onNavigateToResearchTemplates: (() -> Unit)? = null,
    onExportData: (() -> Unit)? = null,
    onNavigateToPreferences: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.Science,
                    label = "Research Templates",
                    onClick = {
                        onNavigateToResearchTemplates?.invoke() ?: run {
                            android.widget.Toast.makeText(
                                context,
                                "Opening research templates...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Export Data",
                    onClick = {
                        onExportData?.invoke() ?: run {
                            android.widget.Toast.makeText(
                                context,
                                "Exporting data...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    label = "Preferences",
                    onClick = {
                        onNavigateToPreferences?.invoke() ?: run {
                            android.widget.Toast.makeText(
                                context,
                                "Opening preferences...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Statistic item component
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

/**
 * Progress item component
 */
@Composable
private fun ProgressItem(
    label: String,
    current: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "$current/$total",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
    }
}

/**
 * Activity item component
 */
@Composable
private fun ActivityItem(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = activity.icon,
            contentDescription = activity.description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.description,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = activity.timestamp,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Quick action button component
 */
@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Data classes
 */
data class Activity(
    val description: String,
    val timestamp: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    IRCameraTheme {
        ProfileScreen()
    }
}