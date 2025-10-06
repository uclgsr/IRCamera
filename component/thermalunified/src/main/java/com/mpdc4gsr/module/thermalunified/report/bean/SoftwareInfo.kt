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
