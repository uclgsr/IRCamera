package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Build
import android.os.Parcelable
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.tools.AppLanguageUtils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SoftwareInfo(
    val app_language: String,
    val sdk_version: String,
) : Parcelable {
    val software_code = BaseApplication.instance.getSoftWareCode()
    val system_language = AppLanguageUtils.getSystemLanguage()
    val app_version = "1.10.000"
    val hardware_version = ""
    val app_sn = ""
    val mobile_phone_model = Build.BRAND
    val system_version = Build.VERSION.RELEASE
}
