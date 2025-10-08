package mpdc4gsr.core

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
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
            } catch (e: Exception) {
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
        @Suppress("DEPRECATION")
        instance = this
        // Enable StrictMode in debug builds to catch performance issues early
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        // Initialize performance metrics tracking as early as possible
        mpdc4gsr.core.monitoring.PerformanceMetrics.initialize()
        // Initialize ContextProvider for AndroidX migration
        ContextProvider.init(this)

        // Load native libraries for thermal camera support
        loadNativeLibraries()

        // Initialize telemetry and observability
        mpdc4gsr.core.monitoring.TelemetryManager.initialize(this)
        setupGlobalExceptionHandler()
        try {
            SPUtils.getInstance(this).put(Config.KEY_PRIVACY_AGREEMENT, true)
            if (SharedManager.getHasShowClause() || !isDomestic()) {
                // Initialize immediately to ensure USB receiver is registered before activities start
                delayInit()
            }
            // RxJava error handling removed - using Kotlin Coroutines exception handling
            if (!isDomestic()) {
                UrlConstants.setBaseUrl("${HttpConfig.HOST}/", false)
                SharedManager.setBaseHost(UrlConstants.BASE_URL)
            }

        } catch (e: Exception) {
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
        // Start RecordingService to enable PC networking and control interface
        startRecordingService()
    }

    override fun initWebSocket() {
        try {
            // Call parent implementation to set up network monitoring and WebSocket infrastructure
            super.initWebSocket()
        } catch (e: Exception) {
            // Continue even if WebSocket initialization fails to avoid breaking app startup
        }
    }



    private fun loadNativeLibraries() {
        try {
            System.loadLibrary("USBUVCCamera")
        } catch (e: UnsatisfiedLinkError) {
        } catch (e: Exception) {
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is IllegalStateException &&
                throwable.message?.contains("Cannot start this animator on a detached view") == true
            ) {
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun startRecordingService() {
        try {
            RecordingService.startServer(this)
        } catch (e: Exception) {
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .permitDiskReads()
                .permitDiskWrites()
                .penaltyLog()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(mainExecutor) { violation ->
                        }
                    }
                }
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(mainExecutor) { violation ->
                            val violationClass = violation.javaClass.simpleName
                            if (violationClass != "UntaggedSocketViolation") {
                            }
                        }
                    }
                }
                .build()
        )
    }
}
