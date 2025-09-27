package mpdc4gsr.core


import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.csl.irCamera.BuildConfig
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.HttpConfig
import com.mpdc4gsr.libunified.app.lms.Config
import com.mpdc4gsr.libunified.app.lms.LMS.mContext
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.lms.utils.SPUtils
import com.mpdc4gsr.module.thermalunified.lite.IrConst
import com.mpdc4gsr.module.thermalunified.lite.util.CommonUtil
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mpdc4gsr.activities.InitUtil.initJPush
import mpdc4gsr.activities.InitUtil.initLms
import mpdc4gsr.activities.InitUtil.initLog
import mpdc4gsr.activities.InitUtil.initReceiver
import mpdc4gsr.activities.InitUtil.initUM

class App : BaseApplication() {


    companion object {
        lateinit var instance: App

        fun delayInit() {
            try {
                initLog()
                initReceiver()
                initLms()
                initUM()
                initJPush()
                XLog.i("App: delayInit completed successfully")
            } catch (e: Exception) {
                XLog.e("App: Error during delayInit: ${e.message}")
                // Continue even if some initialization fails
            }
        }
    }

    override fun getSoftWareCode(): String = BuildConfig.SOFT_CODE

    override fun isDomestic(): Boolean =
        false

    val activityNameList: MutableList<String> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            SPUtils.getInstance(this).put(Config.KEY_PRIVACY_AGREEMENT, true)

            if (SharedManager.getHasShowClause() || !isDomestic()) {
                delayInit()
            }

            RxJavaPlugins.setErrorHandler {
                if (SharedManager.getHasShowClause()) {
                    XLog.w("[ph][ph][ph][ph]： ${it.message}")
                }
            }
            if (!isDomestic()) {

                UrlConstant.setBaseUrl("${HttpConfig.HOST}/", false)
                SharedManager.setBaseHost(UrlConstant.BASE_URL)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    tau_data_H = CommonUtil.getAssetData(mContext, IrConst.TAU_HIGH_GAIN_ASSET_PATH)
                    tau_data_L = CommonUtil.getAssetData(mContext, IrConst.TAU_LOW_GAIN_ASSET_PATH)
                } catch (e: Exception) {
                    XLog.e("App: Failed to load tau data assets: ${e.message}")
                }
            }
        } catch (e: Exception) {
            XLog.e("App: Critical error during onCreate: ${e.message}")
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    if (!activityNameList.contains(activity.javaClass.getSimpleName())) {
                        activityNameList.add(activity.javaClass.getSimpleName())
                    }
                }

                override fun onActivityStarted(activity: Activity) {
                }

                override fun onActivityResumed(activity: Activity) {
                }

                override fun onActivityPaused(activity: Activity) {
                }

                override fun onActivityStopped(activity: Activity) {
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {
                }

                override fun onActivityDestroyed(activity: Activity) {
                    activityNameList.remove(activity.javaClass.getSimpleName())
                }
            },
        )

        // Initialize WebSocket connection
        initWebSocket()
    }

    private fun initZoho() {

    }

    override fun initWebSocket() {
        try {
            XLog.i("App: initWebSocket() - Initializing WebSocket connection")

            // Call parent implementation to set up network monitoring and WebSocket infrastructure
            super.initWebSocket()

            XLog.i("App: WebSocket initialization completed successfully")
        } catch (e: Exception) {
            XLog.e("App: Error during WebSocket initialization: ${e.message}")
            // Continue even if WebSocket initialization fails to avoid breaking app startup
        }
    }
}
