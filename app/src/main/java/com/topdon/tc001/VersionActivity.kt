package com.topdon.tc001

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.CheckDoubleClick
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.UrlConstant
import com.topdon.tc001.utils.AppVersionUtil
import com.csl.irCamera.R
import com.csl.irCamera.BuildConfig
import com.topdon.tc001.utils.VersionUtils
import java.util.*

// Legacy ARouter route annotation - now using NavigationManager
class VersionActivity : BaseActivity(), View.OnClickListener {

    // View declarations using findViewById pattern
    private lateinit var versionYearTxt: TextView
    private lateinit var versionStatementPrivateTxt: TextView
    private lateinit var versionStatementPolicyTxt: TextView
    private lateinit var versionStatementCopyrightTxt: TextView
    private lateinit var settingVersionImg: ImageView
    private lateinit var clNewVersion: ConstraintLayout
    private lateinit var settingVersionTxt: TextView
    private lateinit var tvNewVersion: TextView
    override fun initContentView() = R.layout.activity_version

    override fun initView() {
        // Initialize views using findViewById
        versionYearTxt = findViewById(R.id.version_year_txt)
        versionStatementPrivateTxt = findViewById(R.id.version_statement_private_txt)
        versionStatementPolicyTxt = findViewById(R.id.version_statement_policy_txt)
        versionStatementCopyrightTxt = findViewById(R.id.version_statement_copyright_txt)
        settingVersionImg = findViewById(R.id.setting_version_img)
        clNewVersion = findViewById(R.id.cl_new_version)
        settingVersionTxt = findViewById(R.id.setting_version_txt)
        tvNewVersion = findViewById(R.id.tv_new_version)
        
        // Set up views
        findViewById<TextView>(R.id.version_code_text).text = "${getString(R.string.set_version)}V${VersionUtils.getCodeStr(this)}"
        val year = Calendar.getInstance().get(Calendar.YEAR)
        versionYearTxt.text = getString(R.string.version_year, "2023-$year")
        versionStatementPrivateTxt.setOnClickListener(this)
        versionStatementPolicyTxt.setOnClickListener(this)
        versionStatementCopyrightTxt.setOnClickListener(this)

        settingVersionImg.setOnClickListener {
            if (BuildConfig.DEBUG && CheckDoubleClick.isFastDoubleClick()) {
                LMS.getInstance().activityEnv()
            }
        }
        clNewVersion.setOnClickListener {
            if (!CheckDoubleClick.isFastDoubleClick()) {
                checkAppVersion(true)
            }
        }
        settingVersionTxt.text = CommUtils.getAppName()
    }

    override fun initData() {
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
            versionStatementPrivateTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 1)
                    .navigation(this)
            }
            versionStatementPolicyTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 2)
                    .navigation(this)
            }
            versionStatementCopyrightTxt -> {
                NavigationManager.build(RouterConfig.POLICY)
                    .withInt(PolicyActivity.KEY_THEME_TYPE, 3)
                    .navigation(this)
            }
        }
    }

    private var appVersionUtil: AppVersionUtil?=null
    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil = AppVersionUtil(this, object : AppVersionUtil.DotIsShowListener {
                override fun isShow(show: Boolean) {
                    clNewVersion.visibility = View.VISIBLE
                }

                override fun version(version: String) {
                    tvNewVersion.text = "$version"
                }
            })
        }
        appVersionUtil?.checkVersion(isShow)
    }

}