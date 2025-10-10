package com.mpdc4gsr.libunified.app.http.repository

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mpdc4gsr.libunified.app.bean.base.Resp
import com.mpdc4gsr.libunified.app.bean.json.CheckVersionJson
import com.mpdc4gsr.libunified.app.bean.json.StatementJson
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

object LmsRepository {
    suspend fun getVersionInfo(): CheckVersionJson? {
        var result: CheckVersionJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().checkAppUpdate { response ->
            try {
                val responseBean = Gson().fromJson(response, ResponseBean::class.java)
                if (responseBean.code == "2000") {
                    result =
                        Gson().fromJson(responseBean.data.toString(), CheckVersionJson::class.java)
                }
            } catch (e: Exception) {
            }
            downLatch.countDown()
        }
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }

    suspend fun getStatementUrl(type: String): StatementJson? {
        var result: StatementJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().getStatement(
            type,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    try {
                        val typeOfT = object : TypeToken<Resp<StatementJson>>() {}.type
                        val json = Gson().fromJson<Resp<StatementJson>>(p0, typeOfT)
                        if (json.code == "2000") {
                            result = json.data
                        }
                    } catch (e: Exception) {
                    }
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    downLatch.countDown()
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils
                            .getResString(
                                LMS.mContext,
                                if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                            ).let {
                                TToast.shortToast(LMS.mContext, it)
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }
}
