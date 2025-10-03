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
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.utils.SPUtils
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import mpdc4gsr.core.ui.InitUtils.initJPush
import mpdc4gsr.core.ui.InitUtils.initLms
import mpdc4gsr.core.ui.InitUtils.initLog
import mpdc4gsr.core.ui.InitUtils.initReceiver
import mpdc4gsr.core.ui.InitUtils.initUM

/**
 * Application class for IRCamera.
 * 
 * ANTI-PATTERN WARNING: Static Application instance
 * This class uses a static `instance` reference which is an anti-pattern that:
 * - Creates tight coupling between components
 * - Makes testing difficult
 * - Hides dependencies
 * - Can lead to memory leaks if misused
 * 
 * TODO: Migrate to Hilt Dependency Injection (Estimated: 16-24 hours)
 * 
 * Migration Plan:
 * 1. Add Hilt dependencies to build.gradle.kts
 * 2. Annotate this class with @HiltAndroidApp
 * 3. Create @Module classes for dependencies
 * 4. Replace getInstance() calls with constructor injection
 * 5. Update Activities/Fragments to use @AndroidEntryPoint
 * 6. Remove static instance reference
 * 
 * For now, ContextProvider is available as a safer alternative for accessing
 * application context in most cases.
 */
class App : BaseApplication() {

    companion object {
        @Deprecated(
            message = "Use dependency injection instead of static instance",
            replaceWith = ReplaceWith("Use Hilt @Inject or ContextProvider.getContext()"),
            level = DeprecationLevel.WARNING
        )
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
        
        // Initialize ContextProvider for AndroidX migration
        ContextProvider.init(this)

        setupGlobalExceptionHandler()

        try {
            SPUtils.getInstance(this).put(Config.KEY_PRIVACY_AGREEMENT, true)

            if (SharedManager.getHasShowClause() || !isDomestic()) {
                delayInit()
            }

            // RxJava error handling removed - using Kotlin Coroutines exception handling
            if (!isDomestic()) {

                UrlConstants.setBaseUrl("${HttpConfig.HOST}/", false)
                SharedManager.setBaseHost(UrlConstants.BASE_URL)
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

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {}

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

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is IllegalStateException &&
                throwable.message?.contains("Cannot start this animator on a detached view") == true
            ) {
                XLog.w("App: Caught detached view animator exception: ${throwable.message}")
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
