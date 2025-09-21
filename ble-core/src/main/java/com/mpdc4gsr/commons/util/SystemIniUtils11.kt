package com.mpdc4gsr.commons.util

import android.text.TextUtils
import com.blankj.utilcode.util.StringUtils
import org.ini4j.Config
import org.ini4j.Ini
import java.io.File
import java.util.Locale

object SystemIniUtils {
    fun getSystemVersion(path: String?, systemName: String, systemVersion: Int): Int {
        val file = File(path + "/Version.ini")
        if (!file.exists()) {
            LLog.e("bcf", "  ini不存在：" + file.getPath())
            return -1
        }
        val cfg = Config()
        cfg.setLowerCaseOption(true)
        cfg.setLowerCaseSection(true)
        cfg.setMultiSection(true)
        val ini = Ini()
        ini.setConfig(cfg)
        try {
            ini.load(file)
            val tDartSWSection = ini.get("OTA".lowercase(Locale.getDefault()))
            if (tDartSWSection == null) {
                return 1
            }
            var firmwareSw: String? = ""
            var version: String? = ""
            if (!TextUtils.isEmpty(tDartSWSection.get("firmwaresw"))) {
                firmwareSw = tDartSWSection.get("FirmwareSW".lowercase(Locale.getDefault()))
            }

            if (!TextUtils.isEmpty(tDartSWSection.get("version"))) {
                version = tDartSWSection.get("Version".lowercase(Locale.getDefault()))
            }

            if (StringUtils.isEmpty(firmwareSw)) {
                return 1
            }
            if (StringUtils.isEmpty(version)) {
                return 1
            }
            val version1 = version!!.uppercase(Locale.getDefault()).replace("V", "")
            if (systemName == firmwareSw && version1 == systemVersion.toString()) {
                return 0
            } else {
                return 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 1
        }
    }
}
