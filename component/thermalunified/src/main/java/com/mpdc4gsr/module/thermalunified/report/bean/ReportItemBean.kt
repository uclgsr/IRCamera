package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.blankj.utilcode.util.GsonUtils
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
data class ReportItemBean(
    val testReportId: String?,
    val testInfo: String?,
    val testTime: String?,
    val uploadTime: String?,
    val sn: String?,
    val url: String?,
    val status: Int?,
) : Parcelable {
    @IgnoredOnParcel
    var reportBean: ReportBean? = null
        get() {
            if (field == null) {
                field = GsonUtils.fromJson(testInfo, ReportBean::class.java)
            }
            return field
        }

    @IgnoredOnParcel
    var isFirst: Boolean = false
    @IgnoredOnParcel
    var isTitle: Boolean = false
}
