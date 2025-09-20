package com.mpdc4gsr.lib.core.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

// Simplified FirmwareViewModel with TS004/TC007 functionality removed
class FirmwareViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        // TS004/TC007 constants removed - functionality disabled
        private const val USE_DEBUG_SN = false
    }

    val firmwareDataLD = MutableLiveData<FirmwareData?>()
    val failLD = MutableLiveData<Boolean>()
    var isRequest = false

    data class FirmwareData(
        val version: String?,
        val tipsStr: String?,
        val fileName: String,
        val size: Long = 0,
    )

    fun queryFirmware(isTS004: Boolean = false) {
        if (isRequest) return
        isRequest = true

        // TS004/TC007 functionality removed
        failLD.postValue(false)
        isRequest = false
    }

    private fun getVersionFromStr(versionStr: String?): Double {
        return try {
            versionStr?.replace("V", "")?.replace(".", "")?.toDouble() ?: 1.0
        } catch (e: NumberFormatException) {
            1.0
        }
    }
}