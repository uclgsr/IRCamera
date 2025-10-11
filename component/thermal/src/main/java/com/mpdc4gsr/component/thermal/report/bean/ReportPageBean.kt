package com.mpdc4gsr.component.thermal.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportPageBean(
    val total: Int = 0,
    var current: Int = 0,
    var pages: Int = 0,
    var size: Int = 0,
    var isHitCount: Boolean = false,
    var isOptimizeCountSql: Boolean = false,
    var isSearchCount: Boolean = false,
    var records: MutableList<ReportItemBean>? = null,
) : Parcelable

