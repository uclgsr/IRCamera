package com.mpdc4gsr.libunified.app.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object WifiUtils {
    @Suppress("DEPRECATION")
    fun ScanResult.getWifiName(): String = if (Build.VERSION.SDK_INT < 33) SSID else removeQuotation(wifiSsid.toString())

    fun WifiInfo.getWifiName(): String = removeQuotation(ssid)

    private fun removeQuotation(source: String): String =
        if (source.length > 1 && source[0] == '\"' && source[source.length - 1] == '\"') {
            source.subSequence(1, source.length - 1).toString()
        } else {
            source
        }

    fun getCurrentWifiSSID(context: Context): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        return wifiManager.connectionInfo?.getWifiName()
    }

    fun addWifiStateListener(
        activity: ComponentActivity,
        listener: ((isEnable: Boolean) -> Unit),
    ) {
        activity.lifecycle.addObserver(WifiStateObserver(activity, WifiStateReceiver(listener)))
    }

    fun addWifiScanListener(
        activity: ComponentActivity,
        listener: ((isSuccess: Boolean) -> Unit),
    ) {
        activity.lifecycle.addObserver(WifiScanObserver(activity, WifiScanReceiver(listener)))
    }

    private class WifiStateObserver(
        val context: Context,
        val receiver: BroadcastReceiver,
    ) : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            context.unregisterReceiver(receiver)
        }
    }

    private class WifiScanObserver(
        val context: Context,
        val receiver: BroadcastReceiver,
    ) : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            context.registerReceiver(
                receiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            )
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            context.unregisterReceiver(receiver)
        }
    }

    private class WifiStateReceiver(
        val listener: (isEnable: Boolean) -> Unit,
    ) : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val wifiState =
                    intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                listener(wifiState == WifiManager.WIFI_STATE_ENABLED)
            }
        }
    }

    private class WifiScanReceiver(
        val listener: (isSuccess: Boolean) -> Unit,
    ) : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                listener(success)
            }
        }
    }
}
