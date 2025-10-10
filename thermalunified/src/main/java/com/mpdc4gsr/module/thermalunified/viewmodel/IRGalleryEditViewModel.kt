package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.libunified.app.utils.UnifiedByteUtils.bytesToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class IRGalleryEditViewModel : BaseViewModel() {
    val resultLiveData = SingleLiveEvent<FrameBean>()

    fun initData(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) {
                return@launch
            }
            val bytes = file.readBytes()
            val headLenBytes = ByteArray(2)
            System.arraycopy(bytes, 0, headLenBytes, 0, 2)
            val headLen = headLenBytes.bytesToInt()
            val headDataBytes = ByteArray(headLen)
            val frameDataBytes = ByteArray(bytes.size - headLen)
            System.arraycopy(bytes, 0, headDataBytes, 0, headDataBytes.size)
            System.arraycopy(bytes, headLen, frameDataBytes, 0, frameDataBytes.size)
            resultLiveData.postValue(FrameBean(headDataBytes, frameDataBytes))
        }
    }

    fun getTailData(bytes: ByteArray) {
    }

    data class FrameBean(
        val capital: ByteArray,
        val frame: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FrameBean
            if (!capital.contentEquals(other.capital)) return false
            if (!frame.contentEquals(other.frame)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = capital.contentHashCode()
            result = 31 * result + frame.contentHashCode()
            return result
        }
    }
}
