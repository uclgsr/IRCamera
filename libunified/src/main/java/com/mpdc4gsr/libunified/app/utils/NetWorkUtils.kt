@file:Suppress("DEPRECATION")

package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
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

    fun isWifiNameValid(
        context: Context,
        wifiName: String
    ): Boolean {
        return wifiName.isNotEmpty() && wifiName.length <= 32
    }

    fun switchNetwork(enable: Boolean) {
        try {
            if (enable) {
                val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                    
                mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        netWorkListener?.invoke(network)
                    }
                    
                    override fun onLost(network: Network) {
                        super.onLost(network)
                        netWorkListener?.invoke(null)
                    }
                }
                
                connectivityManager.requestNetwork(networkRequest, mNetworkCallback!!)
            } else {
                mNetworkCallback?.let {
                    connectivityManager.unregisterNetworkCallback(it)
                    mNetworkCallback = null
                }
            }
        } catch (e: Exception) {
            XLog.e("NetWorkUtils", "switchNetwork error: ${e.message}")
        }
    }

    fun setNetworkListener(listener: ((network: Network?) -> Unit)?) {
        netWorkListener = listener
    }
}