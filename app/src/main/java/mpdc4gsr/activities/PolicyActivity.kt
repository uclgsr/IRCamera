package mpdc4gsr

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityPolicyBinding
import com.elvishew.xlog.XLog
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PolicyActivity : BaseBindingActivity<ActivityPolicyBinding>() {
    private val mHandler = Handler(Looper.getMainLooper())

    companion object {
        const val KEY_THEME_TYPE = "key_theme_type"
        const val KEY_USE_TYPE = "key_use_type"
    }

    private var themeType = 1
    private var themeStr = ""
    private var reloadCount = 1
    private var keyUseType = 0

    override fun initContentLayoutId() = R.layout.activity_policy

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        if (intent.hasExtra(KEY_THEME_TYPE)) {
            themeType = intent.getIntExtra(KEY_THEME_TYPE, 1)
        }
        if (intent.hasExtra(KEY_USE_TYPE)) {
            keyUseType = intent.getIntExtra(KEY_USE_TYPE, 0)
        }
        themeStr =
            when (themeType) {
                1 -> getString(R.string.user_services_agreement)
                2 -> getString(R.string.privacy_policy)
                3 -> getString(R.string.third_party_components)
                else -> getString(R.string.user_services_agreement)
            }

        binding.titleView.apply {


        }

        observeHtmlData()

        if (keyUseType != 0) {
            loadHttpWhenNotInit(binding.policyWeb)
            delayShowWebView()
        }
    }

    private fun observeHtmlData() {


    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun delayShowWebView() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            launch(Dispatchers.Main) {
                binding.policyWeb.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun initData() {
        if (keyUseType == 0) {
            showLoadingDialog()

            loadDefaultContent()
        }
    }

    private fun loadDefaultContent() {

        loadHttp(binding.policyWeb)
        delayShowWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWeb(url: String) {
        binding.policyWeb.visibility = android.view.View.INVISIBLE
        val webSettings: android.webkit.WebSettings = binding.policyWeb.settings
        webSettings.javaScriptEnabled = true

        binding.policyWeb.webViewClient =
            object : android.webkit.WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: android.webkit.WebView,
                    url: String,
                ): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(
                    view: android.webkit.WebView?,
                    url: String?,
                ) {
                    super.onPageFinished(view, url)
                    Log.w("123", "onPageFinished url: $url")
                }
            }

        binding.policyWeb.webChromeClient =
            object : android.webkit.WebChromeClient() {
                override fun onProgressChanged(
                    view: android.webkit.WebView,
                    newProgress: Int,
                ) {
                    super.onProgressChanged(view, newProgress)
                }

                override fun onReceivedTitle(
                    view: android.webkit.WebView?,
                    title: String?,
                ) {
                    super.onReceivedTitle(view, title)
                    if (title!!.contains("404") && reloadCount > 0) {
                        loadHttp(view!!)
                        delayShowWebView()
                    } else {
                        mHandler.postDelayed({
                            binding.policyWeb.visibility = android.view.View.VISIBLE
                        }, 200)
                    }
                }
            }

        binding.policyWeb.settings.defaultTextEncodingName = "utf-8"
        binding.policyWeb.loadDataWithBaseURL(null, url, "text/html", "utf-8", null)
    }

    fun getHtmlData(
        htmlBody: String,
        fontColor: String,
        backgroundColor: String,
    ): String {
        val head =
            "<head>" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +
                    "<style>img{max-width: 100%; width:100%; height:auto;}video{max-width: 100%; width:100%; height:auto;}*{margin:0px;}body{font-size:16px;color: $fontColor; background-color: $backgroundColor;}</style>" + "</head>"
        return "<html>$head<body>$htmlBody</body></html>"
    }

    private fun httpErrorTip(
        text: String,
        requestUrl: String,
    ) {
        XLog.w("[ph][ph][ph][ph][ph][ph],[ph][ph][ph][ph][ph][ph]")
        loadHttp(binding.policyWeb)
        delayShowWebView()
    }

    fun loadHttpWhenNotInit(view: android.webkit.WebView) {
        reloadCount--
        when (themeType) {
            1 -> {

                view.loadUrl(
                    "https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?softCode=${BaseApplication.instance.getSoftWareCode()}&language=1&type=21",
                )
            }

            2 -> {

                view.loadUrl(
                    "https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?softCode=${BaseApplication.instance.getSoftWareCode()}&language=1&type=22",
                )
            }

            3 -> {

                view.loadUrl("file:///android_asset/web/third_statement.html")
            }
        }
    }

    fun loadHttp(view: android.webkit.WebView) {
        reloadCount--
        when (themeType) {
            1 -> {
                if (BaseApplication.instance.isDomestic()) {
                    view.loadUrl("file:///android_asset/web/services_agreement_default_inside_china.html")
                } else {

                    view.loadUrl("file:///android_asset/web/services_agreement_default.html")
                }
            }

            2 -> {
                if (BaseApplication.instance.isDomestic()) {
                    view.loadUrl("file:///android_asset/web/privacy_default_inside_china.html")
                } else {

                    view.loadUrl("file:///android_asset/web/privacy_default.html")
                }
            }

            3 -> {

                view.loadUrl("file:///android_asset/web/third_statement.html")
            }
        }
    }
}
