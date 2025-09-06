package com.topdon.tc001

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import com.github.lzyzsd.jsbridge.BridgeWebView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible

import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.csl.irCamera.R

/**
 * 使用 WebView 加载网页的 Activity.
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.URL] 要加载网页地址
 *
 * Created by LCG on 2024/12/18.
 */
// Legacy ARouter route annotation - now using NavigationManager
class WebViewActivity : BaseActivity() {

    // View declarations
    private lateinit var tvReload: TextView
    private lateinit var viewCover: View
    private lateinit var clError: ConstraintLayout
    private lateinit var webView: BridgeWebView

    override fun initContentView(): Int = R.layout.activity_web_view

    override fun initView() {
        // Initialize views using findViewById
        tvReload = findViewById(R.id.tv_reload)
        viewCover = findViewById(R.id.view_cover)
        clError = findViewById(R.id.cl_error)
        webView = findViewById(R.id.web_view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initData() {
        showLoadingDialog()

        val url: String = intent.extras?.getString(ExtraKeyConfig.URL) ?: ""

        tvReload.setOnClickListener {
            showLoadingDialog()
            viewCover.isVisible = true
            clError.isVisible = false
            webView.loadUrl(url)
        }

        val webSettings: WebSettings = webView.settings
        webSettings.setSupportZoom(false)//设置不支持字体缩放
        webSettings.useWideViewPort = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true //允许js弹出窗口
        webSettings.defaultTextEncodingName = "UTF-8"
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = object : BridgeWebViewClient(webView) {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                dismissLoadingDialog()
                viewCover.isVisible = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                dismissLoadingDialog()
                viewCover.isVisible = false
                if (request?.isForMainFrame == true) {
                    clError.isVisible = true
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