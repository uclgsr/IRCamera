// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel\libunified_src_main_java_com_mpdc4gsr_libunified_app_viewmodel_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel\FirmwareViewModel.kt =====

package com.mpdc4gsr.libunified.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.utils.DateUtils
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch

class FirmwareViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TS004_SOFT_CODE = "TS004_FirmwareSW_Scope"
        private const val TC007_SOFT_CODE = "TC007_FirmwareSW_Wireless"
        private const val TS004_FIRMWARE_VERSION = "V1.70"
        private const val TS004_FIRMWARE_NAME = "TS004V1.70.zip"
        private const val TC007_FIRMWARE_VERSION = "V4.06"
        private const val TC007_FIRMWARE_NAME = "TC007V4.06.zip"
        private const val USE_DEBUG_SN = false
        private const val TS004_DEBUG_SN = "1D003655A10016"
        private const val TS004_DEBUG_RANDOM_NUM = "8D2N01"
        private const val TC007_DEBUG_SN = "1D004714E10002"
        private const val TC007_DEBUG_RANDOM_NUM = "EN6L6Q"
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

    fun queryFirmware(isTS004: Boolean) {
        if (isRequest) {
            return
        }
    }

    private fun getInfoFromAssets(
        isTS004: Boolean,
        firmware: String,
    ) {
        val apkVersionStr = if (isTS004) TS004_FIRMWARE_VERSION else TC007_FIRMWARE_VERSION
        val apkFirmwareName = if (isTS004) TS004_FIRMWARE_NAME else TC007_FIRMWARE_NAME
        val newVersion: Double = getVersionFromStr(apkVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - current[ph][ph]ï¼š$currentVersion apk[ph][ph][ph][ph]ï¼š$newVersion")
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
            XLog.e("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]! ${e.message}")
            firmwareFile.delete()
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val tipsStr = getApplication<Application>().getString(R.string.fireware_update_tips)
        firmwareDataLD.postValue(
            FirmwareData(
                apkVersionStr,
                tipsStr,
                apkFirmwareName,
                firmwareFile.length()
            )
        )
        isRequest = false
    }

    private suspend fun getInfoFromNetwork(
        isTS004: Boolean,
        sn: String,
        randomNum: String,
        firmware: String,
    ) {
        val bindCode = bindDevice(sn, randomNum)
        if (bindCode != LMS.SUCCESS.toInt() && bindCode != 15109) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph]! sn: $sn")
            failLD.postValue(bindCode == 15162)
            isRequest = false
            return
        }
        val packageData: PackageData? =
            querySoftPackage(sn, if (isTS004) TS004_SOFT_CODE else TC007_SOFT_CODE)
        if (packageData == null) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]!")
            failLD.postValue(false)
            isRequest = false
            return
        }
        val record: PackageData.Record? = packageData.getFirstRecord()
        val newVersionStr: String? = record?.maxUpdateVersion
        if (record == null || newVersionStr == null) {
            XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph]current[ph][ph][ph][ph][ph][ph]")
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val newVersion: Double = getVersionFromStr(newVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - current[ph][ph]ï¼š$currentVersion [ph][ph][ph][ph][ph]ï¼š$newVersion")
        if (newVersion <= currentVersion) {
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val downloadData = queryDownloadUrl(sn, record.maxUpdateVersionSoftId)
        if (downloadData?.responseCode == LMS.SUCCESS.toInt()) {
            firmwareDataLD.postValue(
                FirmwareData(
                    newVersionStr,
                    record.getUpdateStr(),
                    downloadData.downUrl ?: "",
                    downloadData.size ?: 0,
                ),
            )
        } else {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]!")
            failLD.postValue(downloadData?.responseCode == 60312)
        }
        isRequest = false
    }

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
            val url = UrlConstants.BASE_URL + "api/v1/user/deviceSoftOut/page"
            val params = RequestParams()
            params.addBodyParameter("sn", sn)
            params.addBodyParameter("softCode", softCode)
            params.addBodyParameter(
                "downloadLanguageId",
                LanguageUtils.getLanguageId(ContextProvider.getContext())
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
            val url = UrlConstants.BASE_URL + "api/v1/user/deviceSoftOut/getFileUrl"
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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel\ModernFirmwareViewModel.kt =====

package com.mpdc4gsr.libunified.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.repository.FirmwareRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ModernFirmwareViewModel(
    application: Application,
    private val firmwareRepository: FirmwareRepository = FirmwareRepository(application)
) : AndroidViewModel(application) {
    // State management
    private val _firmwareState = MutableStateFlow<FirmwareState>(FirmwareState.Idle)
    val firmwareState: StateFlow<FirmwareState> = _firmwareState.asStateFlow()
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // One-time events
    private val _events = MutableSharedFlow<FirmwareEvent>()
    val events: SharedFlow<FirmwareEvent> = _events.asSharedFlow()

    // Sealed classes for type-safe state management
    sealed class FirmwareState {
        object Idle : FirmwareState()
        object Checking : FirmwareState()
        data class UpdateAvailable(val info: FirmwareRepository.FirmwareInfo) : FirmwareState()
        object UpToDate : FirmwareState()
        data class Error(val message: String, val isBindError: Boolean = false) : FirmwareState()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    sealed class FirmwareEvent {
        data class ShowUpdateDialog(val info: FirmwareRepository.FirmwareInfo) : FirmwareEvent()
        data class ShowError(val message: String) : FirmwareEvent()
        data class ShowSuccess(val message: String) : FirmwareEvent()
        object UpdateCompleted : FirmwareEvent()
    }

    // Public API for checking firmware updates
    fun checkFirmwareUpdate(isTC007: Boolean, deviceInfo: FirmwareRepository.DeviceInfo) {
        viewModelScope.launch {
            _firmwareState.value = FirmwareState.Checking
            firmwareRepository.checkFirmwareUpdate(isTC007, deviceInfo).collect { result ->
                when (result) {
                    is BaseRepository.Result.Loading -> {
                        _firmwareState.value = FirmwareState.Checking
                    }

                    is BaseRepository.Result.Success -> {
                        val firmwareInfo = result.data
                        if (firmwareInfo != null && firmwareInfo.isUpdateAvailable) {
                            _firmwareState.value = FirmwareState.UpdateAvailable(firmwareInfo)
                            _events.emit(FirmwareEvent.ShowUpdateDialog(firmwareInfo))
                        } else {
                            _firmwareState.value = FirmwareState.UpToDate
                            _events.emit(FirmwareEvent.ShowSuccess("Firmware is up to date"))
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _firmwareState.value = FirmwareState.Error(errorMessage)
                        _events.emit(FirmwareEvent.ShowError(errorMessage))
                    }
                }
            }
        }
    }

    // Download firmware update
    fun downloadFirmwareUpdate(firmwareInfo: FirmwareRepository.FirmwareInfo, outputDir: File) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0f)
            when (val result = firmwareRepository.downloadFirmware(firmwareInfo, outputDir)) {
                is BaseRepository.Result.Success -> {
                    _downloadState.value = DownloadState.Completed(result.data)
                    _events.emit(FirmwareEvent.ShowSuccess("Firmware downloaded successfully"))
                    _events.emit(FirmwareEvent.UpdateCompleted)
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Download failed"
                    _downloadState.value = DownloadState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Get firmware from assets as fallback
    fun getFirmwareFromAssets(isTC007: Boolean) {
        viewModelScope.launch {
            when (val result = firmwareRepository.getFirmwareFromAssets(isTC007)) {
                is BaseRepository.Result.Success -> {
                    _firmwareState.value = FirmwareState.UpdateAvailable(result.data)
                    _events.emit(FirmwareEvent.ShowUpdateDialog(result.data))
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Failed to load local firmware"
                    _firmwareState.value = FirmwareState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Error handling
    fun onBindError() {
        _firmwareState.value = FirmwareState.Error("Service binding failed", true)
        viewModelScope.launch {
            _events.emit(FirmwareEvent.ShowError("Firmware service is not available"))
        }
    }

    // Clear error state
    fun clearError() {
        if (_firmwareState.value is FirmwareState.Error) {
            _firmwareState.value = FirmwareState.Idle
        }
        if (_downloadState.value is DownloadState.Error) {
            _downloadState.value = DownloadState.Idle
        }
    }

    // Reset states
    fun reset() {
        _firmwareState.value = FirmwareState.Idle
        _downloadState.value = DownloadState.Idle
    }

    // Compatibility with legacy FirmwareData
    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long
    ) {
        companion object {
            fun fromFirmwareInfo(info: FirmwareRepository.FirmwareInfo): FirmwareData {
                return FirmwareData(
                    version = info.version,
                    updateStr = info.updateDescription,
                    downUrl = info.downloadUrl,
                    size = info.size
                )
            }
        }
    }

    // Convert to legacy format for compatibility
    fun getFirmwareDataLegacy(): FirmwareData? {
        val state = _firmwareState.value
        return if (state is FirmwareState.UpdateAvailable) {
            FirmwareData.fromFirmwareInfo(state.info)
        } else {
            null
        }
    }
}