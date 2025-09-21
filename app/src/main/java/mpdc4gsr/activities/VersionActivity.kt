package mpdc4gsr.activities

import android.os.Bundle
import android.view.View
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityVersionBinding
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity
import com.mpdc4gsr.lib.core.lms.LMS
import com.mpdc4gsr.lib.core.lms.UrlConstant
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.tools.CheckDoubleClick
import com.mpdc4gsr.lib.core.utils.CommUtils
import mpdc4gsr.utils.AppVersionUtil
import mpdc4gsr.utils.VersionUtils
import java.util.Calendar

class VersionActivity : BaseBindingActivity<ActivityVersionBinding>(), View.OnClickListener {
    override fun initContentLayoutId(): Int = R.layout.activity_version

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {

        binding.versionCodeText.text =
            "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.versionYearTxt.text = getString(R.string.version_year, "2023-$year")
        binding.versionStatementPrivateTxt.setOnClickListener(this)
        binding.versionStatementPolicyTxt.setOnClickListener(this)
        binding.versionStatementCopyrightTxt.setOnClickListener(this)

        binding.settingVersionImg.setOnClickListener {
            if (BuildConfig.DEBUG && CheckDoubleClick.isFastDoubleClick()) {
                LMS.getInstance().activityEnv()
            }
        }
        binding.includeNewVersion.clNewVersion.setOnClickListener {
            if (!CheckDoubleClick.isFastDoubleClick()) {
                checkAppVersion(true)
            }
        }
        binding.settingVersionTxt.text = CommUtils.getAppName()
    }

    private fun initData() {
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(false)
        }
    }

    override fun onResume() {
        super.onResume()
        SharedManager.setBaseHost(UrlConstant.BASE_URL)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.versionStatementPrivateTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .navigation(this)
            }

            binding.versionStatementPolicyTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .navigation(this)
            }

            binding.versionStatementCopyrightTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                    .navigation(this)
            }
        }
    }

    private var appVersionUtil: AppVersionUtil? = null

    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil =
                AppVersionUtil(
                    this,
                    object : AppVersionUtil.DotIsShowListener {
                        override fun isShow(show: Boolean) {
                            binding.includeNewVersion.clNewVersion.visibility = View.VISIBLE
                        }

                        override fun version(version: String) {
                            binding.includeNewVersion.tvNewVersion.text = "$version"
                        }
                    },
                )
        }
        appVersionUtil?.checkVersion(isShow)
    }
}
