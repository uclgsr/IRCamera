// Merged .kt under 'component\user' subtree
// Files: 30; Generated 2025-10-07 23:07:40


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\AutoSaveComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.AutoSaveViewModel
import com.mpdc4gsr.libunified.R as RCore

class AutoSaveComposeActivity : BaseComposeActivity<AutoSaveViewModel>() {
    override fun createViewModel(): AutoSaveViewModel {
        return viewModels<AutoSaveViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AutoSaveViewModel) {
        val isAutoSaveEnabled by viewModel.isAutoSaveEnabled.collectAsState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.ts004_auto_save),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Auto Save Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(RCore.string.ts004_auto_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        // Switch Item
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(RCore.string.ts004_auto_save),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(RCore.string.ts004_save_tips),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                                Switch(
                                    checked = isAutoSaveEnabled,
                                    onCheckedChange = { viewModel.updateAutoSaveState(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\DeviceDetailsComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.module.user.viewmodel.DeviceDetailsViewModel
import com.mpdc4gsr.libunified.R as RCore

class DeviceDetailsComposeActivity : BaseComposeActivity<DeviceDetailsViewModel>() {
    override fun createViewModel(): DeviceDetailsViewModel {
        return viewModels<DeviceDetailsViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DeviceDetailsViewModel) {
        val context = LocalContext.current
        val serialNumber by viewModel.serialNumber.collectAsState()
        val deviceModel by viewModel.deviceModel.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        // Get isTC007 from intent extras
        val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        // Load device details on start
        LaunchedEffect(Unit) {
            viewModel.loadDeviceDetails(isTC007)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.more_device_info),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Device Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(RCore.string.more_device_info),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Serial Number Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = serialNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            // Device Model Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(RCore.string.ts004_device_model),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = deviceModel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            // Copy Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(RCore.string.ts004_msg_copy),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        val copyText = viewModel.getCopyText()
                                        val clipboardManager =
                                            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipData =
                                            ClipData.newPlainText("Device Info", copyText)
                                        clipboardManager.setPrimaryClip(clipData)
                                        TToast.shortToast(context, RCore.string.ts004_copy_success)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\ElectronicManualComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.module.user.viewmodel.ElectronicManualViewModel

class ElectronicManualComposeActivity : BaseComposeActivity<ElectronicManualViewModel>() {
    override fun createViewModel(): ElectronicManualViewModel {
        return viewModels<ElectronicManualViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ElectronicManualViewModel) {
        val title by viewModel.title.collectAsState()
        val options by viewModel.options.collectAsState()
        val productType by viewModel.productType.collectAsState()
        // Get product type from intent
        val intentProductType = intent.getIntExtra(Constants.SETTING_TYPE, 0)
        // Load options on start
        LaunchedEffect(Unit) {
            viewModel.loadManualOptions(intentProductType)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { option ->
                    ManualOptionItem(
                        option = option,
                        isBookMode = viewModel.isBookMode(),
                        onClick = {
                            handleOptionClick(option.isTS001, viewModel.isBookMode())
                        }
                    )
                }
            }
        }
    }

    private fun handleOptionClick(isTS001: Boolean, isBookMode: Boolean) {
        if (isTS001) {
            if (isBookMode) {
                // TS001 manual - no action in original implementation
            } else {
                // TS001 FAQ
                NavigationManager.getInstance()
                    .build(RouterConfig.QUESTION)
                    .withBoolean("isTS001", true)
                    .navigation(this)
            }
        } else {
            if (isBookMode) {
                // TS004 manual - PDF viewer
                NavigationManager.getInstance()
                    .build(RouterConfig.PDF)
                    .withBoolean("isTS001", false)
                    .navigation(this)
            } else {
                // TS004 FAQ
                NavigationManager.getInstance()
                    .build(RouterConfig.QUESTION)
                    .withBoolean("isTS001", false)
                    .navigation(this)
            }
        }
    }
}

@Composable
private fun ManualOptionItem(
    option: ElectronicManualViewModel.ManualOption,
    isBookMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBookMode) "User Manual" else "Frequently Asked Questions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\MoreComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.viewmodel.MoreViewModel

class MoreComposeActivity : BaseComposeActivity<MoreViewModel>() {
    override fun createViewModel(): MoreViewModel {
        return viewModels<MoreViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MoreViewModel) {
        val settingsItems by viewModel.settingsItems.collectAsState()
        val isUpgradeAvailable by viewModel.isUpgradeAvailable.collectAsState()
        // Check for updates on start
        LaunchedEffect(Unit) {
            viewModel.checkForUpdates()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "More Settings",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(settingsItems) { item ->
                    SettingsMenuItem(
                        item = item,
                        hasUpgrade = isUpgradeAvailable && item.action == MoreViewModel.SettingsAction.VERSION,
                        onClick = { handleSettingsClick(item.action) }
                    )
                }
            }
        }
    }

    private fun handleSettingsClick(action: MoreViewModel.SettingsAction) {
        when (action) {
            MoreViewModel.SettingsAction.DEVICE_INFORMATION -> {
                // Navigate to device details - for now just finish, would need router setup
                finish()
            }

            MoreViewModel.SettingsAction.TISR -> {
                // Navigate to TISR Compose Activity (would need to be registered in router)
                // For now, use the original activity
                NavigationManager.getInstance()
                    .build(RouterConfig.TISR)
                    .navigation(this)
            }

            MoreViewModel.SettingsAction.STORAGE_SPACE -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.STORAGE_SPACE)
                    .navigation(this)
            }

            MoreViewModel.SettingsAction.AUTO_SAVE -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.AUTO_SAVE)
                    .navigation(this)
            }

            MoreViewModel.SettingsAction.UNIT -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.UNIT)
                    .navigation(this)
            }

            MoreViewModel.SettingsAction.VERSION -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(this)
            }

            MoreViewModel.SettingsAction.DISCONNECT -> {
                // Handle disconnect logic
                finish()
            }

            MoreViewModel.SettingsAction.RESET -> {
                // Handle reset confirmation dialog
                // Original implementation had complex reset logic
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    item: MoreViewModel.SettingsItem,
    hasUpgrade: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getIconForAction(item.action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasUpgrade) {
                    Badge(
                        modifier = Modifier.padding(end = 8.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun getIconForAction(action: MoreViewModel.SettingsAction): ImageVector {
    return when (action) {
        MoreViewModel.SettingsAction.DEVICE_INFORMATION -> Icons.Default.Info
        MoreViewModel.SettingsAction.TISR -> Icons.Default.Settings
        MoreViewModel.SettingsAction.STORAGE_SPACE -> Icons.Default.Build
        MoreViewModel.SettingsAction.AUTO_SAVE -> Icons.Default.Add
        MoreViewModel.SettingsAction.UNIT -> Icons.Default.Settings
        MoreViewModel.SettingsAction.VERSION -> Icons.Default.Info
        MoreViewModel.SettingsAction.DISCONNECT -> Icons.Default.Close
        MoreViewModel.SettingsAction.RESET -> Icons.Default.Refresh
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\QuestionComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.model.QuestionData
import com.mpdc4gsr.module.user.viewmodel.QuestionViewModel

class QuestionComposeActivity : BaseComposeActivity<QuestionViewModel>() {
    override fun createViewModel(): QuestionViewModel {
        return viewModels<QuestionViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: QuestionViewModel) {
        val questions by viewModel.questions.collectAsState()
        // Get isTS001 from intent extras
        val isTS001 = intent.getBooleanExtra("isTS001", false)
        // Load questions on start
        LaunchedEffect(Unit) {
            viewModel.loadQuestions(isTS001)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "FAQ",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(questions) { question ->
                    QuestionItem(
                        question = question,
                        onClick = {
                            NavigationManager.getInstance()
                                .build(RouterConfig.QUESTION_DETAILS)
                                .withString("question", question.question)
                                .withString("answer", question.answer)
                                .navigation(this@QuestionComposeActivity)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionItem(
    question: QuestionData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Answer",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\QuestionDetailsComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.QuestionDetailsViewModel

class QuestionDetailsComposeActivity : BaseComposeActivity<QuestionDetailsViewModel>() {
    override fun createViewModel(): QuestionDetailsViewModel {
        return viewModels<QuestionDetailsViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: QuestionDetailsViewModel) {
        val question by viewModel.question.collectAsState()
        val answer by viewModel.answer.collectAsState()
        // Load question details from intent
        LaunchedEffect(Unit) {
            val questionText = intent.getStringExtra("question")
            val answerText = intent.getStringExtra("answer")
            viewModel.loadQuestionDetails(questionText, answerText)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "FAQ Details",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Question",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Answer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Answer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\StorageSpaceComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.StorageSpaceViewModel
import com.mpdc4gsr.libunified.app.compose.theme.Spacing
import com.mpdc4gsr.libunified.R as RCore

class StorageSpaceComposeActivity : BaseComposeActivity<StorageSpaceViewModel>() {
    override fun createViewModel(): StorageSpaceViewModel {
        return viewModels<StorageSpaceViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: StorageSpaceViewModel) {
        val storageInfo by viewModel.storageInfo.collectAsState()
        val usagePercentage = viewModel.getUsagePercentage()
        // Load storage info on start
        LaunchedEffect(Unit) {
            viewModel.loadStorageInfo()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.ts004_storage_space),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(Spacing.normal)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.normal)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Text(
                            text = stringResource(RCore.string.ts004_storage_space),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Storage Usage",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { usagePercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Spacing.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Used: ${viewModel.formatFileSize(storageInfo.usedSpace)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Total: ${viewModel.formatFileSize(storageInfo.totalSpace)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Text(
                            text = "Storage Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        StorageItem(
                            icon = Icons.Default.Add,
                            title = "Photos",
                            size = viewModel.formatFileSize(storageInfo.photoSpace),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        StorageItem(
                            icon = Icons.Default.Info,
                            title = "Videos",
                            size = viewModel.formatFileSize(storageInfo.videoSpace),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StorageItem(
                            icon = Icons.Default.Settings,
                            title = "System",
                            size = viewModel.formatFileSize(storageInfo.systemSpace),
                            color = MaterialTheme.colorScheme.outline
                        )
                        StorageItem(
                            icon = Icons.Default.Build,
                            title = "Free Space",
                            size = viewModel.formatFileSize(storageInfo.freeSpace),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Format Storage",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "This will delete all data and free up space",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = Spacing.extraSmall)
                                )
                            }
                            Button(
                                onClick = { viewModel.formatStorage() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.heightIn(min = Spacing.touchTarget)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(Spacing.large)
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text("Format")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageItem(
    icon: ImageVector,
    title: String,
    size: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Spacing.large)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\TISRComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.TISRViewModel
import com.mpdc4gsr.libunified.R as RCore

class TISRComposeActivity : BaseComposeActivity<TISRViewModel>() {
    override fun createViewModel(): TISRViewModel {
        return viewModels<TISRViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: TISRViewModel) {
        val isTISREnabled by viewModel.isTISREnabled.collectAsState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "TISR",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TISR Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(RCore.string.ts004_tisr_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        // TISR Switch Item
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(RCore.string.ts004_tisr_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isTISREnabled,
                                    onCheckedChange = { viewModel.updateTISRState(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Info Text
                        Text(
                            text = stringResource(RCore.string.ts004_tisr_tips),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\activity\UnitComposeActivity.kt =====

package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.user.viewmodel.UnitViewModel
import com.mpdc4gsr.libunified.R as RCore

class UnitComposeActivity : BaseComposeActivity<UnitViewModel>() {
    override fun createViewModel(): UnitViewModel {
        return viewModels<UnitViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: UnitViewModel) {
        val selectedUnit by viewModel.selectedUnit.collectAsState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.setting_unit),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                viewModel.saveTemperatureUnit()
                                finish()
                            }
                        ) {
                            Text(
                                text = stringResource(RCore.string.person_save),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celsius Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedUnit == UnitViewModel.CELSIUS,
                            onClick = { viewModel.selectUnit(UnitViewModel.CELSIUS) }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnit == UnitViewModel.CELSIUS) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "â„ƒ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedUnit == UnitViewModel.CELSIUS) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (selectedUnit == UnitViewModel.CELSIUS) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                // Fahrenheit Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedUnit == UnitViewModel.FAHRENHEIT,
                            onClick = { viewModel.selectUnit(UnitViewModel.FAHRENHEIT) }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "â„‰",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (selectedUnit == UnitViewModel.FAHRENHEIT) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\bean\ColorsBean.kt =====

package com.mpdc4gsr.module.user.bean

data class ColorsBean(
    var start: Int,
    var end: Int,
    var color: Int,
)


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\ble\BleDeviceManager.kt =====

package com.mpdc4gsr.module.user.ble

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topdon.ble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class BleDeviceManager(private val context: Context) : CoroutineScope {
    companion object {
        private const val TAG = "BleDeviceManager"
        private val GSR_DEVICE_NAMES =
            setOf(
                "Shimmer3 GSR+",
                "Shimmer",
                "GSR_Unit",
            )
    }

    enum class SystemBleStatus {
        NOT_SUPPORTED,
        ENABLED,
        DISABLED
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()
    private var easyBLE: EasyBLE? = null
    private val gsrSensorAddresses = mutableSetOf<String>()
    private val _discoveredDevices = MutableLiveData<List<BleDeviceInfo>>()
    val discoveredDevices: LiveData<List<BleDeviceInfo>> = _discoveredDevices
    private val _pairedDevices = MutableLiveData<List<BleDeviceInfo>>()
    val pairedDevices: LiveData<List<BleDeviceInfo>> = _pairedDevices
    private val _deviceStatus = MutableLiveData<Map<String, DeviceConnectionStatus>>()
    val deviceStatus: LiveData<Map<String, DeviceConnectionStatus>> = _deviceStatus
    private val deviceConnections = ConcurrentHashMap<String, Connection>()
    private val deviceInfoMap = ConcurrentHashMap<String, BleDeviceInfo>()

    data class BleDeviceInfo(
        val address: String,
        val name: String?,
        val rssi: Int,
        val isGsrSensor: Boolean,
        val isPaired: Boolean,
        val lastSeen: Long = System.currentTimeMillis(),
    )

    data class DeviceConnectionStatus(
        val address: String,
        val connectionState: ConnectionState,
        val reliabilityScore: Double,
        val dataIntegrity: Double,
        val isActive: Boolean,
    )

    fun initialize(enableNordicBackend: Boolean = true) {
        launch {
            Log.i(TAG, "Initializing BLE Device Manager with Nordic backend: $enableNordicBackend")
            easyBLE =
                EasyBLE.getBuilder()
                    .build().apply {
                        initialize(context.applicationContext as android.app.Application)
                    }
            setupDeviceDiscovery()
            Log.i(TAG, "BLE Device Manager initialized successfully")
        }
    }

    private fun setupDeviceDiscovery() {
        easyBLE?.addScanListener(
            object : com.topdon.ble.callback.ScanListener {
                override fun onScanStart() {
                    Log.d(TAG, "Enhanced BLE scan started")
                }

                override fun onScanStop() {
                    Log.d(TAG, "Enhanced BLE scan stopped")
                }

                override fun onScanResult(
                    device: Device,
                    isConnectedBySys: Boolean,
                ) {
                    val deviceInfo =
                        BleDeviceInfo(
                            address = device.address,
                            name = device.name,
                            rssi = device.rssi,
                            isGsrSensor = isGsrSensorDevice(device),
                            isPaired = isConnectedBySys,
                        )
                    deviceInfoMap[device.address] = deviceInfo
                    if (deviceInfo.isGsrSensor) {
                        gsrSensorAddresses.add(device.address)
                        Log.i(TAG, "GSR sensor detected: ${device.name} (${device.address})")
                    }
                    updateDiscoveredDevices()
                }

                override fun onScanError(
                    errorCode: Int,
                    errorMsg: String?,
                ) {
                    Log.e(
                        TAG,
                        "Enhanced BLE scan failed with error code: $errorCode, message: $errorMsg"
                    )
                }
            },
        )
    }

    private fun isGsrSensorDevice(device: Device): Boolean {
        val deviceName = device.name?.uppercase() ?: return false
        return GSR_DEVICE_NAMES.any { gsrName ->
            deviceName.contains(gsrName.uppercase())
        }
    }

    fun startDeviceDiscovery() {
        launch {
            Log.i(TAG, "Starting enhanced device discovery")
            easyBLE?.startScan()
        }
    }

    fun stopDeviceDiscovery() {
        launch {
            Log.i(TAG, "Stopping device discovery")
            easyBLE?.stopScan()
        }
    }

    fun connectToDevice(deviceAddress: String): Boolean {
        return try {
            Log.i(TAG, "Attempting enhanced connection to device: $deviceAddress")
            val deviceInfo = deviceInfoMap[deviceAddress]
            if (deviceInfo == null) {
                Log.e(TAG, "Device info not found for address: $deviceAddress")
                return false
            }
            val config = createOptimalConnectionConfig(deviceInfo.isGsrSensor)
            val observer = createDeviceObserver(deviceAddress)
            val connection = easyBLE?.connect(deviceAddress, config, observer)
            if (connection != null) {
                deviceConnections[deviceAddress] = connection
                updateDeviceStatus()
                Log.i(TAG, "Enhanced connection successful for device: $deviceAddress")
                true
            } else {
                Log.e(TAG, "Enhanced connection failed for device: $deviceAddress")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device $deviceAddress", e)
            false
        }
    }

    private fun createOptimalConnectionConfig(isGsrSensor: Boolean): ConnectionConfiguration {
        return ConnectionConfiguration().apply {
            if (isGsrSensor) {
                setConnectTimeoutMillis(10000)
                setAutoReconnect(true)
                Log.d(TAG, "Applied GSR-optimized connection configuration")
            } else {
                setConnectTimeoutMillis(5000)
                setAutoReconnect(false)
            }
        }
    }

    private fun createDeviceObserver(deviceAddress: String): EventObserver {
        return object : EventObserver {
            override fun onConnectionStateChanged(device: Device) {
                launch {
                    val connectionState = device.connectionState
                    Log.i(
                        TAG,
                        "Device connection state changed: $deviceAddress, state: $connectionState"
                    )
                    when (connectionState) {
                        ConnectionState.SERVICE_DISCOVERED -> {
                            val deviceInfo = deviceInfoMap[deviceAddress]?.copy(isPaired = true)
                            if (deviceInfo != null) {
                                deviceInfoMap[deviceAddress] = deviceInfo
                            }
                            updatePairedDevices()
                        }

                        ConnectionState.DISCONNECTED -> {
                            deviceConnections.remove(deviceAddress)
                            val deviceInfo = deviceInfoMap[deviceAddress]?.copy(isPaired = false)
                            if (deviceInfo != null) {
                                deviceInfoMap[deviceAddress] = deviceInfo
                            }
                            updatePairedDevices()
                        }

                        else -> {
                            Log.d(
                                TAG,
                                "Connection state: $connectionState for device: $deviceAddress"
                            )
                        }
                    }
                    updateDeviceStatus()
                }
            }

            override fun onConnectFailed(
                device: Device,
                failType: Int,
            ) {
                launch {
                    Log.w(TAG, "Device connection failed: $deviceAddress, error: $failType")
                    updateDeviceStatus()
                }
            }

            override fun onConnectTimeout(
                device: Device,
                type: Int,
            ) {
                launch {
                    Log.w(TAG, "Device connection timeout: $deviceAddress, type: $type")
                    updateDeviceStatus()
                }
            }

            override fun onCharacteristicChanged(
                device: Device,
                service: java.util.UUID,
                characteristic: java.util.UUID,
                value: ByteArray?,
            ) {
                Log.d(
                    TAG,
                    "Characteristic changed for $deviceAddress: service=$service, char=$characteristic"
                )
            }

            override fun onNotificationChanged(
                request: Request,
                isEnabled: Boolean,
            ) {
                Log.d(
                    TAG,
                    "Notification changed for $deviceAddress: ${request.type}, enabled: $isEnabled"
                )
            }

            override fun onCharacteristicRead(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Characteristic read for $deviceAddress: ${request.type}")
            }

            override fun onCharacteristicWrite(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Characteristic write for $deviceAddress: ${request.type}")
            }

            override fun onRequestFailed(
                request: Request,
                failType: Int,
                value: Any?,
            ) {
                Log.w(
                    TAG,
                    "Request failed for $deviceAddress: ${request.type}, fail type: $failType"
                )
            }

            override fun onMtuChanged(
                request: Request,
                mtu: Int,
            ) {
                Log.i(TAG, "MTU changed for $deviceAddress: $mtu")
            }

            override fun onRssiRead(
                request: Request,
                rssi: Int,
            ) {
                Log.d(TAG, "RSSI read for $deviceAddress: $rssi")
            }

            override fun onDescriptorRead(
                request: Request,
                value: ByteArray?,
            ) {
                Log.d(TAG, "Descriptor read for $deviceAddress: ${request.type}")
            }

            override fun onBluetoothAdapterStateChanged(state: Int) {
                Log.d(TAG, "Bluetooth adapter state changed: $state")
            }

            override fun onPhyChange(
                request: Request,
                txPhy: Int,
                rxPhy: Int,
            ) {
                Log.d(TAG, "PHY change for $deviceAddress: TX=$txPhy, RX=$rxPhy")
            }
        }
    }

    fun disconnectDevice(deviceAddress: String) {
        launch {
            Log.i(TAG, "Disconnecting device: $deviceAddress")
            deviceConnections[deviceAddress]?.disconnect()
        }
    }

    fun getSystemBleStatus(): Any? { // UnifiedBleManager.SystemBleStatus replaced
        return try {
            val adapter = easyBLE?.bluetoothAdapter
            when {
                adapter == null -> SystemBleStatus.NOT_SUPPORTED
                adapter.isEnabled -> SystemBleStatus.ENABLED
                else -> SystemBleStatus.DISABLED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system BLE status", e)
            null
        }
    }

    private fun updateDiscoveredDevices() {
        _discoveredDevices.postValue(deviceInfoMap.values.toList())
    }

    private fun updatePairedDevices() {
        val paired = deviceInfoMap.values.filter { it.isPaired }
        _pairedDevices.postValue(paired)
    }

    private fun updateDeviceStatus() {
        val statusMap =
            deviceConnections.mapValues { (address, connection) ->
                DeviceConnectionStatus(
                    address = address,
                    connectionState = connection.connectionState,
                    reliabilityScore = 1.0,
                    dataIntegrity = 1.0,
                    isActive = connection.connectionState == ConnectionState.SERVICE_DISCOVERED,
                )
            }
        _deviceStatus.postValue(statusMap)
    }

    fun getDiscoveredDeviceCount(): Int {
        return deviceInfoMap.size
    }

    fun getGsrDeviceCount(): Int {
        return gsrSensorAddresses.size
    }

    fun getPairedDeviceCount(): Int {
        return deviceInfoMap.values.count { it.isPaired }
    }

    fun isScanning(): Boolean {
        return easyBLE?.isScanning() ?: false
    }

    fun release() {
        launch {
            Log.i(TAG, "Releasing BLE Device Manager")
            stopDeviceDiscovery()
            deviceConnections.values.forEach { connection ->
                connection.disconnect()
            }
            deviceConnections.clear()
            deviceInfoMap.clear()
            easyBLE?.release()
            Log.i(TAG, "BLE Device Manager released successfully")
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\compose\DownloadProgressDialog.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DecimalFormat
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun DownloadProgressDialog(
    isVisible: Boolean,
    currentBytes: Long,
    totalBytes: Long,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(RCore.string.ts004_download_doing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    // File size info
                    val sizeText =
                        "${stringResource(RCore.string.detail_len)}: ${formatFileSize(currentBytes)}/${
                            formatFileSize(totalBytes)
                        }"
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    // Progress bar
                    val progress =
                        if (totalBytes > 0) (currentBytes.toFloat() / totalBytes.toFloat()) else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024) + "KB"
        size < 1024 * 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024) + "MB"
        else -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024 / 1024) + "GB"
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\compose\FirmwareInstallDialog.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FirmwareInstallDialog(
    isVisible: Boolean,
    message: String = "Installing firmware...",
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\compose\ListItemComponent.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListItemComponent(
    leftText: String,
    modifier: Modifier = Modifier,
    leftIconRes: Int? = null,
    leftIcon: ImageVector? = null,
    rightText: String? = null,
    showLine: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        val content: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left icon
                    when {
                        leftIcon != null -> {
                            Icon(
                                imageVector = leftIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        leftIconRes != null -> {
                            Icon(
                                painter = painterResource(id = leftIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                    // Left text
                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Right text
                rightText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                content()
            }
        }
        // Line separator
        if (showLine) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\compose\MineComposeFragment.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun MineComposeFragment(
    viewModel: MineFragmentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val showWinterPoint by viewModel.showWinterPoint.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // User Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                // Winter Easter Egg
                if (showWinterPoint) {
                    IconButton(
                        onClick = { viewModel.onWinterEggClick() }
                    ) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = "Winter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        // Settings Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_version),
                    leftIcon = Icons.Default.Info,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.VERSION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.set_clear_cache),
                    leftIcon = Icons.Default.Delete,
                    showLine = true,
                    onClick = { viewModel.clearCache() }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.electronic_manual),
                    leftIcon = Icons.Default.Book,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_BOOK)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.app_question),
                    leftIcon = Icons.AutoMirrored.Filled.Help,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_FAQ)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_feedback),
                    leftIcon = Icons.Default.Feedback,
                    showLine = true,
                    onClick = {
                        // Feedback navigation
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_unit),
                    leftIcon = Icons.Default.Settings,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.UNIT)
                            .navigation(context)
                    }
                )
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\compose\MoreComposeFragment.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun MoreComposeFragment(
    viewModel: MoreComposeFragmentViewModel,
    isTC007: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    // Initialize ViewModel with device type
    LaunchedEffect(isTC007) {
        viewModel.initialize(isTC007)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Save Setting Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Save Settings",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically save device configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = deviceSettings.isSaveSettingEnabled,
                        onCheckedChange = { viewModel.updateSaveSetting(it) }
                    )
                }
            }
        }
        // Settings Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ListItemComponent(
                    leftText = "Model Settings",
                    leftIcon = Icons.Default.Settings,
                    showLine = true,
                    onClick = {
                        // Model settings navigation
                    }
                )
                ListItemComponent(
                    leftText = "Correction Settings",
                    leftIcon = Icons.Default.Tune,
                    showLine = true,
                    onClick = {
                        // Correction settings navigation
                    }
                )
                ListItemComponent(
                    leftText = "Dual Mode",
                    leftIcon = Icons.Default.Apps,
                    showLine = true,
                    onClick = {
                        // Dual mode navigation
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_unit),
                    leftIcon = Icons.Default.Speed,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.UNIT)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_version),
                    leftIcon = Icons.Default.Info,
                    rightText = if (deviceSettings.hasUpgrade) "Update Available" else deviceSettings.versionText,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.VERSION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = "Device Information",
                    leftIcon = Icons.Default.Devices,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.DEVICE_INFORMATION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = "Factory Reset",
                    leftIcon = Icons.Default.Refresh,
                    onClick = { viewModel.performFactoryReset() }
                )
            }
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\model\FaqRepository.kt =====

package com.mpdc4gsr.module.user.model

import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R as RCore

object FaqRepository {
    fun getQuestionList(isTS001: Boolean): ArrayList<QuestionData> =
        if (isTS001) {
            arrayListOf(
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question1),
                    answer = ContextProvider.getContext().getString(RCore.string.answer1),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question2),
                    answer = ContextProvider.getContext().getString(RCore.string.answer2),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question3),
                    answer = ContextProvider.getContext().getString(RCore.string.answer3),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question4),
                    answer = ContextProvider.getContext().getString(RCore.string.answer4),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question5),
                    answer = ContextProvider.getContext().getString(RCore.string.answer5),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question6),
                    answer = ContextProvider.getContext().getString(RCore.string.answer6),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question7),
                    answer = ContextProvider.getContext().getString(RCore.string.answer7),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question8),
                    answer = ContextProvider.getContext().getString(RCore.string.answer8),
                ),
            )
        } else {
            arrayListOf(
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q1),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a1),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q2),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a2),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q3),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a3),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q4),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a4),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q5),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a5),
                ),
            )
        }
}

data class QuestionData(
    val question: String,
    val answer: String,
)


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\view\ListItemView.kt =====

package com.mpdc4gsr.module.user.view

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.module.user.R

class ListItemView : LinearLayout {
    private lateinit var mIvLeftIcon: ImageView
    private lateinit var mIvLeftContent: TextView
    private lateinit var mIvRightContent: TextView
    private lateinit var mLineView: View
    private var lineShow: Boolean = false
    private var leftIconRes: Int = 0
    private var leftContent: String = ""
    private var rightContent: String = ""

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ListItemView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                R.styleable.ListItemView_list_item_left_icon ->
                    leftIconRes =
                        ta.getResourceId(R.styleable.ListItemView_list_item_left_icon, 0)

                R.styleable.ListItemView_list_item_left_text ->
                    leftContent =
                        ta.getString(R.styleable.ListItemView_list_item_left_text).toString()

                R.styleable.ListItemView_list_item_right_text ->
                    rightContent =
                        ta.getString(R.styleable.ListItemView_list_item_right_text).toString()

                R.styleable.ListItemView_list_item_line ->
                    lineShow =
                        ta.getBoolean(R.styleable.ListItemView_list_item_line, false)
            }
        }
        ta.recycle()
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        inflate(context, R.layout.ui_list_item_view, this)
        mIvLeftIcon = findViewById(R.id.iv_left_icon)
        mIvLeftContent = findViewById(R.id.iv_left_content)
        mIvRightContent = findViewById(R.id.iv_right_content)
        mLineView = findViewById(R.id.view_line)
        mIvLeftIcon.setImageResource(leftIconRes)
        mIvLeftContent.text = leftContent
        mIvRightContent.text = rightContent
        mLineView.visibility = if (lineShow) View.VISIBLE else View.GONE
    }

    fun setLeftText(text: CharSequence?) {
        if (TextUtils.isEmpty(text)) return
        mIvLeftContent.text = text
        mIvLeftContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getLeftText(): String {
        return mIvLeftContent.text.toString()
    }

    fun setRightText(text: CharSequence?) {
        if (TextUtils.isEmpty(text)) return
        mIvRightContent.text = text
        mIvRightContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getRightText(): String {
        return mIvRightContent.text.toString()
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\AutoSaveViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoSaveViewModel : BaseViewModel() {
    private val _isAutoSaveEnabled = MutableStateFlow(false)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    init {
        loadAutoSaveState()
    }

    private fun loadAutoSaveState() {
        _isAutoSaveEnabled.value = SharedManager.is04AutoSync
    }

    fun updateAutoSaveState(enabled: Boolean) {
        launchWithErrorHandling {
            SharedManager.is04AutoSync = enabled
            _isAutoSaveEnabled.value = enabled
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\DeviceDetailsViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceDetailsViewModel : BaseViewModel() {
    companion object {
        private const val TC007_DEMO_SERIAL = "TC007-DEMO-SN"
        private const val TC007_MODEL = "TC007"
        private const val TS004_DEMO_SERIAL = "TS004-DEMO-SN"
        private const val TS004_MODEL = "TS004"
        private const val ERROR_SERIAL = "Error loading SN"
        private const val ERROR_MODEL = "Error loading model"
    }

    private val _serialNumber = MutableStateFlow("N/A")
    val serialNumber: StateFlow<String> = _serialNumber.asStateFlow()
    private val _deviceModel = MutableStateFlow("N/A")
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _copyMessage = MutableStateFlow("")
    val copyMessage: StateFlow<String> = _copyMessage.asStateFlow()
    fun loadDeviceDetails(isTC007: Boolean) {
        launchWithErrorHandling {
            _isLoading.value = true
            try {
                // Note: Original TS004Repository functionality was removed
                // Setting default values as per the original implementation
                if (isTC007) {
                    _serialNumber.value = TC007_DEMO_SERIAL
                    _deviceModel.value = TC007_MODEL
                } else {
                    _serialNumber.value = TS004_DEMO_SERIAL
                    _deviceModel.value = TS004_MODEL
                }
            } catch (e: Exception) {
                _serialNumber.value = ERROR_SERIAL
                _deviceModel.value = ERROR_MODEL
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCopyText(): String {
        return "SN:${_serialNumber.value}  Device Model:${_deviceModel.value}"
    }

    fun setCopyMessage(message: String) {
        _copyMessage.value = message
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\ElectronicManualViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ElectronicManualViewModel : BaseViewModel() {
    data class ManualOption(
        val name: String,
        val isTS001: Boolean
    )

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    private val _options = MutableStateFlow<List<ManualOption>>(emptyList())
    val options: StateFlow<List<ManualOption>> = _options.asStateFlow()
    private val _productType = MutableStateFlow(0)
    val productType: StateFlow<Int> = _productType.asStateFlow()
    fun loadManualOptions(productType: Int) {
        launchWithErrorHandling {
            _productType.value = productType
            val isFAQ = productType != Constants.SETTING_BOOK
            val optionsList = mutableListOf<ManualOption>()
            if (isFAQ) {
                optionsList.add(ManualOption("TS001", true))
            }
            optionsList.add(ManualOption("TS004", false))
            _options.value = optionsList
            _title.value = if (productType == Constants.SETTING_BOOK) {
                "Electronic Manual"
            } else {
                "Questions"
            }
        }
    }

    fun isBookMode(): Boolean = _productType.value == Constants.SETTING_BOOK
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MineFragmentViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineFragmentViewModel : BaseViewModel() {
    data class UserProfileState(
        val username: String = "Guest",
        val avatarUrl: String? = null,
        val isLoggedIn: Boolean = false
    )

    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()
    private val _showWinterPoint = MutableStateFlow(false)
    val showWinterPoint: StateFlow<Boolean> = _showWinterPoint.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        launchWithErrorHandling {
            val userInfoManager = UserInfoManager.getInstance()
            val isLoggedIn = userInfoManager.isLogin()
            _userProfile.value = UserProfileState(
                username = if (isLoggedIn) "User" else "Guest",
                avatarUrl = null,
                isLoggedIn = isLoggedIn
            )
        }
    }

    fun refreshUserProfile() {
        loadUserProfile()
    }

    fun clearCache() {
        launchWithErrorHandling {
            // Clear cache implementation
            // Original implementation used CleanUtils.cleanInternalCache()
        }
    }

    fun onWinterEggClick() {
        _showWinterPoint.value = !_showWinterPoint.value
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MineViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineViewModel : BaseViewModel() {
    data class UserInfo(
        val name: String = "User",
        val email: String = "user@example.com",
        val avatarUrl: String? = null,
        val isLoggedIn: Boolean = false
    )

    data class DeviceInfo(
        val hasLineConnection: Boolean = false,
        val hasTC007: Boolean = false,
        val hasTC007Connection: Boolean = false,
        val tc007Battery: Int? = null,
        val hasTS004: Boolean = false,
        val hasTS004Connection: Boolean = false
    )

    data class AppInfo(
        val version: String = "1.0.0",
        val buildNumber: String = "1000",
        val cacheSize: String = "0 MB",
        val lastUpdated: String = "Never"
    )

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()
    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()
    private val _appInfo = MutableStateFlow(AppInfo())
    val appInfo: StateFlow<AppInfo> = _appInfo.asStateFlow()

    init {
        loadUserInfo()
        loadDeviceInfo()
        loadAppInfo()
    }

    private fun loadUserInfo() {
        launchWithErrorHandling {
            val userInfoManager = UserInfoManager.getInstance()
            val isLoggedIn = userInfoManager.isLogin()
            _userInfo.value = UserInfo(
                name = if (isLoggedIn) "User" else "Guest",
                email = if (isLoggedIn) "user@example.com" else "guest@example.com",
                avatarUrl = null,
                isLoggedIn = isLoggedIn
            )
        }
    }

    private fun loadDeviceInfo() {
        launchWithErrorHandling {
            // Load device connection information
            _deviceInfo.value = DeviceInfo(
                hasLineConnection = false,
                hasTC007 = false,
                hasTC007Connection = false,
                tc007Battery = null,
                hasTS004 = false,
                hasTS004Connection = false
            )
        }
    }

    private fun loadAppInfo() {
        launchWithErrorHandling {
            // Load app information
            _appInfo.value = AppInfo(
                version = "1.10.000",
                buildNumber = "1100",
                cacheSize = "0 MB",
                lastUpdated = "Never"
            )
        }
    }

    fun editUserProfile() {
        launchWithErrorHandling {
            // Navigate to user profile editing
        }
    }

    fun changeAvatar() {
        launchWithErrorHandling {
            // Handle avatar change
        }
    }

    fun openDeviceSettings() {
        launchWithErrorHandling {
            // Navigate to device settings
        }
    }

    fun viewAppLogs() {
        launchWithErrorHandling {
            // Navigate to app logs
        }
    }

    fun clearAppCache() {
        launchWithErrorHandling {
            // Clear app cache
        }
    }

    fun checkForUpdates() {
        launchWithErrorHandling {
            // Check for app updates
        }
    }

    fun refreshData() {
        loadUserInfo()
        loadDeviceInfo()
        loadAppInfo()
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MoreFragmentComposeViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.common.WifiSaveSettingUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MoreComposeFragmentViewModel : BaseViewModel() {
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
        private const val DEFAULT_UPGRADE_AVAILABLE = false
    }

    data class DeviceSettingsState(
        val isTC007: Boolean = false,
        val isSaveSettingEnabled: Boolean = false,
        val hasUpgrade: Boolean = false,
        val versionText: String = ""
    )

    private val _deviceSettings = MutableStateFlow(DeviceSettingsState())
    val deviceSettings: StateFlow<DeviceSettingsState> = _deviceSettings.asStateFlow()
    fun initialize(isTC007: Boolean) {
        launchWithErrorHandling {
            val isSaveEnabled = if (isTC007) {
                WifiSaveSettingUtils.isSaveSetting
            } else {
                SaveSettingUtils.isSaveSetting
            }
            _deviceSettings.value = DeviceSettingsState(
                isTC007 = isTC007,
                isSaveSettingEnabled = isSaveEnabled,
                hasUpgrade = DEFAULT_UPGRADE_AVAILABLE,
                versionText = DEFAULT_VERSION
            )
        }
    }

    fun updateSaveSetting(enabled: Boolean) {
        launchWithErrorHandling {
            val currentState = _deviceSettings.value
            if (currentState.isTC007) {
                WifiSaveSettingUtils.isSaveSetting = enabled
            } else {
                SaveSettingUtils.isSaveSetting = enabled
            }
            _deviceSettings.value = currentState.copy(
                isSaveSettingEnabled = enabled
            )
        }
    }

    fun performFactoryReset() {
        launchWithErrorHandling {
            // Factory reset implementation
            // Original TS004Repository functionality was removed
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MoreViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MoreViewModel : BaseViewModel() {
    data class SettingsItem(
        val title: String,
        val subtitle: String,
        val action: SettingsAction
    )

    data class QuickAction(
        val title: String,
        val subtitle: String,
        val actionType: String
    )

    data class HelpResource(
        val title: String,
        val subtitle: String,
        val resourceType: String
    )

    data class CommunityLink(
        val title: String,
        val subtitle: String,
        val linkType: String
    )

    enum class SettingsAction {
        DEVICE_INFORMATION,
        TISR,
        STORAGE_SPACE,
        AUTO_SAVE,
        UNIT,
        VERSION,
        DISCONNECT,
        RESET
    }

    private val _settingsItems = MutableStateFlow<List<SettingsItem>>(emptyList())
    val settingsItems: StateFlow<List<SettingsItem>> = _settingsItems.asStateFlow()
    private val _quickActions = MutableStateFlow<List<QuickAction>>(emptyList())
    val quickActions: StateFlow<List<QuickAction>> = _quickActions.asStateFlow()
    private val _helpResources = MutableStateFlow<List<HelpResource>>(emptyList())
    val helpResources: StateFlow<List<HelpResource>> = _helpResources.asStateFlow()
    private val _communityLinks = MutableStateFlow<List<CommunityLink>>(emptyList())
    val communityLinks: StateFlow<List<CommunityLink>> = _communityLinks.asStateFlow()
    private val _isUpgradeAvailable = MutableStateFlow(false)
    val isUpgradeAvailable: StateFlow<Boolean> = _isUpgradeAvailable.asStateFlow()

    init {
        loadSettingsItems()
        loadQuickActions()
        loadHelpResources()
        loadCommunityLinks()
    }

    private fun loadSettingsItems() {
        launchWithErrorHandling {
            val items = listOf(
                SettingsItem(
                    title = "Device Information",
                    subtitle = "View device details and specifications",
                    action = SettingsAction.DEVICE_INFORMATION
                ),
                SettingsItem(
                    title = "TISR",
                    subtitle = "Temperature Image Super Resolution settings",
                    action = SettingsAction.TISR
                ),
                SettingsItem(
                    title = "Storage Space",
                    subtitle = "Manage device storage and format options",
                    action = SettingsAction.STORAGE_SPACE
                ),
                SettingsItem(
                    title = "Auto Save",
                    subtitle = "Automatically save images to device",
                    action = SettingsAction.AUTO_SAVE
                ),
                SettingsItem(
                    title = "Temperature Unit",
                    subtitle = "Choose Celsius or Fahrenheit",
                    action = SettingsAction.UNIT
                ),
                SettingsItem(
                    title = "Version Information",
                    subtitle = "App version and update information",
                    action = SettingsAction.VERSION
                ),
                SettingsItem(
                    title = "Disconnect Device",
                    subtitle = "Disconnect from thermal camera",
                    action = SettingsAction.DISCONNECT
                ),
                SettingsItem(
                    title = "Factory Reset",
                    subtitle = "Reset device to factory settings",
                    action = SettingsAction.RESET
                )
            )
            _settingsItems.value = items
        }
    }

    fun checkForUpdates() {
        launchWithErrorHandling {
            // Check for firmware/app updates
            // In a real implementation, this would call an update service or check a remote version API
            // For now, simulate the check and set to false (no updates available)
            kotlinx.coroutines.delay(500) // Simulate network delay
            _isUpgradeAvailable.value = false
        }
    }

    private fun loadQuickActions() {
        launchWithErrorHandling {
            val actions = listOf(
                QuickAction(
                    title = "Quick Calibration",
                    subtitle = "Start thermal camera calibration",
                    actionType = "startQuickCalibration"
                ),
                QuickAction(
                    title = "Export Data",
                    subtitle = "Export thermal images and logs",
                    actionType = "exportData"
                ),
                QuickAction(
                    title = "Share Analysis",
                    subtitle = "Share thermal analysis results",
                    actionType = "shareAnalysis"
                )
            )
            _quickActions.value = actions
        }
    }

    private fun loadHelpResources() {
        launchWithErrorHandling {
            val resources = listOf(
                HelpResource(
                    title = "User Guide",
                    subtitle = "Complete user manual and tutorials",
                    resourceType = "USER_GUIDE"
                ),
                HelpResource(
                    title = "FAQ",
                    subtitle = "Frequently asked questions",
                    resourceType = "FAQ"
                ),
                HelpResource(
                    title = "Troubleshooting",
                    subtitle = "Common issues and solutions",
                    resourceType = "TROUBLESHOOTING"
                )
            )
            _helpResources.value = resources
        }
    }

    private fun loadCommunityLinks() {
        launchWithErrorHandling {
            val links = listOf(
                CommunityLink(
                    title = "Advanced Analysis",
                    subtitle = "Access advanced thermal analysis tools",
                    linkType = "openAdvancedAnalysis"
                ),
                CommunityLink(
                    title = "Batch Processing",
                    subtitle = "Process multiple thermal images",
                    linkType = "openBatchProcessing"
                ),
                CommunityLink(
                    title = "AI Detection",
                    subtitle = "Use AI for thermal anomaly detection",
                    linkType = "openAIDetection"
                )
            )
            _communityLinks.value = links
        }
    }

    fun startQuickCalibration() {
        launchWithErrorHandling {
            // Start quick calibration process
        }
    }

    fun exportData() {
        launchWithErrorHandling {
            // Export thermal data
        }
    }

    fun shareAnalysis() {
        launchWithErrorHandling {
            // Share analysis results
        }
    }

    fun openAdvancedAnalysis() {
        launchWithErrorHandling {
            // Open advanced analysis tools
        }
    }

    fun openBatchProcessing() {
        launchWithErrorHandling {
            // Open batch processing interface
        }
    }

    fun openAIDetection() {
        launchWithErrorHandling {
            // Open AI detection interface
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\QuestionDetailsViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestionDetailsViewModel : BaseViewModel() {
    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()
    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()
    fun loadQuestionDetails(question: String?, answer: String?) {
        _question.value = question ?: ""
        _answer.value = answer ?: ""
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\QuestionViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.module.user.model.FaqRepository
import com.mpdc4gsr.module.user.model.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestionViewModel : BaseViewModel() {
    private val _questions = MutableStateFlow<List<QuestionData>>(emptyList())
    val questions: StateFlow<List<QuestionData>> = _questions.asStateFlow()
    fun loadQuestions(isTS001: Boolean) {
        launchWithErrorHandling {
            val questionList = FaqRepository.getQuestionList(isTS001)
            _questions.value = questionList
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\StorageSpaceViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat

class StorageSpaceViewModel : BaseViewModel() {
    companion object {
        private const val MOCK_TOTAL_SPACE = 32_000_000_000L
        private const val MOCK_USED_SPACE = 12_000_000_000L
        private const val MOCK_FREE_SPACE = 20_000_000_000L
        private const val MOCK_PHOTO_SPACE = 5_000_000_000L
        private const val MOCK_VIDEO_SPACE = 6_000_000_000L
        private const val MOCK_SYSTEM_SPACE = 1_000_000_000L
    }

    data class StorageInfo(
        val totalSpace: Long = 0L,
        val usedSpace: Long = 0L,
        val freeSpace: Long = 0L,
        val photoSpace: Long = 0L,
        val videoSpace: Long = 0L,
        val systemSpace: Long = 0L
    )

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()
    fun loadStorageInfo() {
        launchWithErrorHandling {
            // Original TS004Repository functionality removed - use mock data
            val mockStorageInfo = StorageInfo(
                totalSpace = MOCK_TOTAL_SPACE,
                usedSpace = MOCK_USED_SPACE,
                freeSpace = MOCK_FREE_SPACE,
                photoSpace = MOCK_PHOTO_SPACE,
                videoSpace = MOCK_VIDEO_SPACE,
                systemSpace = MOCK_SYSTEM_SPACE
            )
            _storageInfo.value = mockStorageInfo
        }
    }

    fun getUsagePercentage(): Float {
        val info = _storageInfo.value
        return if (info.totalSpace > 0) {
            (info.usedSpace.toFloat() / info.totalSpace.toFloat())
        } else {
            0f
        }
    }

    fun formatFileSize(fileSize: Long): String {
        return when {
            fileSize == 0L -> "0 B"
            fileSize < 1024 -> DecimalFormat("#.0").format(fileSize.toDouble()) + " B"
            fileSize < 1048576 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1024) + " KB"
            fileSize < 1073741824 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1048576) + " MB"
            else -> DecimalFormat("#.0").format(fileSize.toDouble() / 1073741824) + " GB"
        }
    }

    fun formatStorage() {
        launchWithErrorHandling {
            // Original format storage operation removed - just show confirmation
            // In real implementation, this would format the storage
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\TISRViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TISRViewModel : BaseViewModel() {
    private val _isTISREnabled = MutableStateFlow(false)
    val isTISREnabled: StateFlow<Boolean> = _isTISREnabled.asStateFlow()

    init {
        loadTISRState()
    }

    private fun loadTISRState() {
        launchWithErrorHandling {
            // Original TS004Repository functionality was removed - use SharedManager default
            _isTISREnabled.value = SharedManager.is04TISR
        }
    }

    fun updateTISRState(enabled: Boolean) {
        launchWithErrorHandling {
            SharedManager.is04TISR = enabled
            _isTISREnabled.value = enabled
            // Note: Original socket communication removed as per original activity changes
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\UnitViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnitViewModel : BaseViewModel() {
    companion object {
        const val CELSIUS = 1
        const val FAHRENHEIT = 0
    }

    private val _selectedUnit = MutableStateFlow(CELSIUS)
    val selectedUnit: StateFlow<Int> = _selectedUnit.asStateFlow()

    init {
        loadTemperatureUnit()
    }

    private fun loadTemperatureUnit() {
        _selectedUnit.value = SharedManager.getTemperature()
    }

    fun selectUnit(unit: Int) {
        launchWithErrorHandling {
            _selectedUnit.value = unit
            // Don't save immediately, wait for user to confirm with save button
        }
    }

    fun saveTemperatureUnit() {
        launchWithErrorHandling {
            SharedManager.setTemperature(_selectedUnit.value)
        }
    }

    fun isCelsiusSelected(): Boolean = _selectedUnit.value == CELSIUS
    fun isFahrenheitSelected(): Boolean = _selectedUnit.value == FAHRENHEIT
}


