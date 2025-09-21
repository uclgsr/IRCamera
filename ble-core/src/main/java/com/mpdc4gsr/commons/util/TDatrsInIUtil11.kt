package com.mpdc4gsr.commons.util

import android.text.TextUtils
import org.ini4j.Config
import org.ini4j.Ini
import java.io.File
import java.util.Locale

object TDatrsInIUtil {
    fun getTdartsVersion(path: String?): String? {
        val file = File(path + "T-darts.ini")
        if (!file.exists()) {
            LLog.e("bcf", "  iniDoes Not Exist：" + file.getPath())
            return ""
        }
        val cfg = Config()
        cfg.setLowerCaseOption(true)
        cfg.setLowerCaseSection(true)
        cfg.setMultiSection(true)
        val ini = Ini()
        ini.setConfig(cfg)
        try {
            ini.load(file)
            val tDartSWSection = ini.get("TDartSW".lowercase(Locale.getDefault()))
            if (tDartSWSection == null) {
                return ""
            }
            if (!TextUtils.isEmpty(tDartSWSection.get("version"))) {
                return tDartSWSection.get("Version".lowercase(Locale.getDefault()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return ""
    }

    fun getTdarts(path: String?): HashMap<String?, String?> {
        val hashMap = HashMap<String?, String?>()
        val file = File(path + "T-darts.ini")
        if (!file.exists()) {
            LLog.e("bcf", "  iniDoes Not Exist：" + file.getPath())
            return hashMap
        }
        val cfg = Config()
        cfg.setLowerCaseOption(true)
        cfg.setLowerCaseSection(true)
        cfg.setMultiSection(true)
        val ini = Ini()
        ini.setConfig(cfg)
        try {
            ini.load(file)
            val tDartSWSection = ini.get("TDartSW".lowercase(Locale.getDefault()))
            if (tDartSWSection == null) {
                return hashMap
            }
            if (!TextUtils.isEmpty(tDartSWSection.get("version"))) {
                hashMap.put("Version", tDartSWSection.get("Version".lowercase(Locale.getDefault())))
            }

            val libsSection = ini.get("libs")
            if (libsSection == null) {
                return hashMap
            }

            if (!TextUtils.isEmpty(libsSection.get("T-dartsApp".lowercase(Locale.getDefault())))) {
                hashMap.put("T-dartsApp", libsSection.get("T-dartsApp".lowercase(Locale.getDefault())))
            }
            if (!TextUtils.isEmpty(libsSection.get("825x_module".lowercase(Locale.getDefault())))) {
                hashMap.put("825x_module", libsSection.get("825x_module".lowercase(Locale.getDefault())))
            }
            if (!TextUtils.isEmpty(libsSection.get("N32S032-app".lowercase(Locale.getDefault())))) {
                hashMap.put("N32S032-app", libsSection.get("N32S032-app".lowercase(Locale.getDefault())))
            }
            return hashMap
        } catch (e: Exception) {
            e.printStackTrace()
            return hashMap
        }
    }

    fun getBinPath(data: Int): String {
        val path = FolderUtil.getTdartsUpgradePath()
        if (data == 0) {
            return path + "T-dartsApp.bin"
        } else if (data == 1) {
            return path + "825x_module.bin"
        } else if (data == 2) {
            return path + "N32S032-app.bin"
        }
        return ""
    }
}
