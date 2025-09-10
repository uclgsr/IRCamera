package com.topdon.tc001
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.*
import android.text.style.UnderlineSpan
import android.view.View
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMoreHelpBinding
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.utils.Constants

// Legacy ARouter route annotation - now using NavigationManager
class MoreHelpActivity : BaseBindingActivity<ActivityMoreHelpBinding>() {
    private var connectionType: Int = 0
    private lateinit var wifiManager: WifiManager
    
    override fun initContentLayoutId(): Int = R.layout.activity_more_help
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        initIntent()
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun initIntent() {
        connectionType = intent.getIntExtra(Constants.SETTING_CONNECTION_TYPE, 0)
        if (connectionType == Constants.SETTING_CONNECTION) {
            binding.tvTitle.text = getString(R.string.ts004_guide_text8)
            binding.titleView.setTitleText(R.string.ts004_guide_text6)
            binding.mainGuideTip1.visibility = View.VISIBLE
            binding.mainGuideTip2.visibility = View.VISIBLE
            binding.mainGuideTip4.visibility = View.VISIBLE
            binding.disconnectTip1.visibility = View.GONE
            binding.disconnectTip2.visibility = View.GONE
            binding.ivTvSetting.visibility = View.GONE
        } else {
            binding.tvTitle.text = getString(R.string.ts004_disconnect_tips1)
            binding.mainGuideTip1.visibility = View.GONE
            binding.mainGuideTip2.visibility = View.GONE
            binding.mainGuideTip4.visibility = View.GONE
            binding.disconnectTip1.visibility = View.VISIBLE
            binding.disconnectTip2.visibility = View.VISIBLE
            binding.ivTvSetting.visibility = View.VISIBLE
            val spannable = SpannableStringBuilder(getString(R.string.ts004_disconnect_tips4))
            spannable.setSpan(UnderlineSpan(), 0, getString(R.string.ts004_disconnect_tips4).length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.ivTvSetting.text = spannable
        }
    }

    private fun initData() {
        binding.ivTvSetting.setOnClickListener {
            startWifiList()
        }
    }

    private fun startWifiList() {
        if (wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT < 29) { // 低于 Android10
                wifiManager.isWifiEnabled = true
            } else {
                var wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
                if (wifiIntent.resolveActivity(packageManager) == null) {
                    wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    if (wifiIntent.resolveActivity(packageManager) != null) {
                        startActivity(wifiIntent)
                    }
                } else {
                    startActivity(wifiIntent)
                }
            }
        } else {
            TipDialog.Builder(this)
                .setTitleMessage(getString(R.string.app_tip))
                .setMessage(R.string.ts004_wlan_tips)
                .setPositiveListener(R.string.app_open) {
                    if (Build.VERSION.SDK_INT < 29) { // 低于 Android10
                        wifiManager.isWifiEnabled = true
                    } else {
                        var wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
                        if (wifiIntent.resolveActivity(packageManager) == null) {
                            wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                            if (wifiIntent.resolveActivity(packageManager) != null) {
                                startActivity(wifiIntent)
                            }
                        } else {
                            startActivity(wifiIntent)
                        }
                    }
                }
                .setCancelListener(R.string.app_cancel) {
                }
                .setCanceled(true)
                .create().show()
        }
    }
}
