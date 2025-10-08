package mpdc4gsr.feature.settings.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.ViewModel
import com.csl.irCamera.R
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor() : ViewModel() {
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

@AndroidEntryPoint
class PolicyComposeActivity : ComponentActivity() {
    companion object {
        const val KEY_THEME_TYPE = "key_theme_type"
        const val KEY_USE_TYPE = "key_use_type"
    }

    private val viewModel: PolicyViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeType = intent.getIntExtra(KEY_THEME_TYPE, 1)
        viewModel.setPolicyType(themeType)
        setContent {
            IRCameraTheme {
                PolicyScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(
    viewModel: PolicyViewModel,
    onBackClick: () -> Unit
) {
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