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
import android.util.Log
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication

/**
 * des:
 * author: CaiSongL
 * date: 2024/3/5 9:07
 **/
object NetWorkUtils {

    private var mNetworkCallback: ConnectivityManager.NetworkCallback ?= null
    private var netWorkListener : ((network: Network?) -> Unit) ?= null
    val connectivityManager by lazy {
        BaseApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val wifiManager by lazy {
        BaseApplication.instance.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun isWifiNameValid(context: Context, prefixes: List<String>): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid.replace("\"", "") // 移除双引号
        for (prefix in prefixes) {
            if (ssid.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    fun connectWifi(ssid: String, password: String, listener: ((network: Network?) -> Unit)? = null) {
        netWorkListener = listener
        if (Build.VERSION.SDK_INT < 29) {//低于 Android10
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            //添加网络配置
            addNetworkConfigs(ssid, password)
            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    netWorkListener?.invoke(network)
                    Log.i("NetWorkUtils", "onAvailable: 网络连接成功")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    connectivityManager.bindProcessToNetwork(null)
                    Log.i("NetWorkUtils", "onLost: 网络连接断开")
                    netWorkListener?.invoke(null)
                }
            }
            connectivityManager.requestNetwork(request, mNetworkCallback!!)
        } else {//高于 Android10 使用此方式连接网络
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password) //也可以使用 setWpa3Passphrase
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()
            mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.i("NetWorkUtils", "onAvailable: 网络连接成功")
                    netWorkListener?.invoke(network)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    connectivityManager.bindProcessToNetwork(null)
                    Log.i("NetWorkUtils", "onLost: 网络连接断开")
                    netWorkListener?.invoke(null)
                }
            }
            connectivityManager.requestNetwork(request, mNetworkCallback!!)
        }
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

    private fun addNetworkConfigs(networkSSID: String, networkPass: String) {
        val conf = WifiConfiguration()
        conf.SSID = "\"" + networkSSID + "\""
        conf.preSharedKey = "\"" + networkPass + "\""
        val netId = wifiManager.addNetwork(conf)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
    }
}