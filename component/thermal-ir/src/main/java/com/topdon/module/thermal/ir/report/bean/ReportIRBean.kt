package com.topdon.module.thermal.ir.report.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
一张image对应的datainfo.
 */
/**
 * Report i r data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
@Parcelize
data class ReportIRBean(
    var picture_id: String, // Uploadservice器后interfaceReturn的imageId
    var picture_url: String, // Uploadservice器后interfaceReturn的imageURL
    val full_graph_data: ReportTempBean?, // 全图data
    val point_data: List<ReportTempBean>, // pointdata
    val line_data: List<ReportTempBean>, // linedata
    val surface_data: List<ReportTempBean>, // areadata
) : Parcelable
