package com.topdon.lib.core.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.topdon.lib.core.R
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.repository.ProductBean
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.UrlConstant
import com.topdon.lms.sdk.bean.CommonBean
import com.topdon.lms.sdk.network.HttpProxy
import com.topdon.lms.sdk.network.IResponseCallback
import com.topdon.lms.sdk.network.ResponseBean
import com.topdon.lms.sdk.utils.DateUtils
import com.topdon.lms.sdk.utils.LanguageUtil
import com.topdon.lms.sdk.xutils.http.RequestParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.NumberFormatException
import java.util.TimeZone
import java.util.concurrent.CountDownLatch

/**
 * firmwareUpgrade包
 */
/**
 * FirmwareViewModel implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class FirmwareViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        /**
         * TS004 firmwareUpgrade包 softwareencoding.
         */
        private const val TS004_SOFT_CODE = "TS004_FirmwareSW_Scope"

        /**
         * TC007 firmwareUpgrade包 softwareencoding.
         */
        private const val TC007_SOFT_CODE = "TC007_FirmwareSW_Wireless"

        /**
         * TS004 apk 内置firmwareUpgrade包version.
         */
        private const val TS004_FIRMWARE_VERSION = "V1.70"

        /**
         * TS004 apk 内置firmwareUpgrade包file名.
         */
        private const val TS004_FIRMWARE_NAME = "TS004V1.70.zip"

        /**
         * TC007 apk 内置firmwareUpgrade包version.
         */
        private const val TC007_FIRMWARE_VERSION = "V4.06"

        /**
         * TC007 apk 内置firmwareUpgrade包file名.
         */
        private const val TC007_FIRMWARE_NAME = "TC007V4.06.zip"

        private const val USE_DEBUG_SN = false
        private const val TS004_DEBUG_SN = "1D003655A10016"
        private const val TS004_DEBUG_RANDOM_NUM = "8D2N01"
        private const val TC007_DEBUG_SN = "1D004714E10002"
        private const val TC007_DEBUG_RANDOM_NUM = "EN6L6Q"
    }

    /**
     * 用一个variable来storage请求state，避免重复请求.
     */
    @Volatile
    private var isRequest = false

    /**
     * 查询firmwareUpgrade包success LiveData.
     * null表示查询success但没有配firmwareUpgrade包
     */
    val firmwareDataLD: MutableLiveData<FirmwareData?> = MutableLiveData()

    /**
     * 查询firmwareUpgrade包failed LiveData.
     * true-device已被其他User绑定error false-普通error
     */
    val failLD: MutableLiveData<Boolean> = MutableLiveData()

    /**
     * 一个firmwareUpgrade包info.
     * @param version 该firmwareUpgrade包version，V1.00format
     * @param updateStr Upgrade文案info
     * @param downUrl firmwareUpgrade包 URL
     * @param size firmwareUpgrade包大小，单位 byte
     */
    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long,
    )

    /**
     * 执行一次firmwareUpgrade包查询，结果Send往：
     * - [firmwareDataLD] (success)
     * - [failLD] (failed)
     * @param isTS004 true-TS004 false-TC007
     */
    fun queryFirmware(isTS004: Boolean) {
        if (isRequest) { // 别催别催，在查了
            return
        }
        isRequest = true

        viewModelScope.launch(Dispatchers.IO) {
            // 由于双通道方案存在问题，V3.30临时使用 apk 内置firmwareUpgrade包，以下使用network的代码先comment
            /*if (isTS004) {
                //从 TS004 中Get/Retrieve SN、Activate码
                val deviceInfo: DeviceInfo? = TS004Repository.getDeviceInfo()?.data
                if (deviceInfo == null) {
                    XLog.w("TS004 firmwareUpgrade - 从device查询 SN、Activate码 failed!")
                    failLD.postValue(false)
                    isRequest = false
                    return@launch
                }

                //从 TS004 中Get/Retrievefirmwareversion
                val firmware: String? = TS004Repository.getVersion()?.data?.firmware
                if (firmware == null) {
                    XLog.w("TS004 firmwareUpgrade - 从device查询 firmwareversion failed!")
                    failLD.postValue(false)
                    isRequest = false
                    return@launch
                }

                val sn: String = if (USE_DEBUG_SN) TS004_DEBUG_SN else deviceInfo.sn
                val randomNum: String = if (USE_DEBUG_SN) TS004_DEBUG_RANDOM_NUM else deviceInfo.code
                getInfoFromNetwork(true, sn, randomNum, firmware)
            } else {
                //从 TC007 中Get/Retrieve SN、Activate码
                val productInfo: ProductBean? = TC007Repository.getProductInfo()
                if (productInfo == null) {
                    XLog.w("TC007 firmwareUpgrade - 从device查询 SN、Activate码 failed!")
                    failLD.postValue(false)
                    isRequest = false
                    return@launch
                }

                val sn: String = if (USE_DEBUG_SN) TC007_DEBUG_SN else productInfo.ProductSN
                val randomNum: String = if (USE_DEBUG_SN) TC007_DEBUG_RANDOM_NUM else productInfo.Code
                val firmware = "V${productInfo.getVersionStr()}"
                getInfoFromNetwork(false, sn, randomNum, firmware)
            }*/

            // 由于双通道方案存在问题，V3.30临时使用 apk 内置firmwareUpgrade包，以下为临时方案逻辑
            if (isTS004) {
                // 从 TS004 中Get/Retrievefirmwareversion
                val firmware: String? = TS004Repository.getVersion()?.data?.firmware
                if (firmware == null) {
                    XLog.w("TS004 firmwareUpgrade - 从device查询 firmwareversion failed!")
                    failLD.postValue(false)
                    isRequest = false
                    return@launch
                }

                getInfoFromAssets(true, firmware)
            } else {
                // 从 TC007 中Get/Retrievefirmwareversion
                val productInfo: ProductBean? = TC007Repository.getProductInfo()
                if (productInfo == null) {
                    XLog.w("TC007 firmwareUpgrade - 从device查询 SN、Activate码 failed!")
                    failLD.postValue(false)
                    isRequest = false
                    return@launch
                }

                getInfoFromAssets(false, "V${productInfo.getVersionStr()}")
            }
        }
    }

    /**
     * 将 assets 中的firmwareUpgrade包export，并将相关info post 到对应 LiveData
     */
    private fun getInfoFromAssets(
        isTS004: Boolean,
        firmware: String,
    ) {
        val apkVersionStr = if (isTS004) TS004_FIRMWARE_VERSION else TC007_FIRMWARE_VERSION
        val apkFirmwareName = if (isTS004) TS004_FIRMWARE_NAME else TC007_FIRMWARE_NAME

        val newVersion: Double = getVersionFromStr(apkVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - currentversion：$currentVersion apk内置version：$newVersion")
        if (newVersion <= currentVersion) { 
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }

        val firmwareFile = FileConfig.getFirmwareFile(apkFirmwareName)
        try {
            val application: Application = getApplication()
            val inputStream = application.assets.open(apkFirmwareName)
            val outputStream: OutputStream = FileOutputStream(firmwareFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            XLog.e("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - export内置firmwareUpgrade包failed! ${e.message}")
            FileUtils.delete(firmwareFile)
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }

        // 需求就是只需要中英两种语言，其他语言就使用英文。
        val tipsStr = getApplication<Application>().getString(R.string.fireware_update_tips)

        firmwareDataLD.postValue(FirmwareData(apkVersionStr, tipsStr, apkFirmwareName, firmwareFile.length()))
        isRequest = false
    }

    /**
     * 调interface走完整的Get/RetrievefirmwareUpgrade包info流程.
     */
    private suspend fun getInfoFromNetwork(
        isTS004: Boolean,
        sn: String,
        randomNum: String,
        firmware: String,
    ) {
        
        val bindCode = bindDevice(sn, randomNum)
        if (bindCode != LMS.SUCCESS && bindCode != 15109) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - 绑定devicefailed! sn: $sn")
            failLD.postValue(bindCode == 15162)
            isRequest = false
            return
        }

        // Get/RetrievefirmwareUpgrade包list
        val packageData: PackageData? = querySoftPackage(sn, if (isTS004) TS004_SOFT_CODE else TC007_SOFT_CODE)
        if (packageData == null) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - Get/RetrievefirmwareUpgrade包infofailed!")
            failLD.postValue(false)
            isRequest = false
            return
        }

        val record: PackageData.Record? = packageData.getFirstRecord()
        val newVersionStr: String? = record?.maxUpdateVersion
        if (record == null || newVersionStr == null) { // 没有firmwareUpgrade包，即currentfirmware已是最新
            XLog.d("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - 没有firmwareUpgrade包，即currentfirmware已是最新")
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }

        val newVersion: Double = getVersionFromStr(newVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - currentversion：$currentVersion service器version：$newVersion")
        if (newVersion <= currentVersion) { 
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }

        // Get/RetrievefirmwareUpgrade包Download地址
        val downloadData = queryDownloadUrl(sn, record.maxUpdateVersionSoftId)
        if (downloadData?.responseCode == LMS.SUCCESS) {
            firmwareDataLD.postValue(
                FirmwareData(
                    newVersionStr,
                    record.getUpdateStr(),
                    downloadData.downUrl ?: "",
                    downloadData.size ?: 0,
                ),
            )
        } else {
            XLog.w("${if (isTS004) "TS004" else "TC007"} firmwareUpgrade - Get/Retrievefirmware包Download地址failed!")
            failLD.postValue(downloadData?.responseCode == 60312)
        }
        isRequest = false
    }

    /**
     * 将device SN、Register码与current账号绑定.
     */
    private suspend fun bindDevice(
        sn: String,
        randomNum: String,
    ): Int {
        return withContext(Dispatchers.IO) {
            var code = LMS.SUCCESS
            val countDownLatch = CountDownLatch(1)
            LMS.getInstance().bindDevice(sn, randomNum, "", "") {
                code = it.code
                countDownLatch.countDown()
            }
            countDownLatch.await()
            return@withContext code
        }
    }

    /**
     * 查询指定 SN 的firmwareUpgrade包list
     */
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
            params.addBodyParameter("downloadLanguageId", LanguageUtil.getLanguageId(Utils.getApp()))
            params.addBodyParameter("downloadPlatformId", 2) // 1-IOS 2-APP 3-官网 4-PC 5-生产 6-其他
            params.addBodyParameter(
                "queryTime",
                DateUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("GMT")),
            )
            HttpProxy.instant.post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean = ResponseBean.convertCommonBean(response, null)
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

    /**
     * 查询指定 SN 指定firmwareUpgrade包的Downloadinfo.
     */
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
            params.addBodyParameter("businessType", 20) // 业务type，20-software包
            params.addBodyParameter("productType", 20) // 0-未知 10-贸易体系 20-品牌体系
            params.addBodyParameter("isCheckPoint", 0) // 0-不校验 1-校验（也不知道校验的是什么，interfacedocument没说）
            HttpProxy.instant.post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean = ResponseBean.convertCommonBean(response, null)
                            if (commonBean.code == LMS.SUCCESS) {
                                result = Gson().fromJson(commonBean.data, DownloadData::class.java)
                                result?.responseCode = commonBean.code
                            } else {
                                result = DownloadData("", 0, commonBean.code)
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

    /**
     * 用来parsing Get/RetrievefirmwareUpgrade包list interfaceReturn的data.
     */
    private class PackageData {
        var records: List<Record>? = null

        fun getFirstRecord(): Record? = if (records?.isNotEmpty() == true) records?.get(0) else null

        data class Record(
            var maxUpdateVersion: String?, // version名，如"V1.32"
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
            val valueType: Int, // 1-softwarename 2-software介绍 3-updatedescription 4-注意事项
            val textDescription: String?,
        )
    }

    /**
     * 用来parsing Get/RetrievefirmwareUpgrade包对应Downloadinfo interfaceReturndata.
     */
    private data class DownloadData(
        val downUrl: String?,
        val size: Long?,
        var responseCode: Int,
    )
}
