package com.topdon.tc001

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityClauseBinding
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipProgressDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.utils.NetworkUtil
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.tc001.app.App
import com.topdon.tc001.utils.VersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import com.topdon.lib.core.R as LibCoreR


// Legacy ARouter route annotation - now using NavigationManager
class ClauseActivity : BaseBindingActivity<ActivityClauseBinding>() {
    private lateinit var dialog: TipProgressDialog

    override fun initContentLayoutId() = R.layout.activity_clause

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    }

    private fun initView() {
    dialog =
    TipProgressDialog.Builder(this)
    .setMessage(LibCoreR.string.tip_loading)
    .setCanceleable(false)
    .create()

    val year = Calendar.getInstance().get(Calendar.YEAR)
    binding.clauseYearTxt.text = getString(R.string.version_year, "2023-$year")

        binding.clauseAgreeBtn.setOnClickListener {
            confirmInitApp()
        }
        binding.clauseDisagreeBtn.setOnClickListener {
            // 再次弹框Confirm是否Exit
            TipDialog.Builder(this)
                .setMessage(getString(R.string.privacy_tips))
                .setPositiveListener(R.string.privacy_confirm) {
                    confirmInitApp()
                }
                .setCancelListener(R.string.privacy_cancel) {
                    this.finish()
                }
                .setCanceled(true)
                .create().show()
        }
        val keyUseType = if (BaseApplication.instance.isDomestic()) 1 else 0
        binding.clauseItem.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                // 服务条款
                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        binding.clauseItem2.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                // 隐私条款
                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        binding.clauseItem3.setOnClickListener {
            // 第三方
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }

    if (BaseApplication.instance.isDomestic()) {
    binding.tvPrivacy.text = "    ${getString(R.string.privacy_agreement_tips_new, CommUtils.getAppName())}"
    binding.tvPrivacy.visibility = android.view.View.VISIBLE
    binding.tvPrivacy.movementMethod = ScrollingMovementMethod.getInstance()
    }
    binding.tvWelcome.text = getString(R.string.welcome_use_app, CommUtils.getAppName())
    binding.tvVersion.text = "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
    binding.clauseName.text = CommUtils.getAppName()
    }

    private fun confirmInitApp() {
    lifecycleScope.launch {
    showLoading()
    // 初始化
    App.delayInit()
    async(Dispatchers.IO) {
    // 等待1000ms 初始化结束
    delay(1000)
    return@async
    }.await().let {
    NavigationManager.build(RouterConfig.MAIN).navigation(this@ClauseActivity)
    SharedManager.setHasShowClause(true)
    dismissLoading()
    finish()
    }
    }
    }

    private fun showLoading() {
    dialog.show()
    }

    private fun dismissLoading() {
    dialog.dismiss()
    }
}
