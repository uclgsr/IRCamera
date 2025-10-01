package com.mpdc4gsr.libunified.app.utils

import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy.Companion.instant
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams

/**
 * @author: CaiSongL
 * @date: 2023/5/12 17:17
 */
object HttpHelp {

    /**
     *
     * modelId：TC001 950, TC002 951, TC003952
     */
    fun getFirstReportData(
        isTC007: Boolean,
        pageNumber: Int,
        iResponseCallback: IResponseCallback
    ) {
        val url = UrlConstant.BASE_URL + "api/v1/outProduce/testReport/getTestReport"
        val params = RequestParams()
        params.addBodyParameter(
            "modelId",
            if (isTC007) 1783 else 950
        )//TC001-950, TC002-951, TC003-952 TC007-1783
        params.addBodyParameter("status", 1)
        params.addBodyParameter("reportType", 2)
        params.addBodyParameter("languageId", LanguageUtils.getLanguageId(Utils.getApp()))
        params.addBodyParameter("current", pageNumber)
        params.addBodyParameter("size", 20)
        instant.post(url, true, params, iResponseCallback)
    }

}