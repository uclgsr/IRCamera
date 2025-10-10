package com.mpdc4gsr.libunified.app.matrix

import com.mpdc4gsr.libunified.BuildConfig
import com.mpdc4gsr.libunified.app.utils.LibraryLogger
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    @JvmStatic
    fun e(
        clazz: Class<*>,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun e(
        tag: String?,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun w(
        clazz: Class<*>,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun w(
        tag: String?,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun i(
        clazz: Class<*>,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun i(
        tag: String?,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun d(
        clazz: Class<*>,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun d(
        tag: String?,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    @JvmStatic
    fun v(
        clazz: Class<*>,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    fun v(
        tag: String?,
        msg: String,
    ) {
        if (BuildConfig.DEBUG) {
        }
    }

    private val MYLOG_PATH_SDCARD_DIR = "/sdcard/Guide/log"
    private val MYLOGFILEName = "Log.txt"
    private val myLogSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logfile = SimpleDateFormat("yyyy-MM-dd")

    fun f(
        tag: String,
        text: String,
    ) {
        val nowtime = Date()
        val needWriteFiel = logfile.format(nowtime)
        val needWriteMessage = myLogSdf.format(nowtime) + "    " + "    " + tag + "    " + text
        val dirsFile = File(MYLOG_PATH_SDCARD_DIR)
        if (!dirsFile.exists()) {
            dirsFile.mkdirs()
        }
        val file = File(dirsFile.toString(), needWriteFiel + MYLOGFILEName) // MYLOG_PATH_SDCARD_DIR
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                LibraryLogger.e("Logger", "Unexpected Exception in Logger catch block", e)
            }
        }
        try {
            val filerWriter = FileWriter(file, true)
            val bufWriter = BufferedWriter(filerWriter)
            bufWriter.write(needWriteMessage)
            bufWriter.newLine()
            bufWriter.close()
            filerWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
