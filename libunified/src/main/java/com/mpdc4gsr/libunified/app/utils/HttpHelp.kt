package com.mpdc4gsr.libunified.app.utils

import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy.Companion.instant
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams

object HttpHelp {
    fun getFirstReportData(
        isTC007: Boolean,
        pageNumber: Int,
        iResponseCallback: IResponseCallback
    ) {
        val url = UrlConstants.BASE_URL + "api/v1/outProduce/testReport/getTestReport"
        val params = RequestParams()
        params.addBodyParameter(
            "modelId",
            if (isTC007) 1783 else 950
        )//TC001-950, TC002-951, TC003-952 TC007-1783
        params.addBodyParameter("status", 1)
        params.addBodyParameter("reportType", 2)
        params.addBodyParameter("languageId", LanguageUtils.getLanguageId(ContextProvider.getContext()))
        params.addBodyParameter("current", pageNumber)
        params.addBodyParameter("size", 20)
        instant.post(url, true, params, iResponseCallback)
    }
}