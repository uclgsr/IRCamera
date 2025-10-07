// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\settings\ui' subtree
// Files: 18; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\AboutScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.Spacing
import mpdc4gsr.core.ui.components.NavigationBreadcrumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("About") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        NavigationBreadcrumb(
            currentScreen = "About",
            previousScreen = "Settings"
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.normal, Alignment.CenterVertically)
        ) {
            Text(
                text = "IRCamera",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.paddingFromBaseline(top = Spacing.extraLarge)
            )
            Text(
                text = "Version 1.10.000",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
            Text(
                text = "Thermal imaging and GSR sensor data collection application",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
            Text(
                text = "Modernized with Jetpack Compose for enhanced user experience and maintainability.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\AppInfoScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun AppInfoScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "App Information",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon and Name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "IR Camera",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-Modal Sensor Platform",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            // Version Information
            SettingsCard(
                title = "Version Information",
                icon = Icons.Default.Info
            ) {
                SettingsRow(
                    label = "Version",
                    value = "1.0.0"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Build Number",
                    value = "2024.01.001"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Release Date",
                    value = "January 2024"
                )
            }
            // Developer Information
            SettingsCard(
                title = "Developer",
                icon = Icons.Default.Code
            ) {
                SettingsRow(
                    label = "Organization",
                    value = "UCL GSR"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Project",
                    value = "Multi-Modal Data Collection"
                )
            }
            // Legal
            SettingsCard(
                title = "Legal",
                icon = Icons.Default.Gavel
            ) {
                Text(
                    text = "Â© 2024 UCL GSR. All rights reserved.\n\nThis application is for research purposes only.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppInfoScreenPreview() {
    IRCameraTheme {
        AppInfoScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\ClauseComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import java.util.*

class ClauseViewModel : AppBaseViewModel() {
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    private val _currentYear = mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: State<Int> = _currentYear
    private val _agreementAccepted = mutableStateOf(false)
    val agreementAccepted: State<Boolean> = _agreementAccepted
    fun setAgreementAccepted(accepted: Boolean) {
        _agreementAccepted.value = accepted
    }

    suspend fun confirmInitApp(context: android.content.Context): Boolean {
        return try {
            _isLoading.value = true
            // Simulate initialization process
            delay(2000)
            // Initialize app components
            if (BaseApplication.instance.isDomestic()) {
                // SharedManager.setAppName(context.getString(R.string.app_name))
                // SharedManager.setVersionName(UnifiedVersionUtils.getVersion())
                // Set network status
                val networkStatus =
                    if (NetworkUtils.isNetworkAvailable()) "Connected" else "Disconnected"
                // SharedManager.setNetworkStatus(networkStatus)
            }
            _isLoading.value = false
            true
        } catch (e: Exception) {
            _isLoading.value = false
            false
        }
    }
}

class ClauseComposeActivity : BaseComposeActivity<ClauseViewModel>() {
    private val clauseVM: ClauseViewModel by viewModels()
    override fun createViewModel(): ClauseViewModel = clauseVM

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ClauseViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val isLoading by viewModel.isLoading
            val currentYear by viewModel.currentYear
            val agreementAccepted by viewModel.agreementAccepted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = stringResource(R.string.terms_and_conditions),
                    onBackClick = { finish() }
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // App logo and welcome section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Multi-Modal Data Collection Platform",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // Terms and conditions content
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Terms and Conditions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = buildTermsContent(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                                )
                            }
                        }
                        // Agreement checkbox
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = agreementAccepted,
                                    onCheckedChange = { viewModel.setAgreementAccepted(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "I have read and agree to the Terms and Conditions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        // Action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { finish() },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Disagree & Exit")
                            }
                            Button(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        val success = viewModel.confirmInitApp(context)
                                        if (success) {
                                            NavigationManager.build(RouterConfig.MAIN)
                                                .navigation(context as ClauseComposeActivity)
                                            finish()
                                        }
                                    }
                                },
                                enabled = agreementAccepted && !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Agree & Continue")
                                }
                            }
                        }
                        // Copyright section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Copyright Â© 2023-$currentYear MPDC4GSR Technology Co., Ltd.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "All Rights Reserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    // Loading overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Initializing application...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildTermsContent(): String {
        return """
            Welcome to the IRCamera application. By using this application, you agree to comply with and be bound by the following terms and conditions.
            1. ACCEPTANCE OF TERMS
            By accessing and using this application, you accept and agree to be bound by the terms and provision of this agreement.
            2. DATA COLLECTION AND PRIVACY
            This application collects thermal imaging data, GSR sensor data, and other biometric information for research purposes. All data collection is performed with your explicit consent and in accordance with applicable privacy laws.
            3. PERMITTED USE
            â€¢ Use the application only for legitimate research and educational purposes
            â€¢ Obtain appropriate consent from research participants
            â€¢ Comply with all applicable laws and regulations
            â€¢ Properly secure and protect collected data
            4. PROHIBITED ACTIVITIES
            â€¢ Using the application for unlawful purposes
            â€¢ Attempting to reverse engineer or modify the application
            â€¢ Sharing collected data without proper authorization
            â€¢ Violating privacy rights of individuals
            5. DATA SECURITY
            We implement appropriate security measures to protect your information, including encryption and access controls. However, no system is completely secure, and you acknowledge the inherent risks.
            6. INTELLECTUAL PROPERTY
            The application and its content are protected by copyright, trademark, and other intellectual property laws. All rights not expressly granted are reserved.
            7. LIMITATION OF LIABILITY
            The application is provided "as is" without warranties. We shall not be liable for any damages arising from your use of the application.
            8. MODIFICATIONS
            We reserve the right to modify these terms at any time. Continued use of the application constitutes acceptance of modified terms.
            By clicking "Agree & Continue", you acknowledge that you have read, understood, and agree to these terms and conditions.
        """.trimIndent()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\HelpScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun HelpScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Help & Support",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Start Guide
            SettingsCard(
                title = "Quick Start Guide",
                icon = Icons.AutoMirrored.Filled.MenuBook
            ) {
                Text(
                    text = "1. Connect your sensors (GSR, Thermal Camera)\n" +
                            "2. Configure sensor settings\n" +
                            "3. Start recording\n" +
                            "4. Export your data",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            // FAQ
            SettingsCard(
                title = "Frequently Asked Questions",
                icon = Icons.Default.QuestionAnswer
            ) {
                FAQItem(
                    question = "How do I connect the GSR sensor?",
                    answer = "Enable Bluetooth and scan for devices in Network Settings."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FAQItem(
                    question = "How do I calibrate the thermal camera?",
                    answer = "Go to Settings > Calibration and follow the on-screen instructions."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FAQItem(
                    question = "Where is my data stored?",
                    answer = "Data is stored locally on your device in the configured storage location."
                )
            }
            // Support Contact
            SettingsCard(
                title = "Technical Support",
                icon = Icons.Default.Support
            ) {
                SettingsRow(
                    label = "Email",
                    value = "support@uclgsr.ac.uk"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Documentation",
                    value = "docs.uclgsr.ac.uk"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Open email client with support email pre-filled
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:support@uclgsr.ac.uk")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "IRCamera App Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Email client not available
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support")
                }
            }
            // Troubleshooting
            SettingsCard(
                title = "Troubleshooting",
                icon = Icons.Default.Build
            ) {
                Text(
                    text = "If you encounter issues:\n\n" +
                            "â€¢ Check device connections\n" +
                            "â€¢ Restart the application\n" +
                            "â€¢ Run diagnostics\n" +
                            "â€¢ Check system logs",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun FAQItem(
    question: String,
    answer: String
) {
    Column {
        Text(
            text = question,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    IRCameraTheme {
        HelpScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\MoreHelpComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.dialogs.TipDialogState
import com.mpdc4gsr.libunified.app.utils.Constants
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

data class HelpStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isActionable: Boolean = false,
    val actionText: String = "",
    val action: (() -> Unit)? = null
)

class MoreHelpViewModel : AppBaseViewModel() {
    private val _connectionType = mutableStateOf(0)
    val connectionType: State<Int> = _connectionType
    private val _helpSteps = mutableStateOf<List<HelpStep>>(emptyList())
    val helpSteps: State<List<HelpStep>> = _helpSteps
    fun setConnectionType(type: Int) {
        _connectionType.value = type
        updateHelpSteps(type)
    }

    private fun updateHelpSteps(type: Int) {
        _helpSteps.value = if (type == Constants.SETTING_CONNECTION) {
            getConnectionHelpSteps()
        } else {
            getDisconnectionHelpSteps()
        }
    }

    private fun getConnectionHelpSteps(): List<HelpStep> {
        return listOf(
            HelpStep(
                icon = Icons.Default.Power,
                title = "Power On Device",
                description = "Ensure your thermal camera device is powered on and in pairing mode."
            ),
            HelpStep(
                icon = Icons.Default.Wifi,
                title = "Enable WiFi",
                description = "Make sure WiFi is enabled on your mobile device for wireless connection.",
                isActionable = true,
                actionText = "Open WiFi Settings"
            ),
            HelpStep(
                icon = Icons.Default.Search,
                title = "Search for Device",
                description = "Use the device discovery feature to locate your thermal camera on the network."
            ),
            HelpStep(
                icon = Icons.Default.Link,
                title = "Establish Connection",
                description = "Select your device from the list and follow the pairing instructions."
            ),
            HelpStep(
                icon = Icons.Default.CheckCircle,
                title = "Verify Connection",
                description = "Once connected, you should see the device status as 'Connected' in the main screen."
            )
        )
    }

    private fun getDisconnectionHelpSteps(): List<HelpStep> {
        return listOf(
            HelpStep(
                icon = Icons.Default.Warning,
                title = "Connection Lost",
                description = "If your device disconnected unexpectedly, try the following steps to reconnect."
            ),
            HelpStep(
                icon = Icons.Default.Refresh,
                title = "Restart Device",
                description = "Power off your thermal camera device and turn it back on after 10 seconds."
            ),
            HelpStep(
                icon = Icons.Default.Wifi,
                title = "Check Network",
                description = "Ensure both devices are connected to the same WiFi network.",
                isActionable = true,
                actionText = "Open Network Settings"
            ),
            HelpStep(
                icon = Icons.Default.Bluetooth,
                title = "Reset Bluetooth",
                description = "If using Bluetooth connection, toggle Bluetooth off and on again.",
                isActionable = true,
                actionText = "Open Bluetooth Settings"
            ),
            HelpStep(
                icon = Icons.Default.RestartAlt,
                title = "Restart Application",
                description = "Close and reopen the IRCamera application to refresh all connections."
            )
        )
    }
}

class MoreHelpComposeActivity : BaseComposeActivity<MoreHelpViewModel>() {
    private lateinit var wifiManager: WifiManager
    override fun createViewModel(): MoreHelpViewModel = viewModels<MoreHelpViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionType = intent.getIntExtra(Constants.SETTING_CONNECTION_TYPE, 0)
        viewModels<MoreHelpViewModel>().value.setConnectionType(connectionType)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MoreHelpViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val connectionType by viewModel.connectionType
            val helpSteps by viewModel.helpSteps
            val title = if (connectionType == Constants.SETTING_CONNECTION) {
                stringResource(R.string.connection_help)
            } else {
                stringResource(R.string.disconnection_troubleshooting)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = title,
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (connectionType == Constants.SETTING_CONNECTION)
                                    Icons.AutoMirrored.Filled.Help else Icons.Default.BugReport,
                                contentDescription = if (connectionType == Constants.SETTING_CONNECTION)
                                    "Connection Help" else "Troubleshooting",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (connectionType == Constants.SETTING_CONNECTION)
                                    "Device Connection Guide" else "Troubleshooting Guide",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (connectionType == Constants.SETTING_CONNECTION)
                                    "Follow these steps to connect your thermal camera device"
                                else
                                    "Steps to resolve connection issues",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    // Help steps
                    helpSteps.forEachIndexed { index, step ->
                        HelpStepCard(
                            step = step,
                            stepNumber = index + 1,
                            onActionClick = { action ->
                                when {
                                    step.actionText.contains("WiFi") -> openWifiSettings()
                                    step.actionText.contains("Network") -> openNetworkSettings()
                                    step.actionText.contains("Bluetooth") -> openBluetoothSettings()
                                    else -> action?.invoke()
                                }
                            }
                        )
                        if (index < helpSteps.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Additional help section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ContactSupport,
                                    contentDescription = "Contact Support",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Need More Help?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "If you're still experiencing issues, check the device manual or contact technical support for additional assistance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            showSettingsError("WiFi settings")
        }
    }

    private fun openNetworkSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showSettingsError("Network settings")
        }
    }

    private fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            showSettingsError("Bluetooth settings")
        }
    }

    private fun showSettingsError(settingType: String) {
        val tipDialogState = TipDialogState(this)
        tipDialogState.show(
            title = "",
            message = "Unable to open $settingType. Please access it manually from your device settings.",
            showCancel = false,
            positiveText = getString(R.string.app_got_it),
            onPositive = { }
        )
    }
}

@Composable
private fun HelpStepCard(
    step: HelpStep,
    stepNumber: Int,
    onActionClick: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number and icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Step content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (step.isActionable && step.action != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onActionClick(step.action) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(step.actionText)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\PdfComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class PdfType(val fileName: String, val displayName: String) {
    TC001("TC001.pdf", "TC001 Thermal Camera Manual"),
    TS004("TS004.pdf", "TS004 Thermal Camera Manual")
}

data class PdfDocument(
    val type: PdfType,
    val file: File?,
    val isAvailable: Boolean,
    val fileSize: String = ""
)

class PdfViewModel : AppBaseViewModel() {
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    private val _pdfDocument = mutableStateOf<PdfDocument?>(null)
    val pdfDocument: State<PdfDocument?> = _pdfDocument
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    fun loadPdf(isTS001: Boolean, context: android.content.Context) {
        val pdfType = if (isTS001) PdfType.TC001 else PdfType.TS004
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + exceptionHandler) {
            try {
                _isLoading.value = true
                _error.value = null
                // Simulate loading delay
                delay(1000)
                val externalDir = context.getExternalFilesDir("pdf")
                if (externalDir == null) {
                    _error.value = "External storage not available"
                    _isLoading.value = false
                    return@launch
                }
                val pdfDir = externalDir
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }
                val pdfFile = File(pdfDir, pdfType.fileName)
                val isAvailable = pdfFile.exists()
                if (!isAvailable) {
                    // Copy from assets if available
                    copyPdfFromAssets(context, pdfType.fileName, pdfFile)
                }
                val fileSize = if (pdfFile.exists()) {
                    formatFileSize(pdfFile.length())
                } else {
                    "0 KB"
                }
                val document = PdfDocument(
                    type = pdfType,
                    file = if (pdfFile.exists()) pdfFile else null,
                    isAvailable = pdfFile.exists(),
                    fileSize = fileSize
                )
                _pdfDocument.value = document
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load PDF: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun copyPdfFromAssets(
        context: android.content.Context,
        fileName: String,
        destinationFile: File
    ) {
        try {
            val inputStream = context.assets.open("manuals/$fileName")
            val outputStream = FileOutputStream(destinationFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            // PDF file doesn't exist in assets, which is expected for now
            // We'll show a placeholder message
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

class PdfComposeActivity : BaseComposeActivity<PdfViewModel>() {
    override fun createViewModel(): PdfViewModel = viewModels<PdfViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTS001 = intent.getBooleanExtra("isTS001", false)
        viewModels<PdfViewModel>().value.loadPdf(isTS001, this)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: PdfViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val isLoading by viewModel.isLoading
            val pdfDocument by viewModel.pdfDocument
            val error by viewModel.error
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = pdfDocument?.type?.displayName ?: "Manual Viewer",
                    onBackClick = { finish() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when {
                        isLoading -> {
                            LoadingContent()
                        }

                        error != null -> {
                            ErrorContent(
                                error = error!!,
                                onRetry = {
                                    val isTS001 = intent.getBooleanExtra("isTS001", false)
                                    viewModel.loadPdf(isTS001, context)
                                }
                            )
                        }

                        pdfDocument != null -> {
                            PdfContent(document = pdfDocument!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading manual...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error Loading Manual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun PdfContent(document: PdfDocument) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Document info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Document",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = document.type.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (document.isAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File size: ${document.fileSize}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (document.isAvailable) {
            // PDF available - show viewer placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "PDF Viewer",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PDF Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PDF functionality will be displayed here when PDF library is integrated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        } else {
            // PDF not available
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Download Manual",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Manual Not Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${document.type.fileName} manual is not currently available. Please check for updates or contact support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Quick help section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Quick Tips",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "â€¢ Make sure your device is fully charged before extended use\nâ€¢ Keep the device at room temperature for optimal performance\nâ€¢ Regular calibration ensures accurate measurements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\PolicyComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.csl.irCamera.R
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

class PolicyViewModel : AppBaseViewModel() {
    enum class PolicyType(val title: String, val contentRes: Int) {
        USER_AGREEMENT("User Services Agreement", R.string.user_services_agreement),
        PRIVACY_POLICY("Privacy Policy", R.string.privacy_policy),
        THIRD_PARTY("Third Party Components", R.string.third_party_components)
    }

    private val _policyType = mutableStateOf(PolicyType.USER_AGREEMENT)
    val policyType: State<PolicyType> = _policyType
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    private val _showError = mutableStateOf(false)
    val showError: State<Boolean> = _showError
    private val _htmlContent = mutableStateOf("")
    val htmlContent: State<String> = _htmlContent
    fun setPolicyType(type: Int) {
        _policyType.value = when (type) {
            1 -> PolicyType.USER_AGREEMENT
            2 -> PolicyType.PRIVACY_POLICY
            3 -> PolicyType.THIRD_PARTY
            else -> PolicyType.USER_AGREEMENT
        }
    }

    fun updateLoadingState(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(error: Boolean) {
        _showError.value = error
    }

    suspend fun loadContent(context: android.content.Context) {
        try {
            _isLoading.value = true
            _showError.value = false
            // Simulate loading delay
            delay(500)
            val content = when (_policyType.value) {
                PolicyType.USER_AGREEMENT -> generateUserAgreementContent()
                PolicyType.PRIVACY_POLICY -> generatePrivacyPolicyContent()
                PolicyType.THIRD_PARTY -> generateThirdPartyContent()
            }
            _htmlContent.value = content
            _isLoading.value = false
        } catch (e: Exception) {
            _isLoading.value = false
            _showError.value = true
        }
    }

    private fun generateUserAgreementContent(): String = """
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #ffffff;
                }
                h1 { color: #FF6B35; font-size: 28px; margin-bottom: 20px; }
                h2 { color: #4ECDC4; font-size: 22px; margin-top: 30px; margin-bottom: 15px; }
                h3 { color: #45B7D1; font-size: 18px; margin-top: 25px; margin-bottom: 10px; }
                .section { margin-bottom: 25px; }
                .highlight { background-color: #FFF3CD; padding: 10px; border-left: 4px solid #FF6B35; margin: 15px 0; }
                ul { padding-left: 20px; }
                li { margin-bottom: 8px; }
            </style>
        </head>
        <body>
            <h1>User Services Agreement</h1>
            
            <div class="highlight">
                <strong>Effective Date:</strong> This agreement is effective as of the date you first use the IRCamera application.
            </div>
            <div class="section">
                <h2>1. Acceptance of Terms</h2>
                <p>By downloading, installing, or using the IRCamera application ("App"), you agree to be bound by this User Services Agreement ("Agreement"). If you do not agree to these terms, please do not use the App.</p>
            </div>
            <div class="section">
                <h2>2. Description of Service</h2>
                <p>IRCamera is a multi-modal data collection platform that enables:</p>
                <ul>
                    <li>Thermal imaging data capture and analysis</li>
                    <li>Galvanic Skin Response (GSR) sensor data collection</li>
                    <li>RGB camera integration for research purposes</li>
                    <li>Data synchronization and export capabilities</li>
                    <li>Research template management</li>
                </ul>
            </div>
            <div class="section">
                <h2>3. Data Collection and Privacy</h2>
                <p>The App collects various types of data for research purposes:</p>
                <ul>
                    <li><strong>Thermal Data:</strong> Temperature measurements and thermal images</li>
                    <li><strong>Physiological Data:</strong> GSR measurements and related sensor data</li>
                    <li><strong>Camera Data:</strong> RGB images and video recordings</li>
                    <li><strong>Device Information:</strong> Hardware specifications and performance metrics</li>
                </ul>
                <p>All data collection is performed with your explicit consent and in accordance with our Privacy Policy.</p>
            </div>
            <div class="section">
                <h2>4. User Responsibilities</h2>
                <p>As a user of the App, you agree to:</p>
                <ul>
                    <li>Use the App only for legitimate research and educational purposes</li>
                    <li>Obtain appropriate consent from participants in research studies</li>
                    <li>Comply with applicable laws and regulations regarding data collection</li>
                    <li>Properly secure and protect collected data</li>
                    <li>Report any bugs or security issues to the development team</li>
                </ul>
            </div>
            <div class="section">
                <h2>5. Intellectual Property</h2>
                <p>The App and its original content, features, and functionality are owned by the development team and are protected by international copyright, trademark, patent, trade secret, and other intellectual property laws.</p>
            </div>
            <div class="section">
                <h2>6. Limitation of Liability</h2>
                <p>The App is provided "as is" without any warranties. We shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising out of your use of the App.</p>
            </div>
            <div class="section">
                <h2>7. Contact Information</h2>
                <p>If you have any questions about this Agreement, please contact us at the support channels provided in the App.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun generatePrivacyPolicyContent(): String = """
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #ffffff;
                }
                h1 { color: #FF6B35; font-size: 28px; margin-bottom: 20px; }
                h2 { color: #4ECDC4; font-size: 22px; margin-top: 30px; margin-bottom: 15px; }
                h3 { color: #45B7D1; font-size: 18px; margin-top: 25px; margin-bottom: 10px; }
                .section { margin-bottom: 25px; }
                .highlight { background-color: #E3F2FD; padding: 10px; border-left: 4px solid #45B7D1; margin: 15px 0; }
                ul { padding-left: 20px; }
                li { margin-bottom: 8px; }
                table { width: 100%; border-collapse: collapse; margin: 15px 0; }
                th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
                th { background-color: #f2f2f2; }
            </style>
        </head>
        <body>
            <h1>Privacy Policy</h1>
            
            <div class="highlight">
                <strong>Last Updated:</strong> This privacy policy was last updated on the date of the latest app release.
            </div>
            <div class="section">
                <h2>1. Information We Collect</h2>
                <p>IRCamera collects the following types of information:</p>
                
                <table>
                    <tr>
                        <th>Data Type</th>
                        <th>Purpose</th>
                        <th>Storage</th>
                    </tr>
                    <tr>
                        <td>Thermal Images</td>
                        <td>Research data collection</td>
                        <td>Local device storage</td>
                    </tr>
                    <tr>
                        <td>GSR Sensor Data</td>
                        <td>Physiological monitoring</td>
                        <td>Local device storage</td>
                    </tr>
                    <tr>
                        <td>RGB Camera Data</td>
                        <td>Visual data correlation</td>
                        <td>Local device storage</td>
                    </tr>
                    <tr>
                        <td>Device Information</td>
                        <td>App functionality and debugging</td>
                        <td>Local device storage</td>
                    </tr>
                </table>
            </div>
            <div class="section">
                <h2>2. How We Use Your Information</h2>
                <p>We use the collected information to:</p>
                <ul>
                    <li>Provide core app functionality for data collection</li>
                    <li>Enable research data analysis and export</li>
                    <li>Improve app performance and user experience</li>
                    <li>Provide technical support and troubleshooting</li>
                    <li>Ensure compliance with research protocols</li>
                </ul>
            </div>
            <div class="section">
                <h2>3. Data Sharing and Disclosure</h2>
                <p>We do not sell, trade, or otherwise transfer your personal information to third parties. Data may be shared only in the following circumstances:</p>
                <ul>
                    <li>With your explicit consent for research purposes</li>
                    <li>When required by law or legal process</li>
                    <li>To protect our rights, property, or safety</li>
                    <li>With authorized research collaborators under data use agreements</li>
                </ul>
            </div>
            <div class="section">
                <h2>4. Data Security</h2>
                <p>We implement appropriate security measures to protect your information:</p>
                <ul>
                    <li>Encryption of sensitive data at rest and in transit</li>
                    <li>Access controls and authentication mechanisms</li>
                    <li>Regular security audits and updates</li>
                    <li>Secure data transmission protocols</li>
                    <li>Device-level security requirements</li>
                </ul>
            </div>
            <div class="section">
                <h2>5. Your Rights</h2>
                <p>You have the right to:</p>
                <ul>
                    <li>Access your personal data stored by the app</li>
                    <li>Request correction of inaccurate data</li>
                    <li>Request deletion of your data</li>
                    <li>Withdraw consent for data processing</li>
                    <li>Export your data in a machine-readable format</li>
                </ul>
            </div>
            <div class="section">
                <h2>6. Data Retention</h2>
                <p>We retain your data only as long as necessary for the purposes outlined in this policy or as required by law. Research data may be retained for extended periods to support longitudinal studies.</p>
            </div>
            <div class="section">
                <h2>7. Contact Us</h2>
                <p>If you have questions about this Privacy Policy or wish to exercise your rights, please contact us through the app's support channels.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun generateThirdPartyContent(): String = """
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #ffffff;
                }
                h1 { color: #FF6B35; font-size: 28px; margin-bottom: 20px; }
                h2 { color: #4ECDC4; font-size: 22px; margin-top: 30px; margin-bottom: 15px; }
                .component { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 15px; margin: 15px 0; }
                .component-name { font-weight: bold; color: #FF6B35; font-size: 18px; }
                .component-version { color: #6c757d; font-size: 14px; }
                .component-license { color: #28a745; font-weight: 500; }
                .component-description { margin-top: 8px; }
            </style>
        </head>
        <body>
            <h1>Third Party Components</h1>
            
            <p>IRCamera uses the following third-party libraries and components. We are grateful to the open source community for these excellent tools.</p>
            <div class="component">
                <div class="component-name">Jetpack Compose</div>
                <div class="component-version">Version: 2025.01.01</div>
                <div class="component-license">License: Apache License 2.0</div>
                <div class="component-description">Modern UI toolkit for Android development.</div>
            </div>
            <div class="component">
                <div class="component-name">Kotlin Coroutines</div>
                <div class="component-version">Version: 1.9.0</div>
                <div class="component-license">License: Apache License 2.0</div>
                <div class="component-description">Asynchronous programming support for Kotlin.</div>
            </div>
            <div class="component">
                <div class="component-name">OkHttp</div>
                <div class="component-version">Version: 4.12.0</div>
                <div class="component-license">License: Apache License 2.0</div>
                <div class="component-description">HTTP client for efficient network operations.</div>
            </div>
            <div class="component">
                <div class="component-name">Glide</div>
                <div class="component-version">Version: 5.0.5</div>
                <div class="component-license">License: BSD, MIT</div>
                <div class="component-description">Image loading and caching library for Android.</div>
            </div>
            <div class="component">
                <div class="component-name">RxJava</div>
                <div class="component-version">Version: 2.2.21</div>
                <div class="component-license">License: Apache License 2.0</div>
                <div class="component-description">Reactive programming library for Java and Android.</div>
            </div>
            <div class="component">
                <div class="component-name">EventBus</div>
                <div class="component-version">Version: 3.x</div>
                <div class="component-license">License: Apache License 2.0</div>
                <div class="component-description">Event bus for Android and Java.</div>
            </div>
            <div class="component">
                <div class="component-name">Shimmer Android API</div>
                <div class="component-version">Version: 1.0.0</div>
                <div class="component-license">License: BSD</div>
                <div class="component-description">API for Shimmer sensing devices integration.</div>
            </div>
            <div class="component">
                <div class="component-name">Nordic BLE Library</div>
                <div class="component-version">Version: 2.11.0</div>
                <div class="component-license">License: BSD</div>
                <div class="component-description">Bluetooth Low Energy library for Android.</div>
            </div>
            <div class="component">
                <div class="component-name">TOPDON Thermal SDK</div>
                <div class="component-version">Version: TC001</div>
                <div class="component-license">License: Proprietary</div>
                <div class="component-description">SDK for TOPDON thermal camera integration.</div>
            </div>
            <h2>License Information</h2>
            <p>Most of the third-party components used in this application are licensed under the Apache License 2.0, which allows for commercial use, modification, distribution, and patent use, while requiring preservation of copyright and license notices.</p>
            
            <p>For the full text of the Apache License 2.0, please visit: <a href="https://www.apache.org/licenses/LICENSE-2.0">https://www.apache.org/licenses/LICENSE-2.0</a></p>
            <p>If you have any questions about the third-party components used in this application, please contact us through the app's support channels.</p>
        </body>
        </html>
    """.trimIndent()
}

class PolicyComposeActivity : BaseComposeActivity<PolicyViewModel>() {
    companion object {
        const val KEY_THEME_TYPE = "key_theme_type"
        const val KEY_USE_TYPE = "key_use_type"
    }

    override fun createViewModel(): PolicyViewModel = viewModels<PolicyViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeType = intent.getIntExtra(KEY_THEME_TYPE, 1)
        val viewModel = viewModels<PolicyViewModel>().value
        viewModel.setPolicyType(themeType)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: PolicyViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val policyType by viewModel.policyType
            val isLoading by viewModel.isLoading
            val showError by viewModel.showError
            val htmlContent by viewModel.htmlContent
            LaunchedEffect(policyType) {
                viewModel.loadContent(context)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = policyType.title,
                    onBackClick = { finish() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (htmlContent.isNotEmpty() && !isLoading && !showError) {
                        PolicyWebView(
                            htmlContent = htmlContent,
                            onError = { viewModel.setError(true) }
                        )
                    }
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading ${policyType.title}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (showError) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.Center),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Failed to load content",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                            .launch {
                                                viewModel.loadContent(context)
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PolicyWebView(
    htmlContent: String,
    onError: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            BridgeWebView(context).apply {
                val webSettings: WebSettings = settings
                webSettings.javaScriptEnabled = true
                webSettings.allowFileAccess = true
                webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
                webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webSettings.useWideViewPort = true
                webSettings.setSupportZoom(true)
                webSettings.builtInZoomControls = true
                webSettings.displayZoomControls = false
                webViewClient = object : BridgeWebViewClient(this) {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            onError()
                        }
                    }
                }
                isScrollContainer = true
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { webView ->
        webView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\PolicyScreen.kt =====

package mpdc4gsr.feature.settings.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun PolicyScreen(
    policyType: PolicyType = PolicyType.PRIVACY,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val title = when (policyType) {
        PolicyType.PRIVACY -> "Privacy Policy"
        PolicyType.TERMS -> "Terms of Service"
        PolicyType.ABOUT -> "About"
    }
    val url = when (policyType) {
        PolicyType.PRIVACY -> "file:///android_asset/privacy_policy.html"
        PolicyType.TERMS -> "file:///android_asset/terms_of_service.html"
        PolicyType.ABOUT -> "file:///android_asset/about.html"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = title,
            showBackButton = true,
            onBackClick = onBackClick
        )
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

enum class PolicyType {
    PRIVACY,
    TERMS,
    ABOUT
}

@Preview(showBackground = true)
@Composable
private fun PolicyScreenPreview() {
    IRCameraTheme {
        PolicyScreen(PolicyType.PRIVACY)
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\PrivacyPolicyScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun PrivacyPolicyScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Privacy Policy",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Last Updated: January 2024",
                color = Color.Gray,
                fontSize = 14.sp
            )
            PolicySection(
                title = "Data Collection",
                content = "This application collects multi-modal sensor data including GSR (Galvanic Skin Response), thermal imaging, and RGB camera data for research purposes. All data is collected with explicit user consent."
            )
            PolicySection(
                title = "Data Storage",
                content = "Collected data is stored locally on the device and can be exported by the user. No data is transmitted to external servers without explicit user action."
            )
            PolicySection(
                title = "Data Usage",
                content = "Data collected through this application is intended for research purposes only. Users maintain full control over their data and can delete it at any time."
            )
            PolicySection(
                title = "Third-Party Access",
                content = "No third-party services have access to your data. Data export and sharing are entirely controlled by the user."
            )
            PolicySection(
                title = "Data Security",
                content = "We implement appropriate technical measures to protect your data from unauthorized access, alteration, or destruction."
            )
            PolicySection(
                title = "User Rights",
                content = "You have the right to access, modify, or delete your data at any time. You can also request a copy of all data collected."
            )
            PolicySection(
                title = "Contact",
                content = "For questions or concerns about this privacy policy, please contact the research team at UCL GSR."
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicyScreenPreview() {
    IRCameraTheme {
        PrivacyPolicyScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\ProfileEditScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.ProfileData
import mpdc4gsr.feature.settings.presentation.ProfileEditViewModel

@Composable
fun ProfileEditScreen(
    onBackClick: (() -> Unit)? = null,
    onSave: ((ProfileData) -> Unit)? = null,
    viewModel: ProfileEditViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val profileData by viewModel.profileData.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Edit Profile",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = Color(0xFF16131e)
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Picture Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Photo picker */ }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Photo")
                        }
                    }
                }
                // Basic Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Basic Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = profileData.userName,
                            onValueChange = viewModel::updateUserName,
                            label = { Text("Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = profileData.userId,
                            onValueChange = viewModel::updateUserId,
                            label = { Text("User ID") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                        OutlinedTextField(
                            value = profileData.email,
                            onValueChange = viewModel::updateEmail,
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // Research Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Research Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = profileData.institution,
                            onValueChange = viewModel::updateInstitution,
                            label = { Text("Institution") },
                            leadingIcon = {
                                Icon(Icons.Default.School, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = profileData.researchArea,
                            onValueChange = viewModel::updateResearchArea,
                            label = { Text("Research Area") },
                            leadingIcon = {
                                Icon(Icons.Default.Science, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = profileData.bio,
                            onValueChange = viewModel::updateBio,
                            label = { Text("Bio") },
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
                // Privacy Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Privacy Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Profile Visible to Researchers")
                            Switch(
                                checked = profileData.isProfileVisible,
                                onCheckedChange = viewModel::updateProfileVisibility
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Share Anonymized Data")
                            Switch(
                                checked = profileData.allowDataSharing,
                                onCheckedChange = viewModel::updateDataSharing
                            )
                        }
                    }
                }
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onBackClick?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.saveProfile {
                                onSave?.invoke(profileData)
                                showSaveDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            }
        }
        // Save Confirmation Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Profile Updated") },
                text = { Text("Your profile has been successfully updated.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onBackClick?.invoke()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    IRCameraTheme {
        ProfileEditScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\ProfileScreen.kt =====

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
            val context = androidx.compose.ui.platform.LocalContext.current
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


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\RecordingSettingsScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.data.CameraConfigurationManager
import mpdc4gsr.feature.settings.presentation.RecordingSettingsViewModel

@Composable
fun RecordingSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: RecordingSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.recordingSettings.collectAsState()
    val configManager = remember { CameraConfigurationManager() }
    val (_, _, supports60fps) = remember {
        configManager.detectDeviceCapabilities()
    }
    val maxFrameRate = if (supports60fps) 60f else 30f
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Recording Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Preferences Card
            SettingsCard(
                title = "Recording Preferences",
                icon = Icons.Default.VideoCall
            ) {
                SettingsToggle(
                    label = "Auto Recording",
                    description = "Start recording automatically when all devices are connected",
                    checked = settings.autoRecording,
                    onCheckedChange = { viewModel.updateAutoRecording(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Recording Quality",
                    value = settings.recordingQuality,
                    options = listOf("Low", "Medium", "High", "Ultra"),
                    onValueChange = { viewModel.updateRecordingQuality(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Video Frame Rate",
                    value = settings.videoFrameRate.toFloat(),
                    valueRange = 15f..maxFrameRate,
                    onValueChange = { viewModel.updateVideoFrameRate(it.toInt()) },
                    unit = " fps"
                )
            }
            // Multi-Modal Recording Card
            SettingsCard(
                title = "Multi-Modal Recording",
                icon = Icons.Default.Sync
            ) {
                SettingsToggle(
                    label = "Audio Recording",
                    description = "Record audio along with video and sensor data",
                    checked = settings.audioEnabled,
                    onCheckedChange = { viewModel.updateAudioEnabled(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Simultaneous Recording",
                    description = "Record all sensors at the same time",
                    checked = settings.simultaneousRecording,
                    onCheckedChange = { viewModel.updateSimultaneousRecording(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Timestamp Synchronization",
                    description = "Synchronize timestamps across all recordings",
                    checked = settings.timestampSync,
                    onCheckedChange = { viewModel.updateTimestampSync(it) }
                )
            }
            // Recording Format Card
            SettingsCard(
                title = "Recording Format",
                icon = Icons.Default.VideoLibrary
            ) {
                SettingsRow(
                    label = "Video Format",
                    value = settings.videoFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Audio Format",
                    value = settings.audioFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Sensor Data Format",
                    value = settings.sensorDataFormat
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordingSettingsScreenPreview() {
    IRCameraTheme {
        RecordingSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\SettingsComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.settings.*

class SettingsComposeActivity : BaseComposeActivity<SettingsViewModel>() {
    override fun createViewModel(): SettingsViewModel {
        return viewModels<SettingsViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SettingsViewModel) {
        // Settings state
        var thermalCameraEnabled by remember { mutableStateOf(true) }
        var gsrSensorEnabled by remember { mutableStateOf(true) }
        var autoRecording by remember { mutableStateOf(false) }
        var recordingQuality by remember { mutableStateOf("High") }
        var frameRate by remember { mutableFloatStateOf(10f) }
        var sampleRate by remember { mutableStateOf("51.2Hz") }
        var darkMode by remember { mutableStateOf(false) }
        var exportFormat by remember { mutableStateOf("CSV") }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
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
                // Device Configuration Section
                SettingsSection(title = "Device Configuration") {
                    SwitchSettingsItem(
                        title = "Thermal Camera",
                        subtitle = "Enable TOPDON TC001 thermal camera",
                        checked = thermalCameraEnabled,
                        onCheckedChange = { thermalCameraEnabled = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchSettingsItem(
                        title = "GSR Sensor",
                        subtitle = "Enable Shimmer3 GSR sensor via BLE",
                        checked = gsrSensorEnabled,
                        onCheckedChange = { gsrSensorEnabled = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Device Calibration",
                        subtitle = "Calibrate thermal camera and sensors",
                        onClick = {
                            // TODO: Open calibration screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening calibration screen...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                // Recording Preferences Section
                SettingsSection(title = "Recording Preferences") {
                    SwitchSettingsItem(
                        title = "Auto Recording",
                        subtitle = "Start recording automatically when devices connect",
                        checked = autoRecording,
                        onCheckedChange = { autoRecording = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RadioButtonSettingsItem(
                        title = "Recording Quality",
                        options = listOf("Low", "Medium", "High", "Ultra"),
                        selectedOption = recordingQuality,
                        onOptionSelected = { recordingQuality = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderSettingsItem(
                        title = "Thermal Camera Frame Rate",
                        subtitle = "Adjust thermal camera capture rate",
                        value = frameRate,
                        onValueChange = { frameRate = it },
                        valueRange = 1f..30f,
                        valueLabel = { "${it.toInt()} Hz" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RadioButtonSettingsItem(
                        title = "GSR Sample Rate",
                        options = listOf("25.6Hz", "51.2Hz", "128Hz", "256Hz"),
                        selectedOption = sampleRate,
                        onOptionSelected = { sampleRate = it }
                    )
                }
                // Display Options Section
                SettingsSection(title = "Display Options") {
                    SwitchSettingsItem(
                        title = "Dark Mode",
                        subtitle = "Use dark theme optimized for thermal imaging",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Thermal Color Palette",
                        subtitle = "Choose thermal imaging color scheme",
                        onClick = {
                            // TODO: Open color palette selection dialog
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Color palette selection coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Temperature Units",
                        subtitle = "Celsius, Fahrenheit, or Kelvin",
                        onClick = {
                            // TODO: Open temperature unit selection dialog
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Temperature unit selection coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Display Resolution",
                        subtitle = "Adjust thermal image display resolution",
                        onClick = {
                            // TODO: Open resolution settings dialog
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Resolution settings coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                // Data Export Section
                SettingsSection(title = "Data Export") {
                    RadioButtonSettingsItem(
                        title = "Export Format",
                        options = listOf("CSV", "JSON", "Excel", "HDF5"),
                        selectedOption = exportFormat,
                        onOptionSelected = { exportFormat = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Export Location",
                        subtitle = "Choose where to save exported data",
                        onClick = {
                            // TODO: Open export location selection dialog
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Export location selection coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionSettingsItem(
                        title = "Export All Data",
                        subtitle = "Export all recorded sensor data",
                        actionText = "Export",
                        onAction = {
                            // TODO: Export all sensor data
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Exporting all data...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                // Network Settings Section
                SettingsSection(title = "Network Settings") {
                    SettingsItem(
                        title = "PC Controller Connection",
                        subtitle = "Configure connection to PC controller",
                        onClick = {
                            // TODO: Open network configuration screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening network configuration...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Network Discovery",
                        subtitle = "Enable automatic PC discovery",
                        onClick = {
                            // TODO: Toggle network discovery
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Network discovery toggle coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionSettingsItem(
                        title = "Test Connection",
                        subtitle = "Test connection to PC controller",
                        actionText = "Test",
                        onAction = {
                            // TODO: Test network connection
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Testing connection...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                // Advanced Settings Section
                SettingsSection(title = "Advanced Settings") {
                    SettingsItem(
                        title = "Developer Options",
                        subtitle = "Advanced configuration options",
                        onClick = {
                            // TODO: Open developer options screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening developer options...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Logging Settings",
                        subtitle = "Configure application logging",
                        onClick = {
                            // TODO: Open logging settings screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening logging settings...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionSettingsItem(
                        title = "Reset All Settings",
                        subtitle = "Reset all settings to default values",
                        actionText = "Reset",
                        onAction = {
                            // TODO: Show confirmation dialog and reset settings
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Reset all settings confirmation dialog",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        isDestructive = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionSettingsItem(
                        title = "Clear All Data",
                        subtitle = "Delete all recorded sensor data",
                        actionText = "Clear",
                        onAction = {
                            // TODO: Show confirmation dialog and clear data
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Clear all data confirmation dialog",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        isDestructive = true
                    )
                }
                // About Section
                SettingsSection(title = "About") {
                    SettingsItem(
                        title = "App Version",
                        subtitle = "IRCamera v1.10.000",
                        onClick = {
                            // TODO: Show version details dialog
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Version details coming soon",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "View privacy policy and terms",
                        onClick = {
                            // TODO: Open privacy policy screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening privacy policy...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Help & Support",
                        subtitle = "Get help and contact support",
                        onClick = {
                            // TODO: Open help and support screen
                            android.widget.Toast.makeText(
                                this@SettingsComposeActivity,
                                "Opening help & support...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

class SettingsViewModel : AppBaseViewModel() {
    // Settings-specific state management
    private val _settingsState = mutableStateOf(SettingsState())
    val settingsState: State<SettingsState> = _settingsState

    data class SettingsState(
        val darkModeEnabled: Boolean = false,
        val networkDiscoveryEnabled: Boolean = true,
        val autoSaveEnabled: Boolean = true
    )
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\SettingsScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.NavigationBreadcrumb
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    onNavigateToGSRSettings: (() -> Unit)? = null,
    onNavigateToThermalSettings: (() -> Unit)? = null,
    onNavigateToCameraSettings: (() -> Unit)? = null,
    onNavigateToRecordingSettings: (() -> Unit)? = null,
    onNavigateToStorageSettings: (() -> Unit)? = null,
    onNavigateToSyncSettings: (() -> Unit)? = null,
    onNavigateToCalibration: (() -> Unit)? = null,
    onNavigateToNetworkSettings: (() -> Unit)? = null,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    onNavigateToAppInfo: (() -> Unit)? = null,
    onNavigateToPrivacyPolicy: (() -> Unit)? = null,
    onNavigateToHelp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        NavigationBreadcrumb(
            currentScreen = "Settings",
            previousScreen = "Home"
        )
        // Settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sensor Settings Section
            SettingsSection(
                title = "Sensor Configuration"
            ) {
                SettingsItem(
                    icon = Icons.Default.Sensors,
                    title = "GSR Sensor Settings",
                    subtitle = "Configure Shimmer3 device and sampling rate",
                    onClick = { onNavigateToGSRSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Thermostat,
                    title = "Thermal Camera Settings",
                    subtitle = "Temperature calibration and palette options",
                    onClick = { onNavigateToThermalSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Camera,
                    title = "RGB Camera Settings",
                    subtitle = "Resolution, frame rate, and quality settings",
                    onClick = { onNavigateToCameraSettings?.invoke() }
                )
            }
            // Recording Settings Section
            SettingsSection(
                title = "Recording & Data"
            ) {
                SettingsItem(
                    icon = Icons.Default.VideoCall,
                    title = "Recording Settings",
                    subtitle = "Multi-modal recording preferences",
                    onClick = { onNavigateToRecordingSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Data Storage",
                    subtitle = "Export location and file formats",
                    onClick = { onNavigateToStorageSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Synchronization",
                    subtitle = "Time sync and data alignment settings",
                    onClick = { onNavigateToSyncSettings?.invoke() }
                )
            }
            // Application Settings Section
            SettingsSection(
                title = "Application"
            ) {
                var darkMode by remember { mutableStateOf(true) }
                var notifications by remember { mutableStateOf(true) }
                var autoConnect by remember { mutableStateOf(false) }
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme interface",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Enable system notifications",
                    checked = notifications,
                    onCheckedChange = { notifications = it }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Auto Connect",
                    subtitle = "Automatically connect to known devices",
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
            }
            // Advanced Settings Section
            SettingsSection(
                title = "Advanced"
            ) {
                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = "Calibration",
                    subtitle = "System calibration and alignment tools",
                    onClick = { onNavigateToCalibration?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.NetworkCheck,
                    title = "Network Settings",
                    subtitle = "Device pairing and network configuration",
                    onClick = { onNavigateToNetworkSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Diagnostics",
                    subtitle = "System diagnostics and troubleshooting",
                    onClick = { onNavigateToDiagnostics?.invoke() }
                )
            }
            // About Section
            SettingsSection(
                title = "About"
            ) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Information",
                    subtitle = "Version 1.0.0 - Multi-Modal Sensor Platform",
                    onClick = { onNavigateToAppInfo?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "Data privacy and usage policy",
                    onClick = { onNavigateToPrivacyPolicy?.invoke() }
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help & Support",
                    subtitle = "User guide and technical support",
                    onClick = { onNavigateToHelp?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    IRCameraTheme {
        SettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\StorageSettingsScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.StorageSettingsViewModel
import mpdc4gsr.feature.settings.presentation.StorageSettingsViewModelFactory

@Composable
fun StorageSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: StorageSettingsViewModel = viewModel(
        factory = StorageSettingsViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.storageSettings.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Storage Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Location Card
            SettingsCard(
                title = "Storage Location",
                icon = Icons.Default.Storage
            ) {
                SettingsDropdown(
                    label = "Default Storage",
                    value = settings.storageLocation,
                    options = listOf("Internal Storage", "SD Card", "External USB"),
                    onValueChange = { viewModel.updateStorageLocation(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Available Space",
                    value = storageInfo.availableSpace
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Used Space",
                    value = storageInfo.usedSpace
                )
            }
            // Export Settings Card
            SettingsCard(
                title = "Export Settings",
                icon = Icons.Default.FileUpload
            ) {
                SettingsToggle(
                    label = "Auto Export",
                    description = "Automatically export data after recording",
                    checked = settings.autoExport,
                    onCheckedChange = { viewModel.updateAutoExport(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Export Format",
                    value = settings.exportFormat,
                    options = listOf("CSV", "JSON", "XML", "MATLAB"),
                    onValueChange = { viewModel.updateExportFormat(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Compression",
                    description = "Compress exported files to save space",
                    checked = settings.compressionEnabled,
                    onCheckedChange = { viewModel.updateCompression(it) }
                )
            }
            // Backup Settings Card
            SettingsCard(
                title = "Backup Settings",
                icon = Icons.Default.Backup
            ) {
                SettingsToggle(
                    label = "Auto Backup",
                    description = "Automatically backup data to cloud storage",
                    checked = settings.autoBackup,
                    onCheckedChange = { viewModel.updateAutoBackup(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Delete After Export",
                    description = "Delete local data after successful export",
                    checked = settings.deleteAfterExport,
                    onCheckedChange = { viewModel.updateDeleteAfterExport(it) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageSettingsScreenPreview() {
    IRCameraTheme {
        StorageSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\SyncSettingsScreen.kt =====

package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.SyncSettingsViewModel

@Composable
fun SyncSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: SyncSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.syncSettings.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Synchronization Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Synchronization Card
            SettingsCard(
                title = "Time Synchronization",
                icon = Icons.Default.Schedule
            ) {
                SettingsToggle(
                    label = "NTP Synchronization",
                    description = "Sync time with network time protocol server",
                    checked = settings.ntpSync,
                    onCheckedChange = { viewModel.updateNtpSync(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Sync Method",
                    value = settings.syncMethod,
                    options = listOf("NTP", "GPS", "Manual", "Device Clock"),
                    onValueChange = { viewModel.updateSyncMethod(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Sync Interval",
                    value = settings.syncInterval.toFloat(),
                    valueRange = 10f..300f,
                    onValueChange = { viewModel.updateSyncInterval(it.toInt()) },
                    unit = " sec"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Sync",
                    value = settings.lastSync
                )
            }
            // Data Alignment Card
            SettingsCard(
                title = "Data Alignment",
                icon = Icons.Default.AlignHorizontalCenter
            ) {
                SettingsToggle(
                    label = "Auto Alignment",
                    description = "Automatically align data from multiple sensors",
                    checked = settings.autoAlignment,
                    onCheckedChange = { viewModel.updateAutoAlignment(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Timestamp Correction",
                    description = "Apply correction to align timestamps",
                    checked = settings.timestampCorrection,
                    onCheckedChange = { viewModel.updateTimestampCorrection(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Time Offset",
                    value = "+0.023 ms"
                )
            }
            // Sensor Synchronization Card
            SettingsCard(
                title = "Sensor Synchronization",
                icon = Icons.Default.Sync
            ) {
                SettingsRow(
                    label = "GSR Sensor",
                    value = "Synced"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Thermal Camera",
                    value = "Synced"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "RGB Camera",
                    value = "Synced"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncSettingsScreenPreview() {
    IRCameraTheme {
        SyncSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\VersionComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import java.util.*

class VersionViewModel : AppBaseViewModel() {
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
    }

    data class VersionInfo(
        val appVersion: String,
        val buildCode: String,
        val buildTime: String,
        val unifiedVersion: String,
        val thermalVersion: String,
        val gsrVersion: String
    )

    private val _versionInfo = mutableStateOf(
        VersionInfo(
            appVersion = BuildConfig.VERSION_NAME,
            buildCode = BuildConfig.VERSION_CODE.toString(),
            buildTime = BuildConfig.BUILD_TYPE,
            unifiedVersion = DEFAULT_VERSION,
            thermalVersion = DEFAULT_VERSION,
            gsrVersion = DEFAULT_VERSION
        )
    )
    val versionInfo: State<VersionInfo> = _versionInfo
    fun updateVersionInfo(context: android.content.Context) {
        _versionInfo.value = _versionInfo.value.copy(
            unifiedVersion = UnifiedVersionUtils.getVersionName(context)
        )
    }
}

class VersionComposeActivity : BaseComposeActivity<VersionViewModel>() {
    override fun createViewModel(): VersionViewModel = viewModels<VersionViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModels<VersionViewModel>().value.updateVersionInfo(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: VersionViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val versionInfo by viewModel.versionInfo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = stringResource(R.string.version_info),
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo and Name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Thermal & GSR Data Collection Platform",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Version Information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Version Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "App Version",
                                value = versionInfo.appVersion,
                                icon = Icons.Default.Apps
                            )
                            VersionInfoRow(
                                label = "Build Code",
                                value = versionInfo.buildCode,
                                icon = Icons.Default.Build
                            )
                            VersionInfoRow(
                                label = "Build Type",
                                value = versionInfo.buildTime,
                                icon = Icons.Default.Engineering
                            )
                            VersionInfoRow(
                                label = "Unified Module",
                                value = versionInfo.unifiedVersion,
                                icon = Icons.Default.IntegrationInstructions
                            )
                        }
                    }
                    // Module Versions
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Module Versions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "Thermal Camera",
                                value = versionInfo.thermalVersion,
                                icon = Icons.Default.Thermostat
                            )
                            VersionInfoRow(
                                label = "GSR Sensor",
                                value = versionInfo.gsrVersion,
                                icon = Icons.Default.Sensors
                            )
                        }
                    }
                    // System Information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "System Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "Android Version",
                                value = android.os.Build.VERSION.RELEASE,
                                icon = Icons.Default.Android
                            )
                            VersionInfoRow(
                                label = "API Level",
                                value = android.os.Build.VERSION.SDK_INT.toString(),
                                icon = Icons.Default.Api
                            )
                            VersionInfoRow(
                                label = "Device Model",
                                value = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                                icon = Icons.Default.PhoneAndroid
                            )
                        }
                    }
                    // Copyright and Legal
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Copyright Â© ${Calendar.getInstance().get(Calendar.YEAR)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "MPDC4GSR - Multi-Modal Data Collection Platform",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Navigate to privacy policy
                                        val intent =
                                            Intent(context, PolicyComposeActivity::class.java).apply {
                                                putExtra(PolicyComposeActivity.KEY_THEME_TYPE, 2)
                                            }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Privacy Policy")
                                }
                                OutlinedButton(
                                    onClick = {
                                        // Navigate to terms
                                        val intent =
                                            Intent(context, PolicyComposeActivity::class.java).apply {
                                                putExtra(PolicyComposeActivity.KEY_THEME_TYPE, 1)
                                            }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Terms")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\ui\WebViewComposeActivity.kt =====

package mpdc4gsr.feature.settings.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.csl.irCamera.R
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

class WebViewViewModel : AppBaseViewModel() {
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    private val _showError = mutableStateOf(false)
    val showError: State<Boolean> = _showError
    private val _url = mutableStateOf("")
    val url: State<String> = _url
    fun setUrl(url: String) {
        _url.value = url
    }

    fun setWebViewLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(error: Boolean) {
        _showError.value = error
    }

    fun reload() {
        _showError.value = false
        _isLoading.value = true
    }
}

class WebViewComposeActivity : BaseComposeActivity<WebViewViewModel>() {
    override fun createViewModel(): WebViewViewModel = viewModels<WebViewViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.extras?.getString(ExtraKeyConfig.URL) ?: ""
        viewModels<WebViewViewModel>().value.setUrl(url)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: WebViewViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val url by viewModel.url
            val isLoading by viewModel.isLoading
            val showError by viewModel.showError
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = stringResource(R.string.web_content),
                    onBackClick = { finish() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (url.isNotEmpty()) {
                        ComposeWebView(
                            url = url,
                            onLoadStart = { viewModel.setWebViewLoading(true) },
                            onLoadFinish = {
                                viewModel.setWebViewLoading(false)
                                viewModel.setError(false)
                            },
                            onError = {
                                viewModel.setWebViewLoading(false)
                                viewModel.setError(true)
                            },
                            onReload = { viewModel.reload() }
                        )
                    }
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showError) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.Center),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.network_error),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.reload() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWebView(
    url: String,
    onLoadStart: () -> Unit = {},
    onLoadFinish: () -> Unit = {},
    onError: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    var webView by remember { mutableStateOf<BridgeWebView?>(null) }
    AndroidView(
        factory = { context ->
            BridgeWebView(context).apply {
                val webSettings: WebSettings = settings
                webSettings.setSupportZoom(false)
                webSettings.useWideViewPort = true
                webSettings.javaScriptCanOpenWindowsAutomatically = true
                webSettings.defaultTextEncodingName = "UTF-8"
                webSettings.javaScriptEnabled = true
                webSettings.allowFileAccess = true
                webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
                webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = object : BridgeWebViewClient(this) {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadFinish()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            onError()
                        }
                    }
                }
                registerHandler("goBack") { _, function ->
                    function.onCallBack("android")
                }
                isScrollContainer = false
                webView = this
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        if (view.url != url) {
            onLoadStart()
            view.loadUrl(url)
        }
    }
    LaunchedEffect(onReload) {
        webView?.let { wv ->
            if (url.isNotEmpty()) {
                onLoadStart()
                wv.loadUrl(url)
            }
        }
    }
}


