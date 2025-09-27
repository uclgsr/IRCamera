package mpdc4gsr.activities

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityClauseBinding
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.dialog.TipProgressDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtil
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.App
import java.util.Calendar
import com.mpdc4gsr.libunified.R as LibCoreR

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

                NavigationManager.getInstance()
                    .build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .withInt(PolicyActivity.KEY_USE_TYPE, keyUseType)
                    .navigation(this)
            }
        }
        binding.clauseItem3.setOnClickListener {

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
            binding.tvPrivacy.text =
                "    ${getString(R.string.privacy_agreement_tips_new, CommUtils.getAppName())}"
            binding.tvPrivacy.visibility = android.view.View.VISIBLE
            binding.tvPrivacy.movementMethod = ScrollingMovementMethod.getInstance()
        }
        binding.tvWelcome.text = getString(R.string.welcome_use_app, CommUtils.getAppName())
        binding.tvVersion.text =
            "${getString(R.string.set_version)}V${UnifiedVersionUtils.getVersionName(this)}"
        binding.clauseName.text = CommUtils.getAppName()
    }

    private fun confirmInitApp() {
        lifecycleScope.launch {
            showLoading()

            App.delayInit()
            async(Dispatchers.IO) {

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
