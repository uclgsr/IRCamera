package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.kotlinx.android.parcel.Parcelize

@Parcelize
data class ReportBean(
    val software_info: SoftwareInfo,
    val report_info: ReportInfoBean,
    val detection_condition: ReportConditionBean,
    val infrared_data: List<ReportIRBean>,
) : Parcelable
