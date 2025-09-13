package com.topdon.module.thermal.ir.report.bean

import android.os.Parcelable
import com.blankj.utilcode.util.GsonUtils
import kotlinx.android.parcel.Parcelize

/**
从service器interfaceReturn的，一页reportinfo中的一条reportinfo.
 */
/**
 * Report item data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
@Parcelize
data class ReportItemBean(
    val testReportId: String?,
    val testInfo: String?, // Upload的 JSON
    val testTime: String?,
    val uploadTime: String?, // Upload时间
    val sn: String?,
    val url: String?,
    val status: Int?,
) : Parcelable {
    var reportBean: ReportBean? = null
        get() {
            if (field == null) {
                field = GsonUtils.fromJson(testInfo, ReportBean::class.java)
            }
            return field
        }

    var isFirst: Boolean = false
    var isTitle: Boolean = false
}
