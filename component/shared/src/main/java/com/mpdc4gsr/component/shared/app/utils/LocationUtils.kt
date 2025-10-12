package com.mpdc4gsr.component.shared.app.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

object LocationUtils {
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getLastLocationStr(context: Context): String? =
        withContext(Dispatchers.IO) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null) {
                return@withContext null
            }
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val resultList =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Use the new API for Android 13+
                        suspendCancellableCoroutine<List<Address>?> { continuation ->
                            geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1,
                            ) { addresses ->
                                continuation.resume(addresses)
                            }
                        }
                    } else {
                        // Use the deprecated API for older versions
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                        )
                    }
                if (resultList.isNullOrEmpty()) {
                    return@withContext null
                }
                val address = resultList[0]
                return@withContext (address.adminArea ?: "") + (
                    address.locality
                        ?: ""
                    ) + (address.subLocality ?: "") // --
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    fun addBtStateListener(
        activity: ComponentActivity,
        listener: ((isEnable: Boolean) -> Unit),
    ) {
        if (Build.VERSION.SDK_INT >= 28) { // Android 9
            activity.lifecycle.addObserver(ModeChangeObserver(activity, listener))
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private class ModeChangeObserver(
        val context: Context,
        val listener: ((isEnable: Boolean) -> Unit),
    ) : DefaultLifecycleObserver {
        private val receiver = ModeChangeReceiver()
        private val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        override fun onCreate(owner: LifecycleOwner) {
            context.registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            context.unregisterReceiver(receiver)
            owner.lifecycle.removeObserver(this)
        }

        private inner class ModeChangeReceiver : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                listener.invoke(locationManager.isLocationEnabled)
            }
        }
    }
}


