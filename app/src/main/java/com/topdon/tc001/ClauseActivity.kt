package com.topdon.tc001

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipProgressDialog
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.utils.NetworkUtil
import com.topdon.lms.sdk.weiget.TToast
import com.csl.irCamera.R
import com.topdon.lib.core.R as LibCoreR
import com.topdon.tc001.app.App
import com.topdon.tc001.utils.VersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 条款
 */
// Legacy ARouter route annotation - now using NavigationManager
class ClauseActivity : AppCompatActivity() {

    private lateinit var dialog: TipProgressDialog
    
    // findViewById declarations
    private val clauseYearTxt by lazy { findViewById<TextView>(R.id.clause_year_txt) }
    private val clauseAgreeBtn by lazy { findViewById<Button>(R.id.clause_agree_btn) }
    private val clauseDisagreeBtn by lazy { findViewById<Button>(R.id.clause_disagree_btn) }
    private val clauseItem by lazy { findViewById<View>(R.id.clause_item) }
    private val clauseItem2 by lazy { findViewById<View>(R.id.clause_item2) }
    private val clauseItem3 by lazy { findViewById<View>(R.id.clause_item3) }
    private val tvPrivacy by lazy { findViewById<TextView>(R.id.tv_privacy) }
    private val tvWelcome by lazy { findViewById<TextView>(R.id.tv_welcome) }
    private val tvVersion by lazy { findViewById<TextView>(R.id.tv_version) }
    private val clauseName by lazy { findViewById<TextView>(R.id.clause_name) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clause)
        initView()
    }


    private fun initView() {
        dialog = TipProgressDialog.Builder(this)
            .setMessage(LibCoreR.string.tip_loading)
            .setCanceleable(false)
            .create()

        val year = Calendar.getInstance().get(Calendar.YEAR)
        clauseYearTxt.text = getString(R.string.version_year, "2023-$year")

        clauseAgreeBtn.setOnClickListener {
            confirmInitApp()
        }
        clauseDisagreeBtn.setOnClickListener {
            //再次弹框确认是否退出
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
        clauseItem.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                //服务条款
                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        clauseItem2.setOnClickListener {
            if (!NetworkUtil.isConnected(this)) {
                TToast.shortToast(this, R.string.lms_setting_http_error)
            } else {
                //隐私条款
                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        clauseItem3.setOnClickListener {
            //第三方
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
            tvPrivacy.text = "    ${getString(R.string.privacy_agreement_tips_new, CommUtils.getAppName())}"
            tvPrivacy.visibility = View.VISIBLE
            tvPrivacy.movementMethod = ScrollingMovementMethod.getInstance()
        }
        tvWelcome.text = getString(R.string.welcome_use_app, CommUtils.getAppName())
        tvVersion.text = "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
        clauseName.text = CommUtils.getAppName()
    }

    private fun confirmInitApp() {
        lifecycleScope.launch {
            showLoading()
            //初始化
            App.delayInit()
            async(Dispatchers.IO) {
                //等待1000ms 初始化结束
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