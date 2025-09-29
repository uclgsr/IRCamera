package com.mpdc4gsr.module.user.fragment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.lms.feedback.activity.FeedbackActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.module.user.viewmodel.MoreViewModel

/**
 * Compose migration of MoreFragment
 *
 * This fragment demonstrates:
 * - Complete migration of additional features UI to Compose
 * - Enhanced help and support section
 * - Better organization of utility features
 * - Modern Material 3 design with improved UX
 * - Integration with external apps and services
 */
class MoreFragmentCompose : BaseComposeFragment<MoreViewModel>() {

    override fun createViewModel(): MoreViewModel {
        return viewModels<MoreViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MoreViewModel) {
        val context = LocalContext.current

        // Observe ViewModel state
        val quickActions by viewModel.quickActions.collectAsStateWithLifecycle()
        val helpResources by viewModel.helpResources.collectAsStateWithLifecycle()
        val communityLinks by viewModel.communityLinks.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "More Features",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Additional tools and resources for thermal imaging",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Quick Actions section
                item {
                    SectionHeader("Quick Actions")
                }

                val quickActionItems = getQuickActionItems()
                items(quickActionItems) { action ->
                    QuickActionCard(
                        action = action,
                        onClick = {
                            handleQuickActionClick(context, action, viewModel)
                        }
                    )
                }

                // Help & Support section
                item {
                    SectionHeader("Help & Support")
                }

                val helpSupportItems = getHelpSupportItems()
                items(helpSupportItems) { item ->
                    HelpSupportCard(
                        item = item,
                        onClick = {
                            handleHelpSupportClick(context, item, viewModel)
                        }
                    )
                }

                // Community section
                item {
                    SectionHeader("Community")
                }

                items(getCommunityItems()) { item ->
                    CommunityCard(
                        item = item,
                        onClick = {
                            handleCommunityClick(context, item, viewModel)
                        }
                    )
                }

                // Advanced Tools section
                item {
                    SectionHeader("Advanced Tools")
                }

                items(getAdvancedToolItems()) { tool ->
                    AdvancedToolCard(
                        tool = tool,
                        onClick = {
                            handleAdvancedToolClick(context, tool, viewModel)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    @Composable
    private fun QuickActionCard(
        action: QuickActionItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = action.backgroundColor
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
                    imageVector = action.icon,
                    contentDescription = action.title,
                    modifier = Modifier.size(32.dp),
                    tint = action.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = action.textColor
                    )
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = action.textColor.copy(alpha = 0.8f)
                    )
                }

                if (action.badge.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = action.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HelpSupportCard(
        item: HelpSupportItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun CommunityCard(
        item: CommunityItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open External",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    @Composable
    private fun AdvancedToolCard(
        tool: AdvancedToolItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    if (tool.isExperimental) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "BETA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )

                if (tool.requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Requires: ${tool.requirements}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    private fun getQuickActionItems(): List<QuickActionItem> {
        return listOf(
            QuickActionItem(
                id = "calibrate",
                title = "Quick Calibration",
                description = "Calibrate thermal camera quickly",
                icon = Icons.Default.Tune,
                backgroundColor = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.1f),
                iconTint = androidx.compose.ui.graphics.Color.Blue,
                textColor = androidx.compose.ui.graphics.Color.Blue,
                badge = "QUICK"
            ),
            QuickActionItem(
                id = "export",
                title = "Export Data",
                description = "Export thermal imaging data",
                icon = Icons.Default.FileDownload,
                backgroundColor = androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.1f),
                iconTint = androidx.compose.ui.graphics.Color.Green,
                textColor = androidx.compose.ui.graphics.Color.Green
            ),
            QuickActionItem(
                id = "share",
                title = "Share Analysis",
                description = "Share thermal analysis results",
                icon = Icons.Default.Share,
                backgroundColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.1f),
                iconTint = androidx.compose.ui.graphics.Color.Red,
                textColor = androidx.compose.ui.graphics.Color.Red
            )
        )
    }

    private fun getHelpSupportItems(): List<HelpSupportItem> {
        return listOf(
            HelpSupportItem(
                id = "user_guide",
                title = "User Guide",
                description = "Complete thermal imaging guide",
                icon = Icons.Default.MenuBook
            ),
            HelpSupportItem(
                id = "faq",
                title = "Frequently Asked Questions",
                description = "Common questions and answers",
                icon = Icons.Default.QuestionAnswer
            ),
            HelpSupportItem(
                id = "troubleshooting",
                title = "Troubleshooting",
                description = "Fix common issues",
                icon = Icons.Default.Build
            ),
            HelpSupportItem(
                id = "contact_support",
                title = "Contact Support",
                description = "Get help from our team",
                icon = Icons.Default.ContactSupport
            )
        )
    }

    private fun getCommunityItems(): List<CommunityItem> {
        return listOf(
            CommunityItem(
                id = "forum",
                title = "Community Forum",
                description = "Discuss with other users",
                icon = Icons.Default.Forum,
                url = "https://community.thermalcamera.com"
            ),
            CommunityItem(
                id = "github",
                title = "GitHub Repository",
                description = "View source code and contribute",
                icon = Icons.Default.Code,
                url = "https://github.com/uclgsr/IRCamera"
            ),
            CommunityItem(
                id = "discord",
                title = "Discord Server",
                description = "Real-time chat with community",
                icon = Icons.Default.Chat,
                url = "https://discord.gg/thermalcamera"
            )
        )
    }

    private fun getAdvancedToolItems(): List<AdvancedToolItem> {
        return listOf(
            AdvancedToolItem(
                id = "thermal_analysis",
                title = "Advanced Thermal Analysis",
                description = "Deep thermal data analysis with ML",
                icon = Icons.Default.Analytics,
                requirements = "Pro license",
                isExperimental = false
            ),
            AdvancedToolItem(
                id = "batch_processing",
                title = "Batch Processing",
                description = "Process multiple thermal images",
                icon = Icons.Default.GridView,
                requirements = "High-end device",
                isExperimental = true
            ),
            AdvancedToolItem(
                id = "ai_detection",
                title = "AI Object Detection",
                description = "AI-powered thermal object detection",
                icon = Icons.Default.Psychology,
                requirements = "Internet connection",
                isExperimental = true
            )
        )
    }

    private fun handleQuickActionClick(
        context: android.content.Context,
        action: QuickActionItem,
        viewModel: MoreViewModel
    ) {
        when (action.id) {
            "calibrate" -> viewModel.startQuickCalibration()
            "export" -> viewModel.exportData()
            "share" -> viewModel.shareAnalysis()
        }
    }

    private fun handleHelpSupportClick(
        context: android.content.Context,
        item: HelpSupportItem,
        viewModel: MoreViewModel
    ) {
        when (item.id) {
            "user_guide" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.USER_GUIDE)
                    .navigation(context)
            }

            "faq" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.FAQ)
                    .navigation(context)
            }

            "troubleshooting" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.TROUBLESHOOTING)
                    .navigation(context)
            }

            "contact_support" -> {
                val intent = Intent(context, FeedbackActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    private fun handleCommunityClick(
        context: android.content.Context,
        item: CommunityItem,
        viewModel: MoreViewModel
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
        context.startActivity(intent)
    }

    private fun handleAdvancedToolClick(
        context: android.content.Context,
        tool: AdvancedToolItem,
        viewModel: MoreViewModel
    ) {
        when (tool.id) {
            "thermal_analysis" -> viewModel.openAdvancedAnalysis()
            "batch_processing" -> viewModel.openBatchProcessing()
            "ai_detection" -> viewModel.openAIDetection()
        }
    }

    // Data classes
    data class QuickActionItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val backgroundColor: Color,
        val iconTint: Color,
        val textColor: Color,
        val badge: String = ""
    )

    data class HelpSupportItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector
    )

    data class CommunityItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val url: String
    )

    data class AdvancedToolItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val requirements: String,
        val isExperimental: Boolean
    )
}