package com.mpdc4gsr.libunified.app.socket

import android.text.TextUtils
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject

object SocketCmdUtils {
    fun getSocketCmd(cmd: Int): String? {
        var cmdJson: String? = null
        try {
            val gson = Gson()
            val paramMap: HashMap<String, Int> = HashMap()
            paramMap["cmd"] = cmd
            cmdJson = gson.toJson(paramMap)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            return cmdJson
        }
    }

    fun getCmdResponse(response: String?): Int? {
        var cmd: Int? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            cmd = jsonObject.getInt("cmd")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return cmd
    }

    fun getIpResponse(response: String?): String? {
        var ip: String? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            ip = jsonObject.getString("ip")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ip
    }

    fun getDataResponse(response: String?): String? {
        var data: String? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            data = jsonObject.getString("data")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return data
    }
}
