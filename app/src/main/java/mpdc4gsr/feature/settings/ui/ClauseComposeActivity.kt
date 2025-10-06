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
                                    text = "Copyright © 2023-$currentYear MPDC4GSR Technology Co., Ltd.",
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
            • Use the application only for legitimate research and educational purposes
            • Obtain appropriate consent from research participants
            • Comply with all applicable laws and regulations
            • Properly secure and protect collected data
            4. PROHIBITED ACTIVITIES
            • Using the application for unlawful purposes
            • Attempting to reverse engineer or modify the application
            • Sharing collected data without proper authorization
            • Violating privacy rights of individuals
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