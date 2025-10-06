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
import mpdc4gsr.core.utils.AppLogger
import java.util.concurrent.Executors

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
                AppLogger.i("App", "delayInit completed successfully")
            } catch (e: Exception) {
                AppLogger.e("App", "Error during delayInit", e)
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

        // Initialize centralized logging
        initializeAppLogger()

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
            AppLogger.e("App", "Critical error during onCreate", e)
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
            AppLogger.i("App", "initWebSocket() - Initializing WebSocket connection")

            // Call parent implementation to set up network monitoring and WebSocket infrastructure
            super.initWebSocket()

            AppLogger.i("App", "WebSocket initialization completed successfully")
        } catch (e: Exception) {
            AppLogger.e("App", "Error during WebSocket initialization", e)
            // Continue even if WebSocket initialization fails to avoid breaking app startup
        }
    }

    private fun initializeAppLogger() {
        try {
            AppLogger.initialize(
                minLevel = if (BuildConfig.DEBUG) {
                    AppLogger.LogLevel.DEBUG
                } else {
                    AppLogger.LogLevel.WARN
                },
                enableStructured = true,
                structuredLoggerInstance = StructuredLogger.getInstance(this)
            )
            AppLogger.i("App", "AppLogger initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("App", "Failed to initialize AppLogger: ${e.message}", e)
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is IllegalStateException &&
                throwable.message?.contains("Cannot start this animator on a detached view") == true
            ) {
                AppLogger.w("App", "Caught detached view animator exception", throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun startRecordingService() {
        try {
            AppLogger.i("App", "Starting RecordingService for PC networking and control interface")
            RecordingService.startServer(this)
            AppLogger.i("App", "RecordingService started successfully")
        } catch (e: Exception) {
            AppLogger.e("App", "Failed to start RecordingService - PC networking will not be available", e)
        }
    }

    private fun enableStrictMode() {
        AppLogger.d("App", "Enabling StrictMode for debug build")

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
                            AppLogger.d("StrictMode", "Thread policy violation: ${violation.javaClass.simpleName}")
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
                                AppLogger.d("StrictMode", "VM policy violation: $violationClass")
                            }
                        }
                    }
                }
                .build()
        )

        AppLogger.i(
            "App",
            "StrictMode enabled - monitoring memory leaks and unclosed resources (disk I/O and untagged sockets permitted)"
        )
    }
}
