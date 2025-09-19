package com.mpdc4gsr.lib.core.socket

import android.Manifest
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.os.postDelayed
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.bean.event.SocketStateEvent
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.security.CertificateManager
import com.topdon.lib.core.utils.WifiUtil
import com.topdon.lib.core.utils.WsCmdConstants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import org.greenrobot.eventbus.EventBus

class WebSocketProxy {
    companion object {

        private const val TS004_URL = "wss://192.168.40.1:888"
        private const val TC007_URL = "wss://192.168.40.1:63206/v1/thermal/temp/template/data"

        private const val TS004_URL_FALLBACK = "ws://192.168.40.1:888"
        private const val TC007_URL_FALLBACK =
            "ws://192.168.40.1:63206/v1/thermal/temp/template/data"

        @JvmStatic
        private var mWebSocketProxy: WebSocketProxy? = null

        fun getInstance(): WebSocketProxy {
            if (mWebSocketProxy == null) {
                synchronized(WebSocketProxy::class) {
                    if (mWebSocketProxy == null) {
                        mWebSocketProxy = WebSocketProxy()
                    }
                }
            }
            return mWebSocketProxy!!
        }
    }

    private var currentSSID: String? = null
    private var mWsManager: WsManager? = null
    private var webSocketListener: MyWebSocketListener? = null
    private var reconnectHandler = ReconnectHandler()
    private var network: Network? = null
    private var certificateManager: CertificateManager? = null
    private var useSecureConnection = true 

    fun initializeSecurity(context: android.content.Context) {
        certificateManager = CertificateManager(context)
        val initialized = certificateManager?.initialize() ?: false
        if (!initialized) {
            XLog.tag("WebSocket")
                .w("Failed to initialize certificate manager, falling back to insecure connections")
            useSecureConnection = false
        } else {
            XLog.tag("WebSocket").i("Certificate manager initialized successfully")
        }
    }

    private fun getOKHttpClient(): OkHttpClient {
        val builder =
            OkHttpClient.Builder()

                .addInterceptor(
                    Interceptor { chain ->
                        val originalRequest = chain.request()
                        val requestBuilder: Request.Builder = originalRequest.newBuilder()

                        certificateManager?.let { certManager ->
                            val authToken = certManager.generateAuthToken()
                            requestBuilder.addHeader("Authorization", "Bearer $authToken")
                        }

                        val compressedRequest: Request = requestBuilder.build()
                        XLog.tag("WebSocket").d("request:$compressedRequest")
                        chain.proceed(compressedRequest)
                    },
                )
                .retryOnConnectionFailure(true)

        if (useSecureConnection && certificateManager != null) {
            try {
                val sslSocketFactory = certificateManager?.createSSLSocketFactory()
                val trustManager = certificateManager?.getTrustManager()
                val hostnameVerifier = certificateManager?.createHostnameVerifier()

                if (sslSocketFactory != null && trustManager != null && hostnameVerifier != null) {
                    builder.sslSocketFactory(sslSocketFactory, trustManager)
                    builder.hostnameVerifier(hostnameVerifier)
                    XLog.tag("WebSocket").d("Configured secure WebSocket connection")
                } else {
                    XLog.tag("WebSocket")
                        .w("SSL configuration incomplete, falling back to insecure connection")
                    useSecureConnection = false
                }
            } catch (e: Exception) {
                XLog.tag("WebSocket")
                    .e("Failed to configure SSL, falling back to insecure connection", e)
                useSecureConnection = false
            }
        }

        network?.socketFactory?.let {
            if (!useSecureConnection) { 
                builder.socketFactory(it)
            }
        }

        return builder.build()
    }

    private var onFrameListener: ((frame: SocketFrameBean) -> Unit)? = null

    fun setOnFrameListener(
        activity: ComponentActivity,
        listener: (frame: SocketFrameBean) -> Unit,
    ) {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    onFrameListener = listener
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    onFrameListener = null
                }
            },
        )
    }

    var onMessageListener: ((text: String) -> Unit)? = null

    fun startWebSocket(
        ssid: String,
        network: Network? = null,
    ) {
        if (ssid == currentSSID) {
            if (mWsManager != null) {
                XLog.tag("WebSocket").w("$ssid startWebSocket() 重复调用")
                return
            }
            this.network = network
        } else {
            XLog.tag("WebSocket").d("设备由 $currentSSID 切换到 $ssid，关闭旧连接")
            if (reconnectHandler.isReconnecting) {
                EventBus.getDefault()
                    .post(SocketStateEvent(false, ssid.startsWith(DeviceConfig.TS004_NAME_START)))
            }
            this.network = network
            currentSSID = ssid
            reconnectHandler.currentSSID = ssid
            stopWebSocket()
        }

        XLog.tag("WebSocket").d("$ssid startWebSocket()")

        if (mWsManager == null) {
            webSocketListener =
                MyWebSocketListener(ssid, reconnectHandler, onMessageListener) {
                    onFrameListener?.invoke(it)
                }
            mWsManager =
                WsManager.Builder()
                    .client(getOKHttpClient())
                    .wsUrl(getWebSocketUrl(ssid))
                    .setWsStatusListener(webSocketListener)
                    .build()
        }
        mWsManager?.startConnect()
    }

    fun stopWebSocket() {
        XLog.tag("WebSocket").d("stopWebSocket()")
        webSocketListener?.isNeedReconnect = false
        webSocketListener = null

        mWsManager?.stopConnect()
        mWsManager = null
    }

    fun isConnected(): Boolean = isTS004Connect() || isTC007Connect()

    fun isTS004Connect(): Boolean {
        return currentSSID?.startsWith(DeviceConfig.TS004_NAME_START) == true && mWsManager?.isConnect() == true
    }

    fun isTC007Connect(): Boolean {
        return currentSSID?.startsWith(DeviceConfig.TC007_NAME_START) == true && mWsManager?.isConnect() == true
    }

    fun sendMessage(cmd: String?) {
        mWsManager?.sendMessage(cmd)
    }

    private fun getWebSocketUrl(ssid: String): String {
        val isTS004 = ssid.startsWith(DeviceConfig.TS004_NAME_START)

        return if (useSecureConnection) {

            if (isTS004) TS004_URL else TC007_URL
        } else {

            XLog.tag("WebSocket").w("Using insecure WebSocket connection for $ssid")
            if (isTS004) TS004_URL_FALLBACK else TC007_URL_FALLBACK
        }
    }

    private class MyWebSocketListener(
        val ssid: String,
        val handler: ReconnectHandler,
        val onMessageListener: ((text: String) -> Unit)?,
        val onFrameListener: (frame: SocketFrameBean) -> Unit,
    ) : WsManager.IWebSocketListener() {

        var isNeedReconnect = true

        override fun onOpen(
            webSocket: WebSocket,
            response: Response,
        ) {
            XLog.tag("WebSocket").d("$ssid Socket 连接成功")
            isNeedReconnect = true
            handler.reset()
            EventBus.getDefault()
                .post(SocketStateEvent(true, ssid.startsWith(DeviceConfig.TS004_NAME_START)))
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String,
        ) {
            if (SocketCmdUtil.getCmdResponse(text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {
                Log.v("WebSocket", "<-- 收到心跳消息 ${text.replace("\n", "").replace(" ", "")}")
            } else {
                XLog.tag("WebSocket").d("$ssid 收到TEXT消息:$text")
            }
            onMessageListener?.invoke(text)
        }

        private var needPrint = false

        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString,
        ) {
            if (ssid.startsWith(DeviceConfig.TC007_NAME_START) && bytes.size == 254) {
                val frameBean = SocketFrameBean(bytes.toByteArray())
                onFrameListener.invoke(frameBean)
                needPrint = !needPrint
                if (needPrint) {
                    Log.v("WebSocket", "--------- $ssid 打印一帧数据 ---------")
                    Log.v("WebSocket", frameBean.toString())
                }
            } else {
                XLog.tag("WebSocket").w("$ssid 未知的 bytes 消息，长度 ${bytes.size}")
            }
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            XLog.tag("WebSocket").d("$ssid 连接关闭中，原因：$reason")
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            if (handler.isReconnecting) {
                XLog.tag("WebSocket").d("$ssid 重连过程中，旧连接已关闭，原因：$reason")
            } else {
                XLog.tag("WebSocket").d("$ssid 连接已关闭，原因：$reason")
                handler.reset()
                EventBus.getDefault()
                    .post(SocketStateEvent(false, ssid.startsWith(DeviceConfig.TS004_NAME_START)))
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?,
        ) {
            XLog.tag("WebSocket").d("$ssid 发送或接收失败，response: ${response?.message}")
            XLog.tag("WebSocket").d("$ssid 发送或接收失败，异常原因: ${t.message}")
            if (checkNeedReconnect()) {
                handler.handleFail(ssid)
                if (!handler.isReconnecting) {
                    EventBus.getDefault().post(
                        SocketStateEvent(
                            false,
                            ssid.startsWith(DeviceConfig.TS004_NAME_START)
                        )
                    )
                }
            } else {
                XLog.tag("WebSocket").w("主动断开连接")
                handler.reset()
                getInstance().stopWebSocket()
                EventBus.getDefault()
                    .post(SocketStateEvent(false, ssid.startsWith(DeviceConfig.TS004_NAME_START)))
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onHeartBeat(): String? =
            SocketCmdUtil.getSocketCmd(WsCmdConstants.APP_EVENT_HEART_BEATS)

        override fun onHeartBeatTimeout() {
            XLog.tag("WebSocket").w("心跳超时")
            handler.handleFail(ssid)
        }

        private fun checkNeedReconnect(): Boolean {
            if (!isNeedReconnect) {
                return false
            }
            if (!XXPermissions.isGranted(
                    Utils.getApp(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                return true
            }
            val wifiName: String = WifiUtil.getCurrentWifiSSID(Utils.getApp()) ?: return true
            XLog.tag("WebSocket").i("执行重连前，当前连接 WIFI：$wifiName")
            return wifiName == ssid
        }
    }

    private class ReconnectHandler : Handler(Looper.getMainLooper()) {
        companion object {

            private const val MAX_RECONNECT_COUNT = 3

            private const val RECONNECT_MILLIS = 3000L
        }

        var currentSSID: String = ""
            set(value) {
                if (value != field) {
                    field = value
                    reset()
                }
            }

        var reconnectCount: Int = 0
        var isReconnecting: Boolean = false

        fun reset() {
            reconnectCount = 0
            isReconnecting = false
            removeCallbacksAndMessages(null)
        }

        fun handleFail(currentSSID: String) {
            if (this.currentSSID != currentSSID) {
                XLog.tag("WebSocket")
                    .w("设备切换到 ${this.currentSSID} 后，丢弃 $currentSSID fail 处理")
                return
            }
            if (isReconnecting) {
                reconnectCount++
                if (reconnectCount < MAX_RECONNECT_COUNT) {
                    XLog.tag("WebSocket").w("第 $reconnectCount 次重连失败")

                    getInstance().stopWebSocket()
                    removeCallbacksAndMessages(null)
                    postDelayed(RECONNECT_MILLIS) {
                        getInstance().startWebSocket(currentSSID)
                    }
                } else {
                    XLog.tag("WebSocket").w("最后一次重连失败，发送 连接已断开 事件")
                    reconnectCount = 0
                    isReconnecting = false
                    removeCallbacksAndMessages(null)
                    getInstance().stopWebSocket()
                }
            } else {
                XLog.tag("WebSocket").d("出现心跳超时或错误后，准备开始执行重连")
                reconnectCount = 0
                isReconnecting = true

                getInstance().stopWebSocket()
                removeCallbacksAndMessages(null)
                postDelayed(RECONNECT_MILLIS) {
                    getInstance().startWebSocket(currentSSID)
                }
            }
        }
    }
}
