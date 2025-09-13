package com.topdon.lib.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hjq.permissions.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 *
 * Created by LCG on 2024/6/27.
 */
object LocationUtil {
    /**
     * Get/Retrieve最后一个位置info，并反向地理infoencoding为 省市区.
     * @return 省-市-区，若Get/Retrievefailed或无可知位置info则为 null
     */
    @RequiresPermission(Permission.ACCESS_FINE_LOCATION)
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
                @Suppress("DEPRECATION")
                val resultList = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                if (resultList.isNullOrEmpty()) {
                    return@withContext null
                }
                val address = resultList[0]
                return@withContext (address.adminArea ?: "") + (address.locality ?: "") + (address.subLocality ?: "") // 省-市-区
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    /**
     * 在给定 activity 生命周期内add 位置info 开关stateListener.
     */
    fun addBtStateListener(
        activity: ComponentActivity,
        listener: ((isEnable: Boolean) -> Unit),
    ) {
        if (Build.VERSION.SDK_INT >= 28) { // Android 9及以上version才有位置info开关
            activity.lifecycle.addObserver(ModeChangeObserver(activity, listener))
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private class ModeChangeObserver(val context: Context, val listener: ((isEnable: Boolean) -> Unit)) : DefaultLifecycleObserver {
        private val receiver = ModeChangeReceiver()
        private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
