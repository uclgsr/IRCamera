package com.mpdc4gsr.lib.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.tools.DeviceTools

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

                RouterConfig.MAIN -> getClassByName("mpdc4gsr.MainActivity")
                RouterConfig.CLAUSE -> getClassByName("mpdc4gsr.ClauseActivity")
                RouterConfig.POLICY -> getClassByName("mpdc4gsr.PolicyActivity")
                RouterConfig.VERSION -> getClassByName("mpdc4gsr.VersionActivity")
                RouterConfig.IR_GALLERY_EDIT -> getClassByName("mpdc4gsr.IRGalleryEditActivity")
                RouterConfig.WEB_VIEW -> getClassByName("mpdc4gsr.WebViewActivity")

                RouterConfig.IR_MAIN -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRMainActivity")
                RouterConfig.IR_SETTING -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRConfigActivity")
                RouterConfig.IR_THERMAL_MONITOR -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRMonitorActivity")
                RouterConfig.IR_MONITOR_CHART -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRMonitorChartActivity")
                RouterConfig.IR_GALLERY_DETAIL_01 -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRGalleryDetail01Activity")
                RouterConfig.IR_GALLERY_DETAIL_04 -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRGalleryDetail04Activity")
                RouterConfig.IR_VIDEO_GSY -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRVideoGSYActivity")
                RouterConfig.IR_CORRECTION_TWO -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRCorrectionTwoActivity")
                RouterConfig.IR_CORRECTION_THREE -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRCorrectionThreeActivity")
                RouterConfig.IR_CORRECTION_FOUR -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRCorrectionFourActivity")
                RouterConfig.IR_IMG_PICK -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.ImagePickIRActivity")
                RouterConfig.IR_IMG_PICK_PLUS -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.ImagePickIRPlushActivity")

                RouterConfig.IR_MONOCULAR -> getClassByName("com.mpdc4gsr.module.thermal.activity.IRMonocularActivity")
                RouterConfig.IR_DEVICE_ADD -> getClassByName("com.mpdc4gsr.module.thermal.activity.DeviceAddActivity")

                RouterConfig.GALLERY -> getClassByName("com.mpdc4gsr.module.thermal.activity.GalleryActivity")
                RouterConfig.THERMAL_MONITOR -> getClassByName("com.mpdc4gsr.module.thermal.activity.MonitorActivity")
                RouterConfig.CONNECT -> getClassByName("com.mpdc4gsr.module.thermal.activity.ConnectActivity")
                RouterConfig.VIDEO -> getClassByName("com.mpdc4gsr.module.thermal.activity.VideoActivity")
                RouterConfig.MONITOR_CHART -> getClassByName("com.mpdc4gsr.module.thermal.activity.MonitorChartActivity")
                RouterConfig.LOG_MP_CHART -> getClassByName("com.mpdc4gsr.module.thermal.activity.LogMPChartActivity")

                RouterConfig.IR_TCLITE -> getClassByName("com.example.thermal_lite.activity.IRThermalLiteActivity")
                RouterConfig.IR_THERMAL_MONITOR_LITE -> getClassByName("com.example.thermal_lite.activity.IRMonitorLiteActivity")
                RouterConfig.IR_IMG_PICK_LITE -> getClassByName("com.example.thermal_lite.activity.ImagePickIRLiteActivity")
                RouterConfig.IR_MONITOR_CHART_LITE -> getClassByName("com.example.thermal_lite.activity.IRMonitorChartLiteActivity")
                RouterConfig.IR_CORRECTION_THREE_LITE -> getClassByName("com.example.thermal_lite.activity.IRCorrectionLiteThreeActivity")
                RouterConfig.IR_CORRECTION_FOUR_LITE -> getClassByName("com.example.thermal_lite.activity.IRCorrectionLiteFourActivity")

                RouterConfig.REPORT_CREATE_FIRST -> getClassByName("com.mpdc4gsr.module.thermal.ir.report.activity.ReportCreateFirstActivity")
                RouterConfig.REPORT_CREATE_SECOND ->
                    getClassByName(
                        "com.mpdc4gsr.module.thermal.ir.report.activity.ReportCreateSecondActivity",
                    )

                RouterConfig.REPORT_PREVIEW_SECOND ->
                    getClassByName(
                        "com.mpdc4gsr.module.thermal.ir.report.activity.ReportPreviewSecondActivity",
                    )

                RouterConfig.REPORT_PICK_IMG -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.ReportPickImgActivity")

                RouterConfig.QUESTION -> getClassByName("com.mpdc4gsr.module.user.activity.QuestionActivity")
                RouterConfig.ELECTRONIC_MANUAL -> getClassByName("com.mpdc4gsr.module.user.activity.ElectronicManualActivity")
                RouterConfig.STORAGE_SPACE -> getClassByName("com.mpdc4gsr.module.user.activity.StorageSpaceActivity")
                RouterConfig.TC_MORE -> getClassByName("com.mpdc4gsr.module.user.activity.MoreActivity")

                RouterConfig.GSR_MULTI_MODAL -> getClassByName("mpdc4gsr.gsr.MultiModalRecordingActivity")
                RouterConfig.GSR_DEMO -> getClassByName("com.mpdc4gsr.component.gsr.activity.GSRDemoActivity")

                RouterConfig.IR_GALLERY_HOME -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRGalleryActivity")
                RouterConfig.IR_CAMERA_SETTING -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRCameraSettingActivity")

                RouterConfig.QUESTION_DETAILS -> getClassByName("com.mpdc4gsr.module.user.activity.QuestionDetailActivity")

                RouterConfig.PDF -> getClassByName("com.mpdc4gsr.module.user.activity.PDFActivity")

                RouterConfig.DEVICE_INFORMATION -> getClassByName("com.mpdc4gsr.module.user.activity.DeviceDetailsActivity")
                RouterConfig.TISR -> getClassByName("com.mpdc4gsr.module.user.activity.TISRActivity")
                RouterConfig.AUTO_SAVE -> getClassByName("com.mpdc4gsr.module.user.activity.AutoSaveActivity")

                RouterConfig.IR_CORRECTION_TWO -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRCorrectionTwoActivity")
                RouterConfig.IR_GALLERY_DETAIL_01 -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.IRGalleryDetail01Activity")
                RouterConfig.REPORT_PICK_IMG -> getClassByName("com.mpdc4gsr.module.thermal.ir.activity.ReportPickImgActivity")
                RouterConfig.UNIT -> getClassByName("com.mpdc4gsr.module.user.activity.UnitActivity")

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

    private fun getClassByName(className: String): Class<*> {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Activity class not found: $className", e)
        }
    }
}
