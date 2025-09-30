package com.mpdc4gsr.module.user.fragment

import android.content.Context
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
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.viewmodel.MoreFragmentComposeViewModel

/**
 * Compose migration of MoreFragment - Minimal working version
 */
class MoreFragmentCompose : BaseComposeFragment<MoreFragmentComposeViewModel>() {

    data class QuickActionItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val backgroundColor: Color,
        val iconTint: Color,
        val textColor: Color = Color.Unspecified,
        val badge: String? = null
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
        val isExperimental: Boolean = false
    )

    override fun createViewModel(): MoreFragmentComposeViewModel {
        return viewModels<MoreFragmentComposeViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MoreFragmentComposeViewModel) {
        val context = LocalContext.current
        val quickActionItems = remember { getQuickActionItems() }
        val helpSupportItems = remember { getHelpSupportItems() }
        val communityItems: List<CommunityItem> = remember { getCommunityItems() }
        val advancedToolItems: List<AdvancedToolItem> = remember { getAdvancedToolItems() }

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

                items(communityItems) { item ->
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

                items(advancedToolItems) { tool ->
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
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = action.textColor
                    )
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = action.textColor.copy(alpha = 0.8f)
                    )
                }
                
                action.badge?.let { badge ->
                    Badge {
                        Text(text = badge)
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
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tool.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            
                            if (tool.isExperimental) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = "BETA",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
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
                backgroundColor = Color.Blue.copy(alpha = 0.1f),
                iconTint = Color.Blue,
                textColor = Color.Blue,
                badge = "QUICK"
            ),
            QuickActionItem(
                id = "export",
                title = "Export Data",
                description = "Export thermal imaging data",
                icon = Icons.Default.FileDownload,
                backgroundColor = Color.Green.copy(alpha = 0.1f),
                iconTint = Color.Green,
                textColor = Color.Green
            ),
            QuickActionItem(
                id = "share",
                title = "Share Analysis",
                description = "Share thermal analysis results",
                icon = Icons.Default.Share,
                backgroundColor = Color.Red.copy(alpha = 0.1f),
                iconTint = Color.Red,
                textColor = Color.Red
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

    private fun handleQuickActionClick(
        context: android.content.Context,
        action: QuickActionItem,
        viewModel: MoreFragmentComposeViewModel
    ) {
        when (action.id) {
            "calibrate" -> {
                // Quick calibration logic can be added to ViewModel later
                android.widget.Toast.makeText(
                    context,
                    "Calibration feature is not yet implemented.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            "export" -> {
                // Export data logic can be added to ViewModel later
                android.widget.Toast.makeText(
                    context,
                    "Export feature is not yet implemented.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            "share" -> {
                // Share analysis logic can be added to ViewModel later
                android.widget.Toast.makeText(
                    context,
                    "Share feature is not yet implemented.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleHelpSupportClick(
        context: android.content.Context,
        item: HelpSupportItem,
        viewModel: MoreFragmentComposeViewModel
    ) {
        when (item.id) {
            "user_guide" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
            "faq" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
            "troubleshooting" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
            "contact_support" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
            // Add other help support actions as needed
        }
    }

    private fun handleCommunityClick(
        context: android.content.Context,
        item: CommunityItem,
        viewModel: MoreFragmentComposeViewModel
    ) {
        // Community click logic can be implemented later
        // For now, show a placeholder message
        android.widget.Toast.makeText(
            context,
            "Community feature not yet implemented.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleAdvancedToolClick(
        context: android.content.Context,
        tool: AdvancedToolItem,
        viewModel: MoreFragmentComposeViewModel
    ) {
        // Advanced tool click logic can be implemented later
        // For now, we'll just handle the click without action
    }

    private fun getCommunityItems(): List<CommunityItem> {
        return listOf(
            CommunityItem(
                id = "forum",
                title = "Community Forum",
                description = "Join discussions with other thermal imaging professionals",
                icon = Icons.Default.Forum,
                url = "https://example.com/forum"
            ),
            CommunityItem(
                id = "tutorials",
                title = "Video Tutorials",
                description = "Learn from expert-created video content",
                icon = Icons.Default.VideoLibrary,
                url = "https://example.com/tutorials"
            ),
            CommunityItem(
                id = "github",
                title = "Open Source",
                description = "Contribute to thermal imaging software development",
                icon = Icons.Default.Code,
                url = "https://github.com/uclgsr/IRCamera"
            )
        )
    }

    private fun getAdvancedToolItems(): List<AdvancedToolItem> {
        return listOf(
            AdvancedToolItem(
                id = "batch_analysis",
                title = "Batch Analysis",
                description = "Process multiple thermal images simultaneously",
                icon = Icons.Default.DynamicFeed,
                requirements = "Professional license required",
                isExperimental = false
            ),
            AdvancedToolItem(
                id = "ai_detection",
                title = "AI Anomaly Detection",
                description = "Automatically detect thermal anomalies using machine learning",
                icon = Icons.Default.Psychology,
                requirements = "GPU acceleration recommended",
                isExperimental = true
            ),
            AdvancedToolItem(
                id = "data_export",
                title = "Advanced Data Export",
                description = "Export raw thermal data in multiple formats",
                icon = Icons.Default.GetApp,
                requirements = "Storage space required",
                isExperimental = false
            )
        )
    }
}
