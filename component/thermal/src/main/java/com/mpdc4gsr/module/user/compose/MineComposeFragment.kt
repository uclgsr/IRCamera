package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.component.shared.app.config.RouterConfig
import com.mpdc4gsr.component.shared.app.navigation.NavigationManager
import com.mpdc4gsr.component.shared.app.utils.Constants
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel
import com.mpdc4gsr.component.shared.R as RCore

@Composable
@Suppress("FunctionName")
fun MineComposeFragment(
    viewModel: MineFragmentViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val showWinterPoint by viewModel.showWinterPoint.collectAsState()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // User Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // User Avatar
                Surface(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // User Info
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = userProfile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                // Winter Easter Egg
                if (showWinterPoint) {
                    IconButton(
                        onClick = { viewModel.onWinterEggClick() },
                    ) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = "Winter",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        // Settings Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_version),
                    leftIcon = Icons.Default.Info,
                    showLine = true,
                    onClick = {
                        NavigationManager
                            .getInstance()
                            .build(RouterConfig.VERSION)
                            .navigation(context)
                    },
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.set_clear_cache),
                    leftIcon = Icons.Default.Delete,
                    showLine = true,
                    onClick = { viewModel.clearCache() },
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.electronic_manual),
                    leftIcon = Icons.Default.Book,
                    showLine = true,
                    onClick = {
                        NavigationManager
                            .getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_BOOK)
                            .navigation(context)
                    },
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.app_question),
                    leftIcon = Icons.AutoMirrored.Filled.Help,
                    showLine = true,
                    onClick = {
                        NavigationManager
                            .getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_FAQ)
                            .navigation(context)
                    },
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_feedback),
                    leftIcon = Icons.Default.Feedback,
                    showLine = true,
                    onClick = {
                        // Feedback navigation
                    },
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_unit),
                    leftIcon = Icons.Default.Settings,
                    onClick = {
                        NavigationManager
                            .getInstance()
                            .build(RouterConfig.UNIT)
                            .navigation(context)
                    },
                )
            }
        }
    }
}


