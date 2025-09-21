package com.mpdc4gsr.module.thermal.ir.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.TimeUtils
import com.elvishew.xlog.XLog
import com.mpdc4gsr.lib.core.ktbase.BaseViewModel
import com.mpdc4gsr.lib.core.lms.LMS
import com.mpdc4gsr.lib.core.lms.UrlConstant
import com.mpdc4gsr.lib.core.lms.bean.CommonBean
import com.mpdc4gsr.lib.core.lms.network.HttpProxy
import com.mpdc4gsr.lib.core.lms.network.IResponseCallback
import com.mpdc4gsr.lib.core.lms.network.ResponseBean
import com.mpdc4gsr.lib.core.lms.xutils.http.RequestParams
import com.mpdc4gsr.lib.core.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermal.ir.report.bean.ReportBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch

class UpReportViewModel : BaseViewModel() {
    val commonBeanLD = SingleLiveEvent<CommonBean>()

    val exceptionLD = SingleLiveEvent<Exception?>()

    fun upload(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        viewModelScope.launch {
            uploadImages(reportBean)
            uploadJSON(isTC007, reportBean)
        }
    }

    private suspend fun uploadImages(reportBean: ReportBean?) {
        withContext(Dispatchers.IO) {
            val irList = reportBean?.infrared_data
            if (irList != null) {
                val downLatch = CountDownLatch(irList.size)
                for (reportIrBean in irList) {
                    if (reportIrBean.picture_id.isNotEmpty()) {
                        downLatch.countDown()
                        continue
                    }
                    val file = File(reportIrBean.picture_url)
                    LMS.getInstance().uploadFile(file, 0, 13, 20, object : IResponseCallback {
                        override fun onResponse(response: String?) {
                            try {
                                val responseObj = if (response != null) JSONObject(response) else JSONObject()
                                XLog.i(responseObj.toString())
                                if (responseObj.optString("code", "2000") == LMS.SUCCESS) {
                                    file.delete()
                                    val dataObj = responseObj.optJSONObject("data") ?: JSONObject()
                                    reportIrBean.picture_id = dataObj.optString("fileSecret", "")
                                    reportIrBean.picture_url = dataObj.optString("url", "")
                                }
                            } catch (e: Exception) {
                                XLog.e("Upload response parse error", e)
                            }
                            XLog.i("Upload完一张图")
                            downLatch.countDown()
                        }
                        
                        override fun onFail(exception: Exception?) {
                            XLog.e("Upload failed", exception)
                            downLatch.countDown()
                        }
                    })
                }
                downLatch.await()
                XLog.i("${irList.size} 张图Upload完毕")
            }
        }
    }

    private suspend fun uploadJSON(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        withContext(Dispatchers.IO) {
            val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/addTestReport"
            val params = RequestParams()
            params.addBodyParameter("reportType", 2)
            params.addBodyParameter(
                "modelId",
                if (isTC007) 1783 else 950
            )
            params.addBodyParameter("testTime", TimeUtils.getNowString())
            params.addBodyParameter("testInfo", GsonUtils.toJson(reportBean))
            params.addBodyParameter("sn", "")
            HttpProxy.getInstant().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        commonBeanLD.postValue(ResponseBean.convertCommonBean(response, null))
                    }

                    override fun onFail(exception: Exception?) {
                        exceptionLD.postValue(exception)
                    }
                },
            )
        }
    }
}
