package com.topdon.module.thermal.ir.report.bean

import android.os.Build
import android.os.Parcelable
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.tools.AppLanguageUtils
import kotlinx.android.parcel.Parcelize

/**
 * Software info utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
@Parcelize
data class SoftwareInfo(
    val app_language: String, // APP语言
    val sdk_version: String, // SDKversion
) : Parcelable {
    val software_code = BaseApplication.instance.getSoftWareCode() // softwareencoding
    val system_language = AppLanguageUtils.getSystemLanguage() // 系统语言
    val app_version = "1.10.000" // softwareversion
    val hardware_version = "" // hardwareversion
    val app_sn = ""
    val mobile_phone_model = Build.BRAND // 手机model
    val system_version = Build.VERSION.RELEASE // 系统version
}
