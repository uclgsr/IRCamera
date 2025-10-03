package mpdc4gsr.core

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import com.csl.irCamera.BuildConfig
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.HttpConfig
import com.mpdc4gsr.libunified.app.lms.Config
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.utils.SPUtils
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.libunified.app.utils.WifiUtils
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import dagger.hilt.android.HiltAndroidApp
import mpdc4gsr.core.ui.InitUtils.initJPush
import mpdc4gsr.core.ui.InitUtils.initLms
import mpdc4gsr.core.ui.InitUtils.initLog
import mpdc4gsr.core.ui.InitUtils.initReceiver
import mpdc4gsr.core.ui.InitUtils.initUM
import mpdc4gsr.core.utils.AppLogger

/**
 * Application class for IRCamera with Hilt dependency injection.
 * 
 * Refactored to extend Application directly (instead of BaseApplication) to enable
 * Hilt dependency injection throughout the application. Essential functionality from
 * BaseApplication has been integrated here.
 * 
 * The static instance reference is kept temporarily for backward compatibility during
 * the migration, but should be avoided in new code.
 * 
 * Use constructor injection with @Inject or @HiltViewModel for new components.
 */
//@HiltAndroidApp
class App : Application() {

    // Methods previously abstract in BaseApplication
    fun getSoftWareCode(): String = BuildConfig.SOFT_CODE

    fun isDomestic(): Boolean = false

    val activityNameList: MutableList<String> = mutableListOf()

    companion object {
        val usbObserver by lazy { DeviceBroadcastReceiver() }

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

    companion object {
        val usbObserver by lazy { DeviceBroadcastReceiver() }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize ContextProvider for AndroidX migration
        ContextProvider.init(this)

        // Set WebView data directory path (Android P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val processName = getProcessName()
                if (processName != null && processName != packageName) {
                    WebView.setDataDirectorySuffix(processName)
                }
            } catch (e: Exception) {
                AppLogger.w("App", "Failed to set WebView data directory", e)
            }
        }
        
        // Initialize centralized logging
        initializeAppLogger()

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
    }

    fun initWebSocket() {
        try {
            AppLogger.i("App", "initWebSocket() - Initializing WebSocket connection")

            // Set up WebSocket connection based on WiFi SSID
            connectWebSocket()

            // Register network monitoring callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setupNetworkCallbackQ()
            } else {
                setupNetworkBroadcastReceiver()
            }

            AppLogger.i("App", "WebSocket initialization completed successfully")
        } catch (e: Exception) {
            AppLogger.e("App", "Error during WebSocket initialization", e)
            // Continue even if WebSocket initialization fails to avoid breaking app startup
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupNetworkCallbackQ() {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        manager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val capabilities = manager.getNetworkCapabilities(network)
                    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        connectWebSocket()
                        Log.i("WebSocket", "WiFi network available: $network")
                    }
                }
            }
        )
    }

    private fun setupNetworkBroadcastReceiver() {
        val networkChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                connectWebSocket()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                networkChangedReceiver,
                IntentFilter().apply {
                    addAction("android.net.conn.CONNECTIVITY_CHANGE")
                },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                networkChangedReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }

    private fun connectWebSocket() {
        val ssid = WifiUtils.getCurrentWifiSSID(this) ?: return
        Log.i("WebSocket", "current WiFi SSID: $ssid")
        NetWorkUtils.switchNetwork(true)
    }

    fun disconnectWebSocket() {
        Log.i("WebSocket", "disconnectWebSocket()")
        WebSocketProxy.getInstance().stopWebSocket()
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
}
