package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat

class StorageSpaceViewModel : BaseViewModel() {
    companion object {
        private const val MOCK_TOTAL_SPACE = 32_000_000_000L
        private const val MOCK_USED_SPACE = 12_000_000_000L
        private const val MOCK_FREE_SPACE = 20_000_000_000L
        private const val MOCK_PHOTO_SPACE = 5_000_000_000L
        private const val MOCK_VIDEO_SPACE = 6_000_000_000L
        private const val MOCK_SYSTEM_SPACE = 1_000_000_000L
    }

    data class StorageInfo(
        val totalSpace: Long = 0L,
        val usedSpace: Long = 0L,
        val freeSpace: Long = 0L,
        val photoSpace: Long = 0L,
        val videoSpace: Long = 0L,
        val systemSpace: Long = 0L,
    )

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    fun loadStorageInfo() {
        launchWithErrorHandling {
            // Original TS004Repository functionality removed - use mock data
            val mockStorageInfo =
                StorageInfo(
                    totalSpace = MOCK_TOTAL_SPACE,
                    usedSpace = MOCK_USED_SPACE,
                    freeSpace = MOCK_FREE_SPACE,
                    photoSpace = MOCK_PHOTO_SPACE,
                    videoSpace = MOCK_VIDEO_SPACE,
                    systemSpace = MOCK_SYSTEM_SPACE,
                )
            _storageInfo.value = mockStorageInfo
        }
    }

    fun getUsagePercentage(): Float {
        val info = _storageInfo.value
        return if (info.totalSpace > 0) {
            (info.usedSpace.toFloat() / info.totalSpace.toFloat())
        } else {
            0f
        }
    }

    fun formatFileSize(fileSize: Long): String =
        when {
            fileSize == 0L -> "0 B"
            fileSize < 1024 -> DecimalFormat("#.0").format(fileSize.toDouble()) + " B"
            fileSize < 1048576 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1024) + " KB"
            fileSize < 1073741824 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1048576) + " MB"
            else -> DecimalFormat("#.0").format(fileSize.toDouble() / 1073741824) + " GB"
        }

    fun formatStorage() {
        launchWithErrorHandling {
            // Original format storage operation removed - just show confirmation
            // In real implementation, this would format the storage
        }
    }
}
