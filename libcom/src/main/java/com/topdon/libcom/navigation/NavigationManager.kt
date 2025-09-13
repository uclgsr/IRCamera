package com.topdon.libcom.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.tools.DeviceTools

/**
 * Modern navigation system to replace ARouter
 * Provides type-safe navigation with Intent-based routing
 */
object NavigationManager {
    /**
     * Navigation builder class for fluent API
     */
    class NavigationBuilder(private val route: String) {
        private val extras = Bundle()
        private var requestCode: Int? = null

        fun withString(
            key: String,
            value: String,
        ) = apply {
            extras.putString(key, value)
        }

        fun withBoolean(
            key: String,
            value: Boolean,
        ) = apply {
            extras.putBoolean(key, value)
        }

        fun withInt(
            key: String,
            value: Int,
        ) = apply {
            extras.putInt(key, value)
        }

        fun withParcelable(
            key: String,
            value: Parcelable,
        ) = apply {
            extras.putParcelable(key, value)
        }

        fun withParcelableArrayList(
            key: String,
            value: ArrayList<out Parcelable>,
        ) = apply {
            extras.putParcelableArrayList(key, value)
        }

        fun withExtras(bundle: Bundle) =
            apply {
                extras.putAll(bundle)
            }

        fun navigation(
            context: Context,
            requestCode: Int? = null,
        ) {
            this.requestCode = requestCode
            val intent = createIntent(context, route)
            intent.putExtras(extras)

            if (requestCode != null && context is Activity) {
                context.startActivityForResult(intent, requestCode)
            } else {
                context.startActivity(intent)
            }
        }
    }

    /**
     * Build navigation to a specific route
     */
    fun build(route: String): NavigationBuilder {
        return NavigationBuilder(route)
    }

    /**
     * Get instance for API compatibility with ARouter
     */
    fun getInstance(): NavigationManager = this

    /**
     * Create Intent based on route configuration
     */
    private fun createIntent(
        context: Context,
        route: String,
    ): Intent {
        val activityClass =
            when (route) {
                // App routes
                RouterConfig.MAIN -> getClassByName("com.topdon.tc001.MainActivity")
                RouterConfig.CLAUSE -> getClassByName("com.topdon.tc001.ClauseActivity")
                RouterConfig.POLICY -> getClassByName("com.topdon.tc001.PolicyActivity")
                RouterConfig.VERSION -> getClassByName("com.topdon.tc001.VersionActivity")
                RouterConfig.IR_GALLERY_EDIT -> getClassByName("com.topdon.tc001.IRGalleryEditActivity")
                RouterConfig.WEB_VIEW -> getClassByName("com.topdon.tc001.WebViewActivity")

                // IR routes
                RouterConfig.IR_MAIN -> getClassByName("com.topdon.module.thermal.ir.activity.IRMainActivity")
                RouterConfig.IR_SETTING -> getClassByName("com.topdon.module.thermal.ir.activity.IRConfigActivity")
                RouterConfig.IR_THERMAL_MONITOR -> getClassByName("com.topdon.module.thermal.ir.activity.IRMonitorActivity")
                RouterConfig.IR_MONITOR_CHART -> getClassByName("com.topdon.module.thermal.ir.activity.IRMonitorChartActivity")
                RouterConfig.IR_GALLERY_DETAIL_01 -> getClassByName("com.topdon.module.thermal.ir.activity.IRGalleryDetail01Activity")
                RouterConfig.IR_GALLERY_DETAIL_04 -> getClassByName("com.topdon.module.thermal.ir.activity.IRGalleryDetail04Activity")
                RouterConfig.IR_VIDEO_GSY -> getClassByName("com.topdon.module.thermal.ir.activity.IRVideoGSYActivity")
                RouterConfig.IR_CORRECTION_TWO -> getClassByName("com.topdon.module.thermal.ir.activity.IRCorrectionTwoActivity")
                RouterConfig.IR_CORRECTION_THREE -> getClassByName("com.topdon.module.thermal.ir.activity.IRCorrectionThreeActivity")
                RouterConfig.IR_CORRECTION_FOUR -> getClassByName("com.topdon.module.thermal.ir.activity.IRCorrectionFourActivity")
                RouterConfig.IR_IMG_PICK -> getClassByName("com.topdon.module.thermal.ir.activity.ImagePickIRActivity")
                RouterConfig.IR_IMG_PICK_PLUS -> getClassByName("com.topdon.module.thermal.ir.activity.ImagePickIRPlushActivity")

                // Thermal routes
                RouterConfig.GALLERY -> getClassByName("com.topdon.module.thermal.activity.GalleryActivity")
                RouterConfig.THERMAL_MONITOR -> getClassByName("com.topdon.module.thermal.activity.MonitorActivity")
                RouterConfig.CONNECT -> getClassByName("com.topdon.module.thermal.activity.ConnectActivity")
                RouterConfig.VIDEO -> getClassByName("com.topdon.module.thermal.activity.VideoActivity")
                RouterConfig.MONITOR_CHART -> getClassByName("com.topdon.module.thermal.activity.MonitorChartActivity")
                RouterConfig.LOG_MP_CHART -> getClassByName("com.topdon.module.thermal.activity.LogMpChartActivity")

                // Thermal-lite routes
                RouterConfig.IR_TCLITE -> getClassByName("com.example.thermal_lite.activity.IRThermalLiteActivity")
                RouterConfig.IR_THERMAL_MONITOR_LITE -> getClassByName("com.example.thermal_lite.activity.IRMonitorLiteActivity")
                RouterConfig.IR_IMG_PICK_LITE -> getClassByName("com.example.thermal_lite.activity.ImagePickIRLiteActivity")
                RouterConfig.IR_MONITOR_CHART_LITE -> getClassByName("com.example.thermal_lite.activity.IRMonitorChartLiteActivity")
                RouterConfig.IR_CORRECTION_THREE_LITE -> getClassByName("com.example.thermal_lite.activity.IRCorrectionLiteThreeActivity")
                RouterConfig.IR_CORRECTION_FOUR_LITE -> getClassByName("com.example.thermal_lite.activity.IRCorrectionLiteFourActivity")

                // Report routes
                RouterConfig.REPORT_CREATE_FIRST -> getClassByName("com.topdon.module.thermal.ir.report.activity.ReportCreateFirstActivity")
                RouterConfig.REPORT_CREATE_SECOND ->
                    getClassByName(
                        "com.topdon.module.thermal.ir.report.activity.ReportCreateSecondActivity",
                    )
                RouterConfig.REPORT_PREVIEW_SECOND ->
                    getClassByName(
                        "com.topdon.module.thermal.ir.report.activity.ReportPreviewSecondActivity",
                    )
                RouterConfig.REPORT_PICK_IMG -> getClassByName("com.topdon.module.thermal.ir.activity.ReportPickImgActivity")

                // User routes
                RouterConfig.QUESTION -> getClassByName("com.topdon.module.user.activity.QuestionActivity")
                RouterConfig.ELECTRONIC_MANUAL -> getClassByName("com.topdon.module.user.activity.ElectronicManualActivity")
                RouterConfig.STORAGE_SPACE -> getClassByName("com.topdon.module.user.activity.StorageSpaceActivity")
                RouterConfig.TC_MORE -> getClassByName("com.topdon.module.user.activity.MoreActivity")

                // GSR routes
                RouterConfig.GSR_MULTI_MODAL -> getClassByName("com.topdon.tc001.gsr.MultiModalRecordingActivity")
                RouterConfig.GSR_DEMO -> getClassByName("com.topdon.tc001.gsr.GSRDemoActivity")

                else -> {
                    throw IllegalArgumentException("Unknown route: $route")
                }
            }
        return Intent(context, activityClass)
    }

    /**
     * Device-aware navigation for image picking
     */
    fun jumpImagePick(
        activity: Activity,
        isTC007: Boolean,
        imgPath: String,
    ) {
        val route =
            when {
                isTC007 -> RouterConfig.IR_IMG_PICK_07
                DeviceTools.isTC001PlusConnect() -> RouterConfig.IR_IMG_PICK_PLUS
                DeviceTools.isTC001LiteConnect() -> RouterConfig.IR_IMG_PICK_LITE
                DeviceTools.isHikConnect() -> RouterConfig.IR_HIK_IMG_PICK
                else -> RouterConfig.IR_IMG_PICK
            }

        build(route)
            .withString("RESULT_IMAGE_PATH", imgPath)
            .navigation(activity, 101)
    }

    /**
     * Get class by name with error handling
     */
    private fun getClassByName(className: String): Class<*> {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Activity class not found: $className", e)
        }
    }
}
