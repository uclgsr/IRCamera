package com.mpdc4gsr.component.thermal.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageTempBean(
    val full: TempBean?,
    val pointList: ArrayList<TempBean>,
    val lineList: ArrayList<TempBean>,
    val rectList: ArrayList<TempBean>,
) : Parcelable {
    @Parcelize
    data class TempBean(
        val max: String,
        val min: String? = null,
        val average: String? = null,
    ) : Parcelable
}

