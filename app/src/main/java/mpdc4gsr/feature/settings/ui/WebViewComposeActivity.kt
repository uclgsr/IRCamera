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