package com.mpdc4gsr.libunified.app

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.text.TextUtils
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.socket.SocketCmdUtils
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import com.mpdc4gsr.libunified.app.utils.LibraryLogger
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.libunified.app.utils.WifiUtils
import com.mpdc4gsr.libunified.app.utils.WsCmdConstants
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.model.IRTempConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

abstract class BaseApplication : Application() {
    companion object {
        lateinit var instance: BaseApplication
        val usbObserver by lazy { DeviceBroadcastReceiver() }
    }

    // Application-scoped coroutine scope for database operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    var tau_data_H: ByteArray? = null
    var tau_data_L: ByteArray? = null
    var config: IRTempConfig? = null
    val module: String get() = javaClass.simpleName
    var activitys = arrayListOf<Activity>()
    var hasOtgShow = false

    abstract fun getSoftWareCode(): String

    abstract fun isDomestic(): Boolean

    override fun onCreate() {
        super.onCreate()
        instance = this
        ContextProvider.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webviewSetPath(this)
        }
        onLanguageChange()
        WebSocketProxy.getInstance().onMessageListener = {
            parserSocketMessage(it)
        }
    }

    open fun initWebSocket() {
        connectWebSocket()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest =
                android.net.NetworkRequest
                    .Builder()
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
                        }
                    }
                },
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    NetworkChangedReceiver(),
                    IntentFilter().apply {
                        addAction("android.net.conn.CONNECTIVITY_CHANGE")
                    },
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(
                    NetworkChangedReceiver(),
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                )
            }
        }
    }

    private fun connectWebSocket() {
        val ssid = WifiUtils.getCurrentWifiSSID(this) ?: return
        // TS004/TC007 device functionality removed
        // if (ssid.startsWith(DeviceConfig.TS004_NAME_START)) {
        //     SharedManager.hasTS004 = true
        //     WebSocketProxy.getInstance().startWebSocket(ssid)
        // } else if (ssid.startsWith(DeviceConfig.TC007_NAME_START)) {
        //     SharedManager.hasTC007 = true
        //     WebSocketProxy.getInstance().startWebSocket(ssid)
        // } else {
        NetWorkUtils.switchNetwork(true)
        // }
    }

    fun disconnectWebSocket() {
        WebSocketProxy.getInstance().stopWebSocket()
    }

    private fun parserSocketMessage(msgJson: String) {
        if (TextUtils.isEmpty(msgJson)) return
        if (SharedManager.is04AutoSync) {
            when (SocketCmdUtils.getCmdResponse(msgJson)) {
                WsCmdConstants.AR_COMMAND_SNAPSHOT -> {
                    autoSaveNewest(false)
                }

                WsCmdConstants.AR_COMMAND_VRECORD -> {
                    try {
                        val data: JSONObject = JSONObject(msgJson).getJSONObject("data")
                        val enable: Boolean = data.getBoolean("enable")
                        if (!enable) {
                            autoSaveNewest(true)
                        }
                    } catch (exception: Exception) {
                        LibraryLogger.e(
                            "BaseApplication",
                            "Unexpected Exception in BaseApplication catch block",
                            exception,
                        )
                    }
                }
            }
        }
    }

    private fun autoSaveNewest(isVideo: Boolean) {
        // TS004Repository functionality removed
        // CoroutineScope(Dispatchers.IO).launch {
        //     val fileList: List<FileBean>? = TS004Repository.getNewestFile(if (isVideo) 1 else 0)
        //     if (!fileList.isNullOrEmpty()) {
        //         val fileBean: FileBean = fileList[0]
        //         val url = "http://192.168.40.1:8080/DCIM/${'$'}{fileBean.name}"
        //         val file = File(FileConfig.ts004GalleryDir, fileBean.name)
        //         TS004Repository.download(url, file)
        //         MediaScannerConnection.scanFile(
        //             this@BaseApplication,
        //             arrayOf(FileConfig.ts004GalleryDir),
        //             null,
        //             null
        //         )
        //     }
        // }
    }

    private inner class NetworkChangedReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            if (intent?.action == "android.net.conn.CONNECTIVITY_CHANGE") {
                val manager =
                    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = manager.activeNetwork
                    val capabilities = manager.getNetworkCapabilities(activeNetwork)
                    if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    ) {
                        connectWebSocket()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val activeNetwork = manager.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    if (activeNetwork?.isConnected == true && activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                        connectWebSocket()
                    }
                }
            }
        }
    }

    @RequiresApi(api = 28)
    open fun webviewSetPath(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(context)
            if (!applicationContext.packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName!!)
            }
        }
    }

    open fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager: ActivityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    fun clearDb() {
        applicationScope.launch {
            try {
                AppDatabase.getInstance().thermalDao().deleteZero(SharedManager.getUserId())
            } catch (e: Exception) {
            }
        }
    }

    @Suppress("DEPRECATION")
    open fun onLanguageChange() {
        // Force English locale for the application.
        // Note: updateConfiguration() is deprecated but remains the only way to change
        // app-wide configuration at runtime. Use attachBaseContext() for proper
        // locale setting during Activity/Application initialization.
        val config = resources.configuration
        config.setLocale(Locale.ENGLISH)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(Locale.ENGLISH))
        }
        resources.updateConfiguration(config, resources.displayMetrics)
        SharedManager.setLanguage(baseContext, ConstantLanguages.ENGLISH)
        WebView(this).destroy()
    }

    open fun getAppLanguage(context: Context): String? = ConstantLanguages.ENGLISH

    fun exitAll() {
        hasOtgShow = false
        activitys.forEach {
            it.finish()
        }
    }
}
