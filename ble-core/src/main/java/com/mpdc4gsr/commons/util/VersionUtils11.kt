package com.mpdc4gsr.commons.util

import android.text.TextUtils

object VersionUtils {
    fun compareVersions(v1: String, v2: String): Boolean {
        if (TextUtils.equals(v1, "") || TextUtils.equals(v2, "")) {
            return false
        }
        val str1: Array<String?> = v1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val str2: Array<String?> = v2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (str1.size == str2.size) {
            for (i in str1.indices) {
                if (str1[i]!!.toInt() > str2[i]!!.toInt()) {
                    return true
                } else if (str1[i]!!.toInt() < str2[i]!!.toInt()) {
                    return false
                } else if (str1[i]!!.toInt() == str2[i]!!.toInt()) {
                }
            }
        } else {
            if (str1.size > str2.size) {
                for (i in str2.indices) {
                    if (str1[i]!!.toInt() > str2[i]!!.toInt()) {
                        return true
                    } else if (str1[i]!!.toInt() < str2[i]!!.toInt()) {
                        return false
                    } else if (str1[i]!!.toInt() == str2[i]!!.toInt()) {
                        if (str2.size == 1) {
                            continue
                        }
                        if (i == str2.size - 1) {
                            for (j in i..<str1.size) {
                                if (str1[j]!!.toInt() != 0) {
                                    return true
                                }
                                if (j == str1.size - 1) {
                                    return false
                                }
                            }
                            return true
                        }
                    }
                }
            } else {
                for (i in str1.indices) {
                    if (str1[i]!!.toInt() > str2[i]!!.toInt()) {
                        return true
                    } else if (str1[i]!!.toInt() < str2[i]!!.toInt()) {
                        return false
                    } else if (str1[i]!!.toInt() == str2[i]!!.toInt()) {
                        if (str1.size == 1) {
                            continue
                        }
                        if (i == str1.size - 1) {
                            return false
                        }
                    }
                }
            }
        }
        return false
    }
}
