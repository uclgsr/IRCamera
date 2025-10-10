package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.utils.TLog
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import com.mpdc4gsr.libunified.R as LibR

class PdfViewModel : BaseViewModel() {
    val listData = MutableLiveData<ReportData?>()
    fun getReportData(
        isTC007: Boolean,
        page: Int,
    ) {
        if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
            TToast.shortToast(ContextProvider.getContext(), LibR.string.http_code_z5004)
            listData.postValue(null)
            return
        }
        viewModelScope.launch {
            val data = getReportDataRepository(isTC007, page)
            listData.postValue(data)
        }
    }

    private suspend fun getReportDataRepository(
        isTC007: Boolean,
        page: Int,
    ): ReportData? {
        var result: ReportData? = null
        val downLatch = CountDownLatch(1)
        HttpHelp.getFirstReportData(
            isTC007,
            page,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    result = Gson().fromJson(p0, ReportData::class.java)
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    result = ReportData()
                    result?.msg = p0?.message
                    result?.code = -1
                    downLatch.countDown()
                    TLog.e("bcf", "：" + p0?.message)
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
