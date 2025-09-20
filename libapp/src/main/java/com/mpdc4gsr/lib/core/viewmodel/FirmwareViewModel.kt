package com.mpdc4gsr.lib.core.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.lib.core.R
import com.mpdc4gsr.lib.core.config.FileConfig
import com.mpdc4gsr.lib.core.repository.ProductBean

import com.mpdc4gsr.lms.sdk.LMS
import com.mpdc4gsr.lms.sdk.UrlConstant
import com.mpdc4gsr.lms.sdk.bean.CommonBean
import com.mpdc4gsr.lms.sdk.network.HttpProxy
import com.mpdc4gsr.lms.sdk.network.IResponseCallback
import com.mpdc4gsr.lms.sdk.network.ResponseBean
import com.mpdc4gsr.lms.sdk.utils.DateUtils
import com.mpdc4gsr.lms.sdk.utils.LanguageUtil
import com.mpdc4gsr.lms.sdk.xutils.http.RequestParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.TimeZone
import java.util.concurrent.CountDownLatch

class FirmwareViewModel(application: Application) : AndroidViewModel(application) {
    companion object {

        // TS004/TC007 constants removed - functionality disabled
        // private const val TS004_SOFT_CODE = "TS004_FirmwareSW_Scope"
        // private const val TC007_SOFT_CODE = "TC007_FirmwareSW_Wireless"
        // private const val TS004_FIRMWARE_VERSION = "V1.70"
        // private const val TS004_FIRMWARE_NAME = "TS004V1.70.zip"
        // private const val TC007_FIRMWARE_VERSION = "V4.06"
        // private const val TC007_FIRMWARE_NAME = "TC007V4.06.zip"

        private const val USE_DEBUG_SN = false
        // Debug constants removed for TS004/TC007
        // private const val TS004_DEBUG_SN = "1D003655A10016"
        // private const val TS004_DEBUG_RANDOM_NUM = "8D2N01"
        // private const val TC007_DEBUG_SN = "1D004714E10002"
        // private const val TC007_DEBUG_RANDOM_NUM = "EN6L6Q"
    }

    @Volatile
    private var isRequest = false

    val firmwareDataLD: MutableLiveData<FirmwareData?> = MutableLiveData()

    val failLD: MutableLiveData<Boolean> = MutableLiveData()

    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long,
    )

    fun queryFirmware(isTS004: Boolean = false) { // Parameter kept for compatibility but unused
        if (isRequest) { 
            return
        }
        isRequest = true

        viewModelScope.launch(Dispatchers.IO) {

            // TS004/TC007 functionality removed
            XLog.w("TS004/TC007 功能已移除")
            failLD.postValue(false)
            isRequest = false
            return@launch
            }
        }
    }

    // TS004/TC007 functionality removed - method disabled
    // private fun getInfoFromAssets(
    //     isTS004: Boolean,
    //     firmware: String,
    // ) {
    //     // Implementation removed due to TS004/TC007 support removal
    // }

    // TS004/TC007 functionality removed - method disabled
    // private suspend fun getInfoFromNetwork(
    //     isTS004: Boolean,
    //     sn: String,
    //     randomNum: String,
    //     firmware: String,
    // ) {
    //     // Implementation removed due to TS004/TC007 support removal
    // }

    private suspend fun bindDevice(
        sn: String,
        randomNum: String,
    ): Int {
        return withContext(Dispatchers.IO) {
            var code = LMS.SUCCESS.toInt()
            val countDownLatch = CountDownLatch(1)
            LMS.getInstance().bindDevice(sn, randomNum, "", "", object : IResponseCallback {
                override fun onResponse(response: String) {
                    try {
                        val responseBean = Gson().fromJson(response, ResponseBean::class.java)
                        code = responseBean.code.toInt()
                    } catch (e: Exception) {
                        code = 9999 // Error code
                    }
                    countDownLatch.countDown()
                }
            })
            countDownLatch.await()
            return@withContext code
        }
    }

    private suspend fun querySoftPackage(
        sn: String,
        softCode: String,
    ): PackageData? =
        withContext(Dispatchers.IO) {
            var packageData: PackageData? = null
            val countDownLatch = CountDownLatch(1)

            val url = UrlConstant.BASE_URL + "api/v1/user/deviceSoftOut/page"
            val params = RequestParams()
            params.addBodyParameter("sn", sn)
            params.addBodyParameter("softCode", softCode)
            params.addBodyParameter(
                "downloadLanguageId",
                LanguageUtil.getLanguageId(Utils.getApp())
            )
            params.addBodyParameter("downloadPlatformId", 2) 
            params.addBodyParameter(
                "queryTime",
                DateUtils.formatDate(System.currentTimeMillis()),
            )
            HttpProxy.getInstance().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean =
                                Gson().fromJson(response, CommonBean::class.java)
                            packageData = Gson().fromJson(commonBean.data, PackageData::class.java)
                        } catch (_: Exception) {
                        }
                        countDownLatch.countDown()
                    }

                    override fun onFail(exception: Exception?) {
                        countDownLatch.countDown()
                    }
                },
            )

            countDownLatch.await()
            return@withContext packageData
        }

    private suspend fun queryDownloadUrl(
        sn: String,
        businessId: Int,
    ): DownloadData? =
        withContext(Dispatchers.IO) {
            var result: DownloadData? = null
            val countDownLatch = CountDownLatch(1)
            val url = UrlConstant.BASE_URL + "api/v1/user/deviceSoftOut/getFileUrl"
            val params = RequestParams()
            params.addBodyParameter("sn", sn)
            params.addBodyParameter("businessId", businessId)
            params.addBodyParameter("businessType", 20) 
            params.addBodyParameter("productType", 20) 
            params.addBodyParameter("isCheckPoint", 0) 
            HttpProxy.getInstance().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean =
                                Gson().fromJson(response, CommonBean::class.java)
                            if (commonBean.code == LMS.SUCCESS) {
                                result = Gson().fromJson(commonBean.data, DownloadData::class.java)
                                result?.responseCode = commonBean.code.toInt()
                            } else {
                                result = DownloadData("", 0, commonBean.code.toInt())
                            }
                        } catch (_: Exception) {
                        }
                        countDownLatch.countDown()
                    }

                    override fun onFail(exception: Exception?) {
                        countDownLatch.countDown()
                    }
                },
            )
            countDownLatch.await()
            return@withContext result
        }

    private fun getVersionFromStr(versionStr: String): Double =
        try {
            if (versionStr[0] == 'V') {
                versionStr.substring(1, versionStr.length).toDouble()
            } else {
                versionStr.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }

    private class PackageData {
        var records: List<Record>? = null

        fun getFirstRecord(): Record? = if (records?.isNotEmpty() == true) records?.get(0) else null

        data class Record(
            var maxUpdateVersion: String?, 
            var maxUpdateVersionSoftId: Int, 
            var maxVersionDetailResVO: MaxVersionDetailResVO?,
        ) {
            fun getUpdateStr(): String {
                val otherExplain: List<OtherExplain>? = maxVersionDetailResVO?.otherExplain
                if (otherExplain != null) {
                    for (data in otherExplain) {
                        if (data.valueType == 3) {
                            return data.textDescription ?: ""
                        }
                    }
                }
                return ""
            }
        }

        data class MaxVersionDetailResVO(
            val otherExplain: List<OtherExplain>?,
        )

        data class OtherExplain(
            val valueType: Int, 
            val textDescription: String?,
        )
    }

    private data class DownloadData(
        val downUrl: String?,
        val size: Long?,
        var responseCode: Int,
    )
}
