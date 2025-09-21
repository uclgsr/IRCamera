package com.mpdc4gsr.libunified.app.utils

import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy.Companion.instant
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtil
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams

object HttpHelp {

    fun getFirstReportData(
        isTC007: Boolean,
        pageNumber: Int,
        iResponseCallback: IResponseCallback,
    ) {
        val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/getTestReport"
        val params = RequestParams()
        params.addBodyParameter(
            "modelId",
            if (isTC007) 1783 else 950
        )
        params.addBodyParameter("status", 1)
        params.addBodyParameter("reportType", 2)
        params.addBodyParameter("languageId", LanguageUtil.getLanguageId(Utils.getApp()))
        params.addBodyParameter("current", pageNumber)
        params.addBodyParameter("size", 20)
        instant.post(url, true, params, iResponseCallback)
    }
}
