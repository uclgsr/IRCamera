package com.mpdc4gsr.module.user.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.user.viewmodel.MoreFragmentComposeViewModel

/**
 * Compose migration of MoreFragment - Minimal working version
 */
class MoreFragmentCompose : BaseComposeFragment<MoreFragmentComposeViewModel>() {

    override fun createViewModel(): MoreFragmentComposeViewModel {
        return viewModels<MoreFragmentComposeViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MoreFragmentComposeViewModel) {
        LibUnifiedTheme {
            Column(
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
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Additional Features",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Explore more tools and settings",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.primary,
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
        }
    }
}
