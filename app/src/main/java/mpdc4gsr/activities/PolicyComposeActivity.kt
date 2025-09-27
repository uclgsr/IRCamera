package mpdc4gsr.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of PolicyActivity demonstrating WebView integration in Compose.
 * Shows how to handle complex Views within Compose architecture.
 */
class PolicyComposeActivity : BaseComposeActivity() {

    companion object {
        const val KEY_THEME_TYPE = "key_theme_type"
        const val KEY_USE_TYPE = "key_use_type"
    }

    private var themeType = 1
    private var keyUseType = 0

    @Composable
    override fun Content() {
        // Extract intent data in Compose-compatible way
        LaunchedEffect(Unit) {
            themeType = intent.getIntExtra(KEY_THEME_TYPE, 1)
            keyUseType = intent.getIntExtra(KEY_USE_TYPE, 0)
        }

        IRCameraTheme {
            PolicyScreen(
                themeType = themeType,
                keyUseType = keyUseType,
                onBackPressed = { finish() }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PolicyScreen(
        themeType: Int,
        keyUseType: Int,
        onBackPressed: () -> Unit
    ) {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(keyUseType == 0) }
        var webViewVisible by remember { mutableStateOf(keyUseType != 0) }

        // Determine title based on theme type
        val title = when (themeType) {
            1 -> stringResource(R.string.user_services_agreement)
            2 -> stringResource(R.string.privacy_policy)
            3 -> stringResource(R.string.third_party_components)
            else -> stringResource(R.string.user_services_agreement)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // WebView content
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setupWebView(this)
                            loadContent(themeType, keyUseType)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { webView ->
                    // Update WebView when parameters change
                    if (webViewVisible) {
                        webView.alpha = 1f
                    } else {
                        webView.alpha = 0f
                    }
                }

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Handle delayed WebView visibility (mimicking original behavior)
        LaunchedEffect(keyUseType) {
            if (keyUseType != 0) {
                delay(200)
                webViewVisible = true
            } else {
                // Load default content
                showLoadingDialog()
                delay(200)
                webViewVisible = true
                dismissLoadingDialog()
                isLoading = false
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Handle page load completion
                }
            }
        }
    }

    private fun WebView.loadContent(themeType: Int, keyUseType: Int) {
        // Load appropriate content based on theme type
        val htmlContent = when (themeType) {
            1 -> loadUserAgreementContent()
            2 -> loadPrivacyPolicyContent()
            3 -> loadThirdPartyContent()
            else -> loadUserAgreementContent()
        }
        
        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadUserAgreementContent(): String {
        // Return HTML content for user agreement
        // This could be loaded from assets or built dynamically
        return """
            <html>
            <head><meta charset="UTF-8"></head>
            <body>
                <h1>User Services Agreement</h1>
                <p>Agreement content goes here...</p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun loadPrivacyPolicyContent(): String {
        return """
            <html>
            <head><meta charset="UTF-8"></head>
            <body>
                <h1>Privacy Policy</h1>
                <p>Privacy policy content goes here...</p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun loadThirdPartyContent(): String {
        // Load from assets file
        return try {
            assets.open("web/third_statement.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            """
                <html>
                <head><meta charset="UTF-8"></head>
                <body>
                    <h1>Third Party Components</h1>
                    <p>Third party components information goes here...</p>
                </body>
                </html>
            """.trimIndent()
        }
    }
}