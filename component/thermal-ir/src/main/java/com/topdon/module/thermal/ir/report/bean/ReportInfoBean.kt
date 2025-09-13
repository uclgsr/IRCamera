package com.topdon.module.thermal.ir.report.bean

import android.os.Parcelable
import com.topdon.lib.core.utils.CommUtils
import kotlinx.android.parcel.Parcelize

/**
reportinfo.
 *
report由 3 部Group成：reportinfo、检测条件、infrareddata.
 */
/**
 * Report info data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
@Parcelize
data class ReportInfoBean(
    val report_name: String?, // reportname
    val report_author: String?, // 作者name
    val is_report_author: Int, // 是否Show/Display作者name，0、不Show/Display 1、Show/Display
    val report_date: String?, // report日期
    val is_report_date: Int, // 是否Show/Displayreport日期，0、不Show/Display 1、Show/Display
    val report_place: String?, // report地point
    val is_report_place: Int, // 是否Show/Displayreport地point，0、不Show/Display 1、Show/Display
    val report_watermark: String?, // reportwatermark
    val is_report_watermark: Int, // 是否Show/Displayreportwatermark，0、不Show/Display 1、Show/Display
) : Parcelable {
    val is_report_name: Int = 1 // 是否Show/Displayreportname，0、不Show/Display 1、Show/Display
    val report_type: Int = 1 // reporttype，1、pointlineareareport
    val report_version: String = "V1.00" // reportversion，当前为 V1.00
    val report_number: String = "${CommUtils.getAppName()}${System.currentTimeMillis()}" // report编号，APPname + 时间戳秒级
}
