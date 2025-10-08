// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository\libunified_src_main_java_com_mpdc4gsr_libunified_app_http_repository_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository\LmsRepository.kt =====

package com.mpdc4gsr.libunified.app.http.repository

import android.text.TextUtils
import com.elvishew.xlog.XLog
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
                XLog.e("version json[ph][ph][ph][ph]: ${e.message}")
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
                        XLog.e("json[ph][ph][ph][ph]: ${e.message}")
                    }
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    downLatch.countDown()
                    XLog.w("onFail: $result")
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils.getResString(
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