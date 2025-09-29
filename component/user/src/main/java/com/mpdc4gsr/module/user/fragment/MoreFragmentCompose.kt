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
                            handleQuickActionClick(context, action)
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
                            handleHelpSupportClick(context, item)
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
                        color = if (action.textColor != Color.Unspecified) action.textColor else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                action.badge?.let { badge ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
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
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
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
        context: Context,
        action: QuickActionItem
    ) {
        when (action.id) {
            "calibrate" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_SETTING)
                    .navigation(context)
            }
            "export" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
            "share" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(context)
            }
        }
    }

    private fun handleHelpSupportClick(
        context: Context,
        item: HelpSupportItem
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
        }
    }
}
