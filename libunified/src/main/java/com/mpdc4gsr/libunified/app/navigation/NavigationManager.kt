package com.mpdc4gsr.libunified.app.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.tools.DeviceTools

/**
 * Legacy Navigation Manager using string-based routing and reflection.
 * 
 * WARNING: This class uses Class.forName() for dynamic class loading which is an anti-pattern.
 * Reasons for current implementation:
 * - Module separation: libunified cannot directly reference app module classes
 * - String-based routing allows decoupling between modules
 * 
 * RECOMMENDED: Use UnifiedNavigation with Compose Navigation instead.
 * See: app/src/main/java/mpdc4gsr/compose/navigation/UnifiedNavigation.kt
 * 
 * TODO: Refactor to use a proper dependency injection or service locator pattern
 * that doesn't require reflection. Consider:
 * 1. Navigation component with deep links
 * 2. Router interface with module-specific implementations
 * 3. Centralized activity registry without reflection
 * 
 * @deprecated Use UnifiedNavigation with Compose Navigation for new code
 */
@Deprecated(
    message = "Use UnifiedNavigation with Compose Navigation instead",
    replaceWith = ReplaceWith("UnifiedNavHost()", "mpdc4gsr.compose.navigation.UnifiedNavHost")
)
object NavigationManager {


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
        val activityClass =
            when (route) {

                RouterConfig.MAIN -> getClassByName("mpdc4gsr.activities.MainActivity")
                RouterConfig.CLAUSE -> getClassByName("mpdc4gsr.activities.ClauseActivity")
                RouterConfig.POLICY -> getClassByName("mpdc4gsr.activities.PolicyActivityCompose")
                RouterConfig.VERSION -> getClassByName("mpdc4gsr.activities.VersionActivityCompose")
                RouterConfig.IR_GALLERY_EDIT -> getClassByName("mpdc4gsr.activities.IRGalleryEditActivity")
                RouterConfig.WEB_VIEW -> getClassByName("mpdc4gsr.activities.WebViewActivity")

                RouterConfig.IR_MAIN -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRMainComposeActivity")
                RouterConfig.IR_SETTING -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRConfigComposeActivity")
                RouterConfig.IR_THERMAL_MONITOR -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRMonitorComposeActivity")
                RouterConfig.IR_MONITOR_CHART -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRMonitorChartComposeActivity")
                RouterConfig.IR_GALLERY_DETAIL_01 -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRGalleryDetail01ComposeActivity")
                RouterConfig.IR_GALLERY_DETAIL_04 -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRGalleryDetail04ComposeActivity")
                RouterConfig.IR_VIDEO_GSY -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRVideoGSYComposeActivity")
                RouterConfig.IR_CORRECTION_TWO -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRCorrectionTwoComposeActivity")
                RouterConfig.IR_CORRECTION_THREE -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRCorrectionThreeComposeActivity")
                RouterConfig.IR_CORRECTION_FOUR -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRCorrectionFourComposeActivity")
                RouterConfig.IR_IMG_PICK -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.ImagePickIRComposeActivity")
                RouterConfig.IR_IMG_PICK_PLUS -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.ImagePickIRPlushComposeActivity")

                // RouterConfig.IR_MONOCULAR -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRMonocularActivity")
                // RouterConfig.IR_DEVICE_ADD -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.DeviceAddActivity")

                RouterConfig.GALLERY -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.GalleryComposeActivity")
                RouterConfig.THERMAL_MONITOR -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.MonitorComposeActivity")
                RouterConfig.CONNECT -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.ConnectComposeActivity")
                RouterConfig.VIDEO -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.VideoComposeActivity")
                RouterConfig.MONITOR_CHART -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.MonitorChartComposeActivity")
                RouterConfig.LOG_MP_CHART -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.LogMPChartComposeActivity")

                RouterConfig.IR_TCLITE -> getClassByName("com.mpdc4gsr.module.thermalunified.lite.activity.IRThermalLiteComposeActivity")
                RouterConfig.IR_THERMAL_MONITOR_LITE -> getClassByName("com.mpdc4gsr.module.thermalunified.lite.activity.IRMonitorLiteComposeActivity")
                RouterConfig.IR_IMG_PICK_LITE -> getClassByName("com.mpdc4gsr.module.thermalunified.lite.activity.ImagePickIRLiteComposeActivity")
                RouterConfig.IR_MONITOR_CHART_LITE -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRMonitorChartComposeActivity")
                RouterConfig.IR_CORRECTION_THREE_LITE -> getClassByName("com.mpdc4gsr.module.thermalunified.lite.activity.IRCorrectionLiteThreeComposeActivity")
                RouterConfig.IR_CORRECTION_FOUR_LITE -> getClassByName("com.mpdc4gsr.module.thermalunified.lite.activity.IRCorrectionLiteFourComposeActivity")

                RouterConfig.REPORT_CREATE_FIRST -> getClassByName("com.mpdc4gsr.module.thermalunified.report.activity.ReportCreateComposeActivity")
                RouterConfig.REPORT_CREATE_SECOND ->
                    getClassByName(
                        "com.mpdc4gsr.module.thermalunified.report.activity.ReportCreateComposeActivity",
                    )

                RouterConfig.REPORT_PREVIEW_SECOND ->
                    getClassByName(
                        "com.mpdc4gsr.module.thermalunified.activity.ReportPreviewSecondComposeActivity",
                    )

                RouterConfig.REPORT_PICK_IMG -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.ReportPickImgComposeActivity")

                RouterConfig.QUESTION -> getClassByName("com.mpdc4gsr.module.user.activity.QuestionComposeActivity")
                RouterConfig.ELECTRONIC_MANUAL -> getClassByName("com.mpdc4gsr.module.user.activity.ElectronicManualComposeActivity")
                RouterConfig.STORAGE_SPACE -> getClassByName("com.mpdc4gsr.module.user.activity.StorageSpaceComposeActivity")
                RouterConfig.TC_MORE -> getClassByName("com.mpdc4gsr.module.user.activity.MoreComposeActivity")

                RouterConfig.GSR_MULTI_MODAL -> getClassByName("mpdc4gsr.gsr.MultiModalRecordingActivity")
                RouterConfig.GSR_DEMO -> getClassByName("com.mpdc4gsr.component.gsr.activity.GSRDemoActivity")

                RouterConfig.IR_GALLERY_HOME -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRGalleryActivity")
                RouterConfig.IR_CAMERA_SETTING -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRCameraSettingActivity")

                RouterConfig.QUESTION_DETAILS -> getClassByName("com.mpdc4gsr.module.user.activity.QuestionDetailsComposeActivity")

                RouterConfig.PDF -> getClassByName("com.mpdc4gsr.module.user.activity.PDFActivity")

                RouterConfig.DEVICE_INFORMATION -> getClassByName("com.mpdc4gsr.module.user.activity.DeviceDetailsComposeActivity")
                RouterConfig.TISR -> getClassByName("com.mpdc4gsr.module.user.activity.TISRComposeActivity")
                RouterConfig.AUTO_SAVE -> getClassByName("com.mpdc4gsr.module.user.activity.AutoSaveComposeActivity")

                RouterConfig.IR_CORRECTION_TWO -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRCorrectionTwoActivity")
                RouterConfig.IR_GALLERY_DETAIL_01 -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.IRGalleryDetail01Activity")
                RouterConfig.REPORT_PICK_IMG -> getClassByName("com.mpdc4gsr.module.thermalunified.activity.ReportPickImgActivity")
                RouterConfig.UNIT -> getClassByName("com.mpdc4gsr.module.user.activity.UnitComposeActivity")

                else -> {
                    throw IllegalArgumentException("Unknown route: $route")
                }
            }
        return Intent(context, activityClass)
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

    /**
     * Get activity class by name using reflection.
     * 
     * WARNING: This method uses Class.forName() which is an anti-pattern.
     * It's kept for backward compatibility with the string-based routing system.
     * 
     * @param className Fully qualified class name
     * @return Class object for the activity
     * @throws IllegalArgumentException if the class is not found
     */
    private fun getClassByName(className: String): Class<*> {
        return try {
            Log.d("NavigationManager", "Loading class via reflection: $className")
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            Log.e("NavigationManager", "Activity class not found: $className", e)
            throw IllegalArgumentException("Activity class not found: $className. Consider using UnifiedNavigation instead.", e)
        }
    }
}
