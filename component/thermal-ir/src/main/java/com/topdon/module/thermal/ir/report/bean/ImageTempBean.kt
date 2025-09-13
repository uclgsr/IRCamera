package com.topdon.module.thermal.ir.report.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
一张image的pointlineareafull imagetemperatureinfo.
 */
/**
 * Image temp data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
@Parcelize
data class ImageTempBean(
    val full: TempBean?, // 全图
    val pointList: ArrayList<TempBean>, // point
    val lineList: ArrayList<TempBean>, // line
    val rectList: ArrayList<TempBean>, // area
) : Parcelable {
/**
 * Temp data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
    @Parcelize
    data class TempBean(
        val max: String, // 不带符号maximum温，单位跟随Userconfiguration
        val min: String? = null, // 不带符号minimum温，单位跟随Userconfiguration
        val average: String? = null, // 不带符号average温，单位跟随Userconfiguration
    ) : Parcelable
}
