package com.mpdc4gsr.commons.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader

object UTF8StringUtils {
    fun readByUtf8WithBom(path: String): String {
        val file = File(path)
        val `in`: FileInputStream?
        val read: Reader?
        try {
            if (file.exists() && file.isFile()) {
                `in` = FileInputStream(file)
                read = InputStreamReader(`in`)
                val bf = BufferedReader(read)
                var txt: String
                while ((bf.readLine().also { txt = it }) != null) {
                    txt = txt.trim { it <= ' ' }
                    val flag = txt.substring(txt.lastIndexOf("|") + 1)
                    if (flag == "1") {
                        return txt.substring(0, txt.lastIndexOf("|"))
                    }
                    return txt
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    fun readByUtf8WithOutBom(path: String): String {
        val file = File(path)
        val `in`: FileInputStream?
        try {
            if (file.exists() && file.isFile()) {
                `in` = FileInputStream(file)
                val bf = BufferedReader(UnicodeReader(`in`, "utf-8"))
                var txt = ""
                while ((bf.readLine().also { txt = it }) != null) {
                    txt = txt.trim { it <= ' ' }
                    val flag = txt.substring(txt.lastIndexOf("|") + 1)
                    if (flag == "1") {
                        return txt.substring(0, txt.lastIndexOf("|"))
                    }
                    return txt
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
}
