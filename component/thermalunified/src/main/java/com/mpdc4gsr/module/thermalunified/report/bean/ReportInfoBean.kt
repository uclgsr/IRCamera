package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.mpdc4gsr.libunified.app.utils.CommUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportInfoBean(
    val report_name: String?,
    val report_author: String?,
    val is_report_author: Int,
    val report_date: String?,
    val is_report_date: Int,
    val report_place: String?,
    val is_report_place: Int,
    val report_watermark: String?,
    val is_report_watermark: Int,
) : Parcelable {
    @IgnoredOnParcel
    val is_report_name: Int = 1

    @IgnoredOnParcel
    val report_type: Int = 1

    @IgnoredOnParcel
    val report_version: String = "V1.00"

    @IgnoredOnParcel
    val report_number: String =
        "${CommUtils.getAppName()}${System.currentTimeMillis()}"
}
