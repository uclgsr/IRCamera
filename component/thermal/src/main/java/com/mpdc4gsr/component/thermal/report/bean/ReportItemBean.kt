package com.mpdc4gsr.component.thermal.report.bean

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
                field = Gson().fromJson(testInfo, ReportBean::class.java)
            }
            return field
        }

    @IgnoredOnParcel
    var isFirst: Boolean = false

    @IgnoredOnParcel
    var isTitle: Boolean = false
}

