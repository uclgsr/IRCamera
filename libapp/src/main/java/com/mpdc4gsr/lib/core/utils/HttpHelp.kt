package com.mpdc4gsr.lib.core.utils

import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.lms.sdk.UrlConstant
import com.mpdc4gsr.lms.sdk.network.HttpProxy.Companion.instant
import com.mpdc4gsr.lms.sdk.network.IResponseCallback
import com.mpdc4gsr.lms.sdk.utils.LanguageUtil
import com.mpdc4gsr.lms.sdk.xutils.http.RequestParams

object HttpHelp {

    fun getFirstReportData(
        // isTC007 parameter removed - TC007 functionality disabled
        pageNumber: Int,
        iResponseCallback: IResponseCallback,
    ) {
        val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/getTestReport"
        val params = RequestParams()
        params.addBodyParameter(
            "modelId",
            950 // Always use default model, TC007 functionality removed
        ) 
        params.addBodyParameter("status", 1)
        params.addBodyParameter("reportType", 2)
        params.addBodyParameter("languageId", LanguageUtil.getLanguageId(Utils.getApp()))
        params.addBodyParameter("current", pageNumber)
        params.addBodyParameter("size", 20)
        instant.post(url, true, params, iResponseCallback)
    }
}
