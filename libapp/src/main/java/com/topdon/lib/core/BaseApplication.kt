package com.topdon.lib.core

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LanguageUtils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.broadcast.DeviceBroadcastReceiver
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.repository.FileBean
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.AppLanguageUtils
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.WifiUtil
import com.topdon.lib.core.utils.WsCmdConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File

abstract class BaseApplication : Application() {
    companion object {
        lateinit var instance: BaseApplication
        val usbObserver by lazy { DeviceBroadcastReceiver() }
    }

    var tau_data_H: ByteArray? = null
    var tau_data_L: ByteArray? = null

    var activitys = arrayListOf<Activity>()
    var hasOtgShow = false // otg提示只出现一次

    /**
     * 获取软件编码.
     */
    abstract fun getSoftWareCode(): String

    /**
     * 是否国内渠道。
     *
     * 国内渠道一些逻辑不同，如国内渠道可以应用内升级，权限申请前有提示弹窗等。
     * 根据 2024/8/27 邮件结论，“热视界和电小搭其实没有形成销售，可以不用维护。”
     * @return true-国内渠道 false-非国内渠道
     */
    abstract fun isDomestic(): Boolean

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webviewSetPath(this)
        }
        onLanguageChange()

        WebSocketProxy.getInstance().onMessageListener = {
            parserSocketMessage(it)
        }
    }

    open fun initWebSocket()  {
        connectWebSocket()
        // 注册网络变更广播 - using modern network callback for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest =
                android.net.NetworkRequest.Builder()
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
                },
            )
        } else {
            // Fallback for older Android versions - use modern Intent filter approach
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
                registerReceiver(NetworkChangedReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }
    }

    private fun connectWebSocket() {
        val ssid = WifiUtil.getCurrentWifiSSID(this) ?: return
        Log.i("WebSocket", "当前连接 Wifi SSID: $ssid")
        if (ssid.startsWith(DeviceConfig.TS004_NAME_START)) {
            SharedManager.hasTS004 = true
            WebSocketProxy.getInstance().startWebSocket(ssid)
        } else if (ssid.startsWith(DeviceConfig.TC007_NAME_START)) {
            SharedManager.hasTC007 = true
            WebSocketProxy.getInstance().startWebSocket(ssid)
        } else
            {
                NetWorkUtils.switchNetwork(true)
            }
    }

    fun disconnectWebSocket() {
        Log.i("WebSocket", "disconnectWebSocket()")
        WebSocketProxy.getInstance().stopWebSocket()
    }

    /**
     * 解析socket消息
     * @param msgJson
     */
    private fun parserSocketMessage(msgJson: String) {
        if (TextUtils.isEmpty(msgJson)) return
        EventBus.getDefault().post(SocketMsgEvent(msgJson))

        if (SharedManager.is04AutoSync) { // 自动保存到手机开启
            when (SocketCmdUtil.getCmdResponse(msgJson)) {
                WsCmdConstants.AR_COMMAND_SNAPSHOT -> { // 拍照事件
                    autoSaveNewest(false)
                }

                WsCmdConstants.AR_COMMAND_VRECORD -> { // 开始或结束录像事件
                    try {
                        val data: JSONObject = JSONObject(msgJson).getJSONObject("data")
                        val enable: Boolean = data.getBoolean("enable")
                        if (!enable) { // 结束才同步
                            autoSaveNewest(true)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun autoSaveNewest(isVideo: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val fileList: List<FileBean>? = TS004Repository.getNewestFile(if (isVideo) 1 else 0)
            if (!fileList.isNullOrEmpty()) {
                val fileBean: FileBean = fileList[0]
                val url = "http://192.168.40.1:8080/DCIM/${fileBean.name}"
                val file = File(FileConfig.ts004GalleryDir, fileBean.name)
                TS004Repository.download(url, file)
                MediaScannerConnection.scanFile(this@BaseApplication, arrayOf(FileConfig.ts004GalleryDir), null, null)
            }
        }
    }

    private inner class NetworkChangedReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            if (intent?.action == "android.net.conn.CONNECTIVITY_CHANGE") {
                val manager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // Use modern API for Android M+ (API 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = manager.activeNetwork
                    val capabilities = manager.getNetworkCapabilities(activeNetwork)
                    if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    ) {
                        connectWebSocket()
                        Log.i("WebSocket", "WiFi network connected: $activeNetwork")
                    }
                } else {
                    // Fallback for API < 23 (Android 6.0)
                    @Suppress("DEPRECATION")
                    val activeNetwork = manager.activeNetworkInfo
                    if (activeNetwork?.isConnected == true && activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                        connectWebSocket()
                        Log.i("WebSocket", "WiFi network connected (legacy): ${activeNetwork.type}")
                    }
                }
            }
        }
    }

    /**
     * 设置webview的android9以上系统的多进程兼容性处理
     */
    @RequiresApi(api = 28)
    open fun webviewSetPath(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(context)
            if (!applicationContext.packageName.equals(processName)) { // 判断不等于默认进程名称
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

    // 清除无用数据
    fun clearDb() {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                AppDatabase.getInstance().thermalDao().deleteZero(SharedManager.getUserId())
            } catch (e: Exception) {
                XLog.e("delete db error: ${e.message}")
            }
        }
    }

    open fun onLanguageChange() {
        // Always set and use English
        val locale = AppLanguageUtils.getLocaleByLanguage(ConstantLanguages.ENGLISH)
        LanguageUtils.applyLanguage(locale)
        SharedManager.setLanguage(baseContext, ConstantLanguages.ENGLISH)
        WebView(this).destroy()
    }

    open fun getAppLanguage(context: Context): String? {
        return ConstantLanguages.ENGLISH
    }

    /**
     * 退出所有
     */
    fun exitAll() {
        hasOtgShow = false
        activitys.forEach {
            it.finish()
        }
    }
}
