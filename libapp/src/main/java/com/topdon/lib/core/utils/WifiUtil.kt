package com.topdon.lib.core.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hjq.permissions.XXPermissions

/**
 * WIFI 相关工具class.
 */
object WifiUtil {
    /**
     * 不带双引号的 SSID.
     */
    fun ScanResult.getWifiName(): String =
        if (Build.VERSION.SDK_INT < 33) {
            @Suppress("DEPRECATION")
            SSID
        } else {
            removeQuotation(wifiSsid.toString())
        }

    /**
     * Executes wifiinfo functionality.
     */
    fun WifiInfo.getWifiName(): String = removeQuotation(ssid)

    /**
     * 如果指定字符串以双引号开头及结尾，则去除开头及结尾的双引号
     */
    private fun removeQuotation(source: String): String {
        return if (source.length > 1 && source[0] == '\"' && source[source.length - 1] == '\"') {
            source.subSequence(1, source.length - 1).toString()
        } else {
            source
        }
    }

    /**
     * Get/Retrievecurrentconnection的 Wifi ssid，如果有的话，移除首尾的双引号。
     * @return 若未connection WIFI 或 无 [Manifest.permission.ACCESS_FINE_LOCATION] Permission，则为 null
     */
    fun getCurrentWifiSSID(context: Context): String? {
        if (!XXPermissions.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return null
        }
        val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        return wifiManager.connectionInfo?.getWifiName()
    }

    /**
     * 在给定 activity 生命周期内add WIFI 开关stateListener.
     */
    fun addWifiStateListener(
        activity: ComponentActivity,
        listener: ((isEnable: Boolean) -> Unit),
    ) {
        activity.lifecycle.addObserver(WifiStateObserver(activity, WifiStateReceiver(listener)))
    }

    /**
     * 在给定 activity 生命周期内add WIFI 扫描结果Listener.
     */
    fun addWifiScanListener(
        activity: ComponentActivity,
        listener: ((isSuccess: Boolean) -> Unit),
    ) {
        activity.lifecycle.addObserver(WifiScanObserver(activity, WifiScanReceiver(listener)))
    }

    private class WifiStateObserver(val context: Context, val receiver: BroadcastReceiver) : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            context.unregisterReceiver(receiver)
            owner.lifecycle.removeObserver(this)
        }
    }

    private class WifiScanObserver(val context: Context, val receiver: BroadcastReceiver) : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            context.unregisterReceiver(receiver)
            owner.lifecycle.removeObserver(this)
        }
    }

    /**
     * WIFI state变更广播Listener.
     */
    private class WifiStateReceiver(val listener: ((isEnable: Boolean) -> Unit)) : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            when (intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {
                WifiManager.WIFI_STATE_ENABLED -> listener.invoke(true)
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_UNKNOWN -> listener.invoke(false)
            }
        }
    }

    /**
     * WIFI 扫描结果广播Listener.
     */
    private class WifiScanReceiver(val listener: ((isSuccess: Boolean) -> Unit)) : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent?,
        ) {
            listener.invoke(intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false)
        }
    }
}
