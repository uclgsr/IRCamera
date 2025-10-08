// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\navigation' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\navigation\libunified_src_main_java_com_mpdc4gsr_libunified_app_navigation_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\navigation' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\navigation\NavigationManager.kt =====

package com.mpdc4gsr.libunified.app.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.tools.DeviceTools

object NavigationManager {
    private val routeToClassMap = mapOf(
        RouterConfig.MAIN to "mpdc4gsr.feature.main.ui.MainComposeActivity",
        RouterConfig.CLAUSE to "mpdc4gsr.activities.ClauseActivity",
        RouterConfig.POLICY to "mpdc4gsr.activities.PolicyComposeActivity",
        RouterConfig.VERSION to "mpdc4gsr.activities.VersionComposeActivity",
        RouterConfig.IR_GALLERY_EDIT to "mpdc4gsr.activities.IRGalleryEditActivity",
        RouterConfig.WEB_VIEW to "mpdc4gsr.activities.WebViewActivity",
        RouterConfig.IR_MAIN to "com.mpdc4gsr.module.thermalunified.activity.IRMainComposeActivity",
        RouterConfig.IR_SETTING to "com.mpdc4gsr.module.thermalunified.activity.IRConfigComposeActivity",
        RouterConfig.IR_THERMAL_MONITOR to "com.mpdc4gsr.module.thermalunified.activity.IRMonitorComposeActivity",
        RouterConfig.IR_MONITOR_CHART to "com.mpdc4gsr.module.thermalunified.activity.IRMonitorChartComposeActivity",
        RouterConfig.IR_GALLERY_DETAIL_01 to "com.mpdc4gsr.module.thermalunified.activity.IRGalleryDetail01ComposeActivity",
        RouterConfig.IR_GALLERY_DETAIL_04 to "com.mpdc4gsr.module.thermalunified.activity.IRGalleryDetail04ComposeActivity",
        RouterConfig.IR_VIDEO_GSY to "com.mpdc4gsr.module.thermalunified.activity.IRVideoGSYComposeActivity",
        RouterConfig.IR_CORRECTION_TWO to "com.mpdc4gsr.module.thermalunified.activity.IRCorrectionTwoComposeActivity",
        RouterConfig.IR_CORRECTION_THREE to "com.mpdc4gsr.module.thermalunified.activity.IRCorrectionThreeComposeActivity",
        RouterConfig.IR_CORRECTION_FOUR to "com.mpdc4gsr.module.thermalunified.activity.IRCorrectionFourComposeActivity",
        RouterConfig.IR_IMG_PICK to "com.mpdc4gsr.module.thermalunified.activity.ImagePickIRComposeActivity",
        RouterConfig.IR_IMG_PICK_PLUS to "com.mpdc4gsr.module.thermalunified.activity.ImagePickIRPlushComposeActivity",
        RouterConfig.GALLERY to "com.mpdc4gsr.module.thermalunified.activity.GalleryComposeActivity",
        RouterConfig.THERMAL_MONITOR to "com.mpdc4gsr.module.thermalunified.activity.MonitorComposeActivity",
        RouterConfig.CONNECT to "com.mpdc4gsr.module.thermalunified.activity.ConnectComposeActivity",
        RouterConfig.VIDEO to "com.mpdc4gsr.module.thermalunified.activity.VideoComposeActivity",
        RouterConfig.MONITOR_CHART to "com.mpdc4gsr.module.thermalunified.activity.MonitorChartComposeActivity",
        RouterConfig.LOG_MP_CHART to "com.mpdc4gsr.module.thermalunified.activity.LogMPChartComposeActivity",
        RouterConfig.IR_TCLITE to "com.mpdc4gsr.module.thermalunified.lite.activity.IRThermalLiteComposeActivity",
        RouterConfig.IR_THERMAL_MONITOR_LITE to "com.mpdc4gsr.module.thermalunified.activity.IRMonitorComposeActivity",
        RouterConfig.IR_IMG_PICK_LITE to "com.mpdc4gsr.module.thermalunified.lite.activity.ImagePickIRLiteComposeActivity",
        RouterConfig.IR_MONITOR_CHART_LITE to "com.mpdc4gsr.module.thermalunified.activity.IRMonitorChartComposeActivity",
        RouterConfig.IR_CORRECTION_THREE_LITE to "com.mpdc4gsr.module.thermalunified.lite.activity.IRCorrectionLiteThreeComposeActivity",
        RouterConfig.IR_CORRECTION_FOUR_LITE to "com.mpdc4gsr.module.thermalunified.lite.activity.IRCorrectionLiteFourComposeActivity",
        RouterConfig.REPORT_CREATE_FIRST to "com.mpdc4gsr.module.thermalunified.report.activity.ReportCreateComposeActivity",
        RouterConfig.REPORT_CREATE_SECOND to "com.mpdc4gsr.module.thermalunified.report.activity.ReportCreateComposeActivity",
        RouterConfig.REPORT_PREVIEW_SECOND to "com.mpdc4gsr.module.thermalunified.activity.ReportPreviewSecondComposeActivity",
        RouterConfig.REPORT_PICK_IMG to "com.mpdc4gsr.module.thermalunified.activity.ReportPickImgComposeActivity",
        RouterConfig.QUESTION to "com.mpdc4gsr.module.user.activity.QuestionComposeActivity",
        RouterConfig.ELECTRONIC_MANUAL to "com.mpdc4gsr.module.user.activity.ElectronicManualComposeActivity",
        RouterConfig.STORAGE_SPACE to "com.mpdc4gsr.module.user.activity.StorageSpaceComposeActivity",
        RouterConfig.TC_MORE to "com.mpdc4gsr.module.user.activity.MoreComposeActivity",
        RouterConfig.GSR_MULTI_MODAL to "mpdc4gsr.gsr.MultiModalRecordingActivity",
        RouterConfig.GSR_DEMO to "com.mpdc4gsr.component.gsr.activity.GSRDemoActivity",
        RouterConfig.IR_GALLERY_HOME to "com.mpdc4gsr.module.thermalunified.activity.IRGalleryHomeComposeActivity",
        RouterConfig.IR_CAMERA_SETTING to "com.mpdc4gsr.module.thermalunified.activity.IRCameraSettingActivity",
        RouterConfig.QUESTION_DETAILS to "com.mpdc4gsr.module.user.activity.QuestionDetailsComposeActivity",
        RouterConfig.PDF to "com.mpdc4gsr.module.user.activity.PDFActivity",
        RouterConfig.DEVICE_INFORMATION to "com.mpdc4gsr.module.user.activity.DeviceDetailsComposeActivity",
        RouterConfig.TISR to "com.mpdc4gsr.module.user.activity.TISRComposeActivity",
        RouterConfig.AUTO_SAVE to "com.mpdc4gsr.module.user.activity.AutoSaveComposeActivity",
        RouterConfig.UNIT to "com.mpdc4gsr.module.user.activity.UnitComposeActivity"
    )

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

        fun withFloat(
            key: String,
            value: Float,
        ) = apply {
            extras.putFloat(key, value)
        }

        fun withLong(
            key: String,
            value: Long,
        ) = apply {
            extras.putLong(key, value)
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

    fun build(route: String): NavigationBuilder {
        return NavigationBuilder(route)
    }

    fun getInstance(): NavigationManager = this
    private fun createIntent(
        context: Context,
        route: String,
    ): Intent {
        val className = routeToClassMap[route]
            ?: throw IllegalArgumentException("Unknown route: $route")
        return Intent(context, getClassByName(className))
    }

    fun jumpImagePick(
        activity: Activity,
        // isTC007 parameter removed - TC007 functionality disabled
        imgPath: String,
    ) {
        val route =
            when {
                // TC007 functionality removed
                DeviceTools.isTC001PlusConnect() -> RouterConfig.IR_IMG_PICK_PLUS
                DeviceTools.isTC001LiteConnect() -> RouterConfig.IR_IMG_PICK_LITE
                DeviceTools.isHikConnect() -> RouterConfig.IR_HIK_IMG_PICK
                else -> RouterConfig.IR_IMG_PICK
            }
        build(route)
            .withString("RESULT_IMAGE_PATH", imgPath)
            .navigation(activity, 101)
    }

    private fun getClassByName(className: String): Class<*> {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Activity class not found: $className", e)
        }
    }
}