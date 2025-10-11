package com.mpdc4gsr.component.shared.app.tools

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea
import java.util.ArrayDeque

/**
 * Utility entry point for registering lifecycle-aware permission request controllers.
 *
 * Usage:
 * ```
 * class MyActivity : ComponentActivity() {
 *     private val permissionController = PermissionTools.controller(this)
 *
 *     fun ensureCamera() {
 *         permissionController.requestFeature(FeaturePermissionArea.RGB_VIDEO) {
 *             startCamera()
 *         }
 *     }
 * }
 * ```
 */
object PermissionTools {

    /**
     * Create a [Controller] tied to the supplied [ComponentActivity].
     * Call once (typically in `onCreate`) and reuse for subsequent requests.
     */
    fun controller(activity: ComponentActivity): Controller = Controller(activity)

    /**
     * Returns true if every runtime permission associated with [feature] is currently granted.
     */
    fun hasPermissions(
        context: Context,
        feature: FeaturePermissionArea,
    ): Boolean = missingPermissions(context, listOf(feature)).isEmpty()

    /**
     * Returns true if all permissions for the provided [features] are granted.
     */
    fun hasPermissions(
        context: Context,
        features: Collection<FeaturePermissionArea>,
    ): Boolean = missingPermissions(context, features).isEmpty()

    /**
     * Convenience helper for Bluetooth/GSR permissions.
     */
    fun hasBtPermission(context: Context): Boolean = hasPermissions(context, FeaturePermissionArea.GSR_SENSORS)

    /**
     * Computes the list of runtime permission strings that are not granted for [features].
     */
    fun missingPermissions(
        context: Context,
        features: Collection<FeaturePermissionArea>,
    ): List<String> =
        features
            .flatMap { it.allPermissions() }
            .distinct()
            .filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }

    class Controller internal constructor(
        private val activity: ComponentActivity,
    ) {
        private val pendingRequests = ArrayDeque<PermissionRequest>()
        private val launcher: ActivityResultLauncher<Array<String>> =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                val request =
                    if (pendingRequests.isEmpty()) {
                        null
                    } else {
                        pendingRequests.removeFirst()
                    } ?: return@registerForActivityResult
                val denied = result.filterValues { granted -> !granted }.keys.toList()
                if (denied.isEmpty()) {
                    request.onGranted()
                } else {
                    request.onDenied(denied)
                }
            }

        /**
         * Request all runtime permissions required for [feature].
         */
        fun requestFeature(
            feature: FeaturePermissionArea,
            onGranted: () -> Unit,
            onDenied: (List<String>) -> Unit = {},
        ) = requestFeatures(listOf(feature), onGranted, onDenied)

        /**
         * Request all runtime permissions required for [features].
         *
         * If every permission is already granted, [onGranted] executes immediately. Otherwise,
         * [onDenied] receives the list of permission strings that remain denied after the prompt.
         */
        fun requestFeatures(
            features: Collection<FeaturePermissionArea>,
            onGranted: () -> Unit,
            onDenied: (List<String>) -> Unit = {},
        ) {
            if (features.isEmpty()) {
                onGranted()
                return
            }
            val missing = missingPermissions(activity, features)
            if (missing.isEmpty()) {
                onGranted()
                return
            }
            pendingRequests += PermissionRequest(onGranted = onGranted, onDenied = onDenied)
            launcher.launch(missing.toTypedArray())
        }
    }

    private data class PermissionRequest(
        val onGranted: () -> Unit,
        val onDenied: (List<String>) -> Unit,
    )
}
