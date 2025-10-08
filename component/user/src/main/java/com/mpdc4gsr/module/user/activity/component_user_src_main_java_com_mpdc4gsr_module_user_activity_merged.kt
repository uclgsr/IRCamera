// Merged ALL .kt and .java files from the 'component\user\src\main\java\com\mpdc4gsr\module\user\activity' directory and its subdirectories.
// Total files: 9 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\AutoSaveComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\DeviceDetailsComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\ElectronicManualComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\MoreComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\QuestionComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\QuestionDetailsComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\StorageSpaceComposeActivity.kt =====

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.Spacing
import com.mpdc4gsr.module.user.viewmodel.StorageSpaceViewModel
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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\TISRComposeActivity.kt =====

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


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\activity\UnitComposeActivity.kt =====

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