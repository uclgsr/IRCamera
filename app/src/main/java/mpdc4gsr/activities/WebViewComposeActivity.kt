package mpdc4gsr.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.csl.irCamera.R
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of WebViewActivity demonstrating WebView integration with error handling.
 * Shows how to handle complex WebView interactions within Compose architecture.
 */
class WebViewComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        val url = intent.extras?.getString(ExtraKeyConfig.URL) ?: ""
        
        IRCameraTheme {
            WebViewScreen(url = url)
        }
    }

    @Composable
    private fun WebViewScreen(url: String) {
        var isLoading by remember { mutableStateOf(true) }
        var hasError by remember { mutableStateOf(false) }
        var webView by remember { mutableStateOf<BridgeWebView?>(null) }

        LaunchedEffect(Unit) {
            showLoadingDialog()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16131E))
        ) {
            // WebView
            AndroidView(
                factory = { context ->
                    BridgeWebView(context).apply {
                        setupWebView(this, url) { loading, error ->
                            isLoading = loading
                            hasError = error
                            if (!loading) {
                                dismissLoadingDialog()
                            }
                        }
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading cover
            if (isLoading && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF16131E))
                )
            }

            // Error state
            if (hasError) {
                ErrorScreen(
                    onRetry = {
                        hasError = false
                        isLoading = true
                        showLoadingDialog()
                        webView?.loadUrl(url)
                    }
                )
            }
        }
    }

    @Composable
    private fun ErrorScreen(onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16131E))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error icon
            Image(
                painter = painterResource(id = R.drawable.ic_web_view_error),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            // Error message
            Text(
                text = stringResource(R.string.loading_failed_hint),
                color = Color(0x80FFFFFF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Retry button
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.app_reload),
                    color = Color.White
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(
        webView: BridgeWebView,
        url: String,
        onStateChange: (isLoading: Boolean, hasError: Boolean) -> Unit
    ) {
        val webSettings: WebSettings = webView.settings
        webSettings.setSupportZoom(false)
        webSettings.useWideViewPort = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.defaultTextEncodingName = "UTF-8"
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = object : BridgeWebViewClient(webView) {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onStateChange(false, false)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    onStateChange(false, true)
                }
            }
        }

        webView.registerHandler("goBack") { _, function ->
            function.onCallBack("android")
        }

        webView.loadUrl(url)
        webView.isScrollContainer = false
    }
}