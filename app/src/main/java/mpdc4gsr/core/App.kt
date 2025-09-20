package mpdc4gsr.app


import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.csl.irCamera.BuildConfig
import com.elvishew.xlog.XLog
import com.example.thermal_lite.IrConst
import com.example.thermal_lite.util.CommonUtil
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.config.HttpConfig
import com.mpdc4gsr.lib.core.lms.Config
import com.mpdc4gsr.lib.core.lms.LMS.mContext
import com.mpdc4gsr.lib.core.lms.UrlConstant
import com.mpdc4gsr.lib.core.lms.utils.SPUtils
import mpdc4gsr.InitUtil.initJPush
import mpdc4gsr.InitUtil.initLms
import mpdc4gsr.InitUtil.initLog
import mpdc4gsr.InitUtil.initReceiver
import mpdc4gsr.InitUtil.initUM
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : BaseApplication() {


    companion object {
        lateinit var instance: App

        fun delayInit() {
            initReceiver()
            initLog()
            initLms()
            initUM()
            initJPush()
        }
    }

    override fun getSoftWareCode(): String = BuildConfig.SOFT_CODE

    override fun isDomestic(): Boolean =
        false

    val activityNameList: MutableList<String> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        instance = this

        SPUtils.getInstance(this).put(Config.KEY_PRIVACY_AGREEMENT, true)

        if (SharedManager.getHasShowClause() || !isDomestic()) {
            delayInit()
        }

        RxJavaPlugins.setErrorHandler {
            if (SharedManager.getHasShowClause()) {
                XLog.w("未知异常： ${it.message}")
            }
        }
        if (!isDomestic()) {

            UrlConstant.setBaseUrl("${HttpConfig.HOST}/", false)
            SharedManager.setBaseHost(UrlConstant.BASE_URL)
        }

        CoroutineScope(Dispatchers.IO).launch {
            tau_data_H = CommonUtil.getAssetData(mContext, IrConst.TAU_HIGH_GAIN_ASSET_PATH)
            tau_data_L = CommonUtil.getAssetData(mContext, IrConst.TAU_LOW_GAIN_ASSET_PATH)
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

    }

    private fun initZoho() {


    }
}
