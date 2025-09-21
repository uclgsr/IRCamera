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
import com.mpdc4gsr.lib.core.bean.event.SocketStateEvent
import com.mpdc4gsr.lib.core.security.CertificateManager
import com.mpdc4gsr.lib.core.utils.WifiUtil
import com.mpdc4gsr.lib.core.utils.WsCmdConstants
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
                XLog.tag("WebSocket").w("$ssid startWebSocket() [ph][ph][ph][ph]")
                return
            }
            this.network = network
        } else {
            XLog.tag("WebSocket").d("[ph][ph][ph] $currentSSID [ph][ph][ph] $ssid，[ph][ph][ph][ph][ph]")
            if (reconnectHandler.isReconnecting) {
                EventBus.getDefault()
                    .post(SocketStateEvent(false, false)) // TS004 functionality removed
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
        // TS004 functionality removed
        return false
    }

    fun isTC007Connect(): Boolean {
        // TC007 functionality removed
        return false
    }

    fun sendMessage(cmd: String?) {
        mWsManager?.sendMessage(cmd)
    }

    private fun getWebSocketUrl(ssid: String): String {
        // TS004/TC007 functionality removed
        throw UnsupportedOperationException("TS004/TC007 device support removed")
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
            XLog.tag("WebSocket").d("$ssid Socket [ph][ph][ph][ph]")
            isNeedReconnect = true
            handler.reset()
            EventBus.getDefault()
                .post(SocketStateEvent(true, false)) // TS004 functionality removed
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String,
        ) {
            if (SocketCmdUtil.getCmdResponse(text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {
                Log.v("WebSocket", "<-- [ph][ph][ph][ph][ph][ph] ${text.replace("\n", "").replace(" ", "")}")
            } else {
                XLog.tag("WebSocket").d("$ssid [ph][ph]TEXT[ph][ph]:$text")
            }
            onMessageListener?.invoke(text)
        }

        private var needPrint = false

        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString,
        ) {
            XLog.tag("WebSocket").w("[ph][ph][ph][ph][ph][ph][ph]，[ph][ph][ph] bytes [ph][ph]，[ph][ph] ${bytes.size}")
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph]，[ph][ph]：$reason")
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            if (handler.isReconnecting) {
                XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph]，[ph][ph][ph][ph][ph][ph]，[ph][ph]：$reason")
            } else {
                XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph]，[ph][ph]：$reason")
                handler.reset()
                EventBus.getDefault()
                    .post(SocketStateEvent(false, false)) // TS004 functionality removed
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?,
        ) {
            XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph][ph][ph]，response: ${response?.message}")
            XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph][ph][ph]，[ph][ph][ph][ph]: ${t.message}")
            if (checkNeedReconnect()) {
                handler.handleFail(ssid)
                if (!handler.isReconnecting) {
                    EventBus.getDefault().post(
                        SocketStateEvent(
                            false,
                            false // TS004 functionality removed
                        )
                    )
                }
            } else {
                XLog.tag("WebSocket").w("[ph][ph][ph][ph][ph][ph]")
                handler.reset()
                getInstance().stopWebSocket()
                EventBus.getDefault()
                    .post(SocketStateEvent(false, false)) // TS004 functionality removed
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onHeartBeat(): String? =
            SocketCmdUtil.getSocketCmd(WsCmdConstants.APP_EVENT_HEART_BEATS)

        override fun onHeartBeatTimeout() {
            XLog.tag("WebSocket").w("[ph][ph][ph][ph]")
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
            XLog.tag("WebSocket").i("[ph][ph][ph][ph][ph]，[ph][ph][ph][ph] WIFI：$wifiName")
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
                    .w("[ph][ph][ph][ph][ph] ${this.currentSSID} [ph]，[ph][ph] $currentSSID fail [ph][ph]")
                return
            }
            if (isReconnecting) {
                reconnectCount++
                if (reconnectCount < MAX_RECONNECT_COUNT) {
                    XLog.tag("WebSocket").w("[ph] $reconnectCount [ph][ph][ph][ph][ph]")

                    getInstance().stopWebSocket()
                    removeCallbacksAndMessages(null)
                    postDelayed(RECONNECT_MILLIS) {
                        getInstance().startWebSocket(currentSSID)
                    }
                } else {
                    XLog.tag("WebSocket").w("[ph][ph][ph][ph][ph][ph][ph][ph]，[ph][ph] [ph][ph][ph][ph][ph] [ph][ph]")
                    reconnectCount = 0
                    isReconnecting = false
                    removeCallbacksAndMessages(null)
                    getInstance().stopWebSocket()
                }
            } else {
                XLog.tag("WebSocket").d("[ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]，[ph][ph][ph][ph][ph][ph][ph][ph]")
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
