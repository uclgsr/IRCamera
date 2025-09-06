package com.topdon.tc001

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lms.sdk.Config
import com.topdon.lms.sdk.LMS
import com.csl.irCamera.R
import com.csl.irCamera.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    // findViewById declarations
    private val tvAppName by lazy { findViewById<TextView>(R.id.tv_app_name) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LMS.getInstance().screenOrientation = Config.SCREEN_PORTRAIT
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_splash)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)

        lifecycleScope.launch {
            delay(if (BuildConfig.DEBUG) 3000 else 1000)
            if (SharedManager.getHasShowClause()) {
                NavigationManager.build(RouterConfig.MAIN).navigation(this@SplashActivity)
            } else {
                NavigationManager.build(RouterConfig.CLAUSE).navigation(this@SplashActivity)
            }
            finish()
        }
        tvAppName.text = CommUtils.getAppName()
    }

    override fun onBackPressed() {

    }
}