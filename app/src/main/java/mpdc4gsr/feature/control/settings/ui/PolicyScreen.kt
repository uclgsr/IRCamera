package mpdc4gsr.feature.control.settings.ui

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
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

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

