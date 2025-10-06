@file:Suppress("DEPRECATION")
package com.mpdc4gsr.libunified.app.utils
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
object NetWorkUtils {
    private var mNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var netWorkListener: ((network: Network?) -> Unit)? = null
    val connectivityManager by lazy {
        BaseApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val wifiManager by lazy {
        BaseApplication.instance.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    fun isWifiNameValid(context: Context, prefixes: List<String>): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid.replace("\"", "") // 
        for (prefix in prefixes) {
            if (ssid.startsWith(prefix)) {
                return true
            }
        }
        return false
    }
    fun connectWifi(
        ssid: String,
        password: String,
        listener: ((network: Network?) -> Unit)? = null
    ) {
        netWorkListener = listener
        if (Build.VERSION.SDK_INT < 29) {// Android10
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    netWorkListener?.invoke(network)
                }
                override fun onUnavailable() {
                    super.onUnavailable()
                    netWorkListener?.invoke(null)
                }
            }
            connectivityManager.requestNetwork(request, mNetworkCallback!!)
        } else {
            // Android 10+ approach
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()
            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    netWorkListener?.invoke(network)
                }
                override fun onUnavailable() {
                    super.onUnavailable()
                    netWorkListener?.invoke(null)
                }
            }
            connectivityManager.requestNetwork(request, mNetworkCallback!!)
        }
    }
    fun switchNetwork(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, network switching is handled differently
            XLog.d("NetWorkUtils: switchNetwork called with enable=$enable")
        }
    }
    fun disconnectWifi() {
        mNetworkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            mNetworkCallback = null
        }
        netWorkListener = null
    }
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }
}