// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_report_bean_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean' subtree
// Files: 10; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ImageTempBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportBean(
    val software_info: SoftwareInfo,
    val report_info: ReportInfoBean,
    val detection_condition: ReportConditionBean,
    val infrared_data: List<ReportIRBean>,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportConditionBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportConditionBean(
    val ambient_humidity: String?,
    val is_ambient_humidity: Int,
    val ambient_temperature: String?,
    val is_ambient_temperature: Int,
    val emissivity: String?,
    val is_emissivity: Int,
    val test_distance: String?,
    val is_test_distance: Int,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportData.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import com.google.gson.Gson

class ReportData {
    var code = 0
    var data: DataBean? = null
    var msg: String? = null
    var serverTime: String? = null

    class DataBean {
        var total = 0
        var current = 0
        var isHitCount = false
        var pages = 0
        var size = 0
        var isOptimizeCountSql = false
        var isSearchCount = false
        var records: MutableList<Records?>? = null
    }

    class Records {
        var testReportId: String? = null
        var testTime: String? = null
        var testInfo: String? = null
        var sn: String? = null
        var uploadTime: String? = null
        var status: String? = null
        var isShowTitleTime: Boolean = false
        var reportContent: ReportBean? = null
            get() {
                if (field == null) {
                    field = Gson().fromJson(testInfo, ReportBean::class.java)
                }
                return field
            }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportInfoBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.mpdc4gsr.libunified.app.utils.CommUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportInfoBean(
    val report_name: String?,
    val report_author: String?,
    val is_report_author: Int,
    val report_date: String?,
    val is_report_date: Int,
    val report_place: String?,
    val is_report_place: Int,
    val report_watermark: String?,
    val is_report_watermark: Int,
) : Parcelable {
    @IgnoredOnParcel
    val is_report_name: Int = 1

    @IgnoredOnParcel
    val report_type: Int = 1

    @IgnoredOnParcel
    val report_version: String = "V1.00"

    @IgnoredOnParcel
    val report_number: String =
        "${CommUtils.getAppName()}${System.currentTimeMillis()}"
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportIRBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportIRBean(
    var picture_id: String,
    var picture_url: String,
    val full_graph_data: ReportTempBean?,
    val point_data: List<ReportTempBean>,
    val line_data: List<ReportTempBean>,
    val surface_data: List<ReportTempBean>,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportItemBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportPageBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportTempBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportTempBean(
    val max_temperature: String?,
    val is_max_temperature: Int,
    val min_temperature: String?,
    val is_min_temperature: Int,
    val comment: String?,
    val is_comment: Int,
    val mean_temperature: String? = null,
    val is_mean_temperature: Int = 0,
    val temperature: String? = null,
    val is_temperature: Int = 0,
) : Parcelable {
    constructor(
        temperature: String?,
        is_temperature: Int,
        comment: String?,
        is_comment: Int
    ) : this(
        null,
        0,
        null,
        0,
        comment,
        is_comment,
        null,
        0,
        temperature,
        is_temperature,
    )

    fun isMaxOpen() = is_max_temperature == 1
    fun isMinOpen() = is_min_temperature == 1
    fun isAverageOpen() = is_mean_temperature == 1
    fun isExplainOpen() = is_comment == 1
    fun isTempOpen() = is_temperature == 1
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\SoftwareInfo.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Build
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SoftwareInfo(
    val app_language: String,
    val sdk_version: String,
) : Parcelable {
    @IgnoredOnParcel
    val software_code = BaseApplication.instance.getSoftWareCode()

    @IgnoredOnParcel
    val system_language = AppLanguageUtils.getSystemLanguage()

    @IgnoredOnParcel
    val app_version = "1.10.000"

    @IgnoredOnParcel
    val hardware_version = ""

    @IgnoredOnParcel
    val app_sn = ""

    @IgnoredOnParcel
    val mobile_phone_model = Build.BRAND

    @IgnoredOnParcel
    val system_version = Build.VERSION.RELEASE
}