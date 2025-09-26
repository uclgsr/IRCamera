package com.mpdc4gsr.libunified.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.elvishew.xlog.XLog

/**
 * Location utility class for handling GPS operations
 * Simplified implementation for MVP compatibility
 */
object LocationUtil {

    private const val MIN_TIME_BETWEEN_UPDATES = 1000L // 1 second
    private const val MIN_DISTANCE = 10f // 10 meters

    /**
     * Interface for location updates
     */
    interface LocationCallback {
        fun onLocationReceived(location: Location)
        fun onLocationError(error: String)
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if GPS is enabled
     */
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Get last known location
     */
    fun getLastKnownLocation(context: Context, callback: LocationCallback) {
        if (!hasLocationPermissions(context)) {
            callback.onLocationError("Location permissions not granted")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val bestLocation = when {
                lastKnownGps != null && lastKnownNetwork != null -> {
                    if (lastKnownGps.time > lastKnownNetwork.time) lastKnownGps else lastKnownNetwork
                }
                lastKnownGps != null -> lastKnownGps
                lastKnownNetwork != null -> lastKnownNetwork
                else -> null
            }

            if (bestLocation != null) {
                callback.onLocationReceived(bestLocation)
            } else {
                callback.onLocationError("No last known location available")
            }
        } catch (e: SecurityException) {
            callback.onLocationError("Security exception: ${e.message}")
        } catch (e: Exception) {
            callback.onLocationError("Error getting location: ${e.message}")
        }
    }

    /**
     * Start location updates
     */
    fun startLocationUpdates(
        context: Context,
        callback: LocationCallback
    ): LocationListener? {
        if (!hasLocationPermissions(context)) {
            callback.onLocationError("Location permissions not granted")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                callback.onLocationReceived(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                XLog.d("Location provider status changed: $provider, status: $status")
            }

            override fun onProviderEnabled(provider: String) {
                XLog.d("Location provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                XLog.d("Location provider disabled: $provider")
                if (provider == LocationManager.GPS_PROVIDER) {
                    callback.onLocationError("GPS provider disabled")
                }
            }
        }

        try {
            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE,
                    locationListener
                )
            }
            
            // Also use network provider as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE,
                    locationListener
                )
            }
            
            return locationListener
        } catch (e: SecurityException) {
            callback.onLocationError("Security exception: ${e.message}")
            return null
        } catch (e: Exception) {
            callback.onLocationError("Error starting location updates: ${e.message}")
            return null
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates(context: Context, locationListener: LocationListener) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            XLog.e("Error stopping location updates: ${e.message}")
        }
    }

    /**
     * Get location string for display
     */
    fun formatLocation(location: Location): String {
        return "Lat: ${String.format("%.6f", location.latitude)}, " +
                "Lng: ${String.format("%.6f", location.longitude)}"
    }

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Get location accuracy description
     */
    fun getAccuracyDescription(accuracy: Float): String {
        return when {
            accuracy < 5 -> "Excellent"
            accuracy < 10 -> "Good"
            accuracy < 20 -> "Fair"
            else -> "Poor"
        }
    }

    /**
     * Check if location is valid
     */
    fun isValidLocation(location: Location?): Boolean {
        if (location == null) return false
        
        val lat = location.latitude
        val lng = location.longitude
        
        return lat != 0.0 && lng != 0.0 && 
               lat >= -90 && lat <= 90 && 
               lng >= -180 && lng <= 180
    }

    /**
     * Simple compatibility wrapper for RxAppCompatActivity usage
     * Returns a simplified activity type check
     */
    fun isCompatibleActivity(activity: Any?): Boolean {
        return activity is FragmentActivity
    }
}