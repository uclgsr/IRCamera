package com.mpdc4gsr.component.thermal.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportBean(
    val software_info: SoftwareInfo,
    val report_info: ReportInfoBean,
    val detection_condition: ReportConditionBean,
    val infrared_data: List<ReportIRBean>,
) : Parcelable

