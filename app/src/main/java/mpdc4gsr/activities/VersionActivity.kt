package mpdc4gsr

import android.os.Bundle
import android.view.View
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityVersionBinding
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.tools.CheckDoubleClick
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.UrlConstant
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
