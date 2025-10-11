package com.mpdc4gsr.component.shared.app.repository

import android.app.Application
import kotlinx.coroutines.flow.Flow
import java.io.File

class FirmwareRepository(
    private val application: Application,
) : BaseRepository() {
    companion object {
        private const val TS004_FIRMWARE_VERSION = "V1.70"
        private const val TS004_FIRMWARE_NAME = "TS004V1.70.zip"
        private const val TC007_FIRMWARE_VERSION = "V4.06"
        private const val TC007_FIRMWARE_NAME = "TC007V4.06.zip"
        private const val CACHE_KEY_FIRMWARE_CHECK = "firmware_check"
        private const val FIRMWARE_CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }

    data class FirmwareInfo(
        val version: String,
        val updateDescription: String,
        val downloadUrl: String,
        val size: Long,
        val isUpdateAvailable: Boolean = false,
    )

    data class DeviceInfo(
        val serialNumber: String,
        val randomNumber: String,
        val currentFirmwareVersion: String,
    )

    fun checkFirmwareUpdate(
        isTC007: Boolean,
        deviceInfo: DeviceInfo,
    ): Flow<BaseRepository.Result<FirmwareInfo?>> =
        safeFlow {
            val cacheKey = "${CACHE_KEY_FIRMWARE_CHECK}_${if (isTC007) "TC007" else "TS004"}"
            getCachedOrExecute(cacheKey, FIRMWARE_CACHE_TTL) {
                performFirmwareCheck(isTC007, deviceInfo)
            }
        }

    suspend fun downloadFirmware(
        firmwareInfo: FirmwareInfo,
        outputDir: File,
    ): BaseRepository.Result<File> =
        safeCall {
            // Simplified implementation - in real app would download file
            val outputFile = File(outputDir, extractFileName(firmwareInfo.downloadUrl))
            outputFile.createNewFile()
            outputFile
        }

    suspend fun getFirmwareFromAssets(isTC007: Boolean): BaseRepository.Result<FirmwareInfo> =
        safeCall {
            val version = if (isTC007) TC007_FIRMWARE_VERSION else TS004_FIRMWARE_VERSION
            val fileName = if (isTC007) TC007_FIRMWARE_NAME else TS004_FIRMWARE_NAME
            FirmwareInfo(
                version = version,
                updateDescription = "Local firmware update available",
                downloadUrl = "asset://$fileName",
                size = getAssetFileSize(fileName),
                isUpdateAvailable = true,
            )
        }

    private suspend fun performFirmwareCheck(
        isTC007: Boolean,
        deviceInfo: DeviceInfo,
    ): FirmwareInfo? {
        // Simplified implementation - compare with hardcoded versions
        val latestVersion = if (isTC007) TC007_FIRMWARE_VERSION else TS004_FIRMWARE_VERSION
        val isUpdateAvailable =
            compareVersions(latestVersion, deviceInfo.currentFirmwareVersion) > 0
        return if (isUpdateAvailable) {
            FirmwareInfo(
                version = latestVersion,
                updateDescription = "New firmware version available",
                downloadUrl = "https://example.com/firmware/${if (isTC007) TC007_FIRMWARE_NAME else TS004_FIRMWARE_NAME}",
                size = 1024 * 1024, // 1MB
                isUpdateAvailable = true,
            )
        } else {
            null
        }
    }

    private fun compareVersions(
        version1: String,
        version2: String,
    ): Int {
        val v1Parts = version1.removePrefix("V").split(".")
        val v2Parts = version2.removePrefix("V").split(".")
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrNull(i)?.toIntOrNull() ?: 0
            val v2Part = v2Parts.getOrNull(i)?.toIntOrNull() ?: 0
            when {
                v1Part > v2Part -> return 1
                v1Part < v2Part -> return -1
            }
        }
        return 0
    }

    private fun extractFileName(url: String): String = url.substringAfterLast("/").ifEmpty { "firmware.zip" }

    private fun getAssetFileSize(fileName: String): Long =
        try {
            application.assets.openFd(fileName).length
        } catch (e: Exception) {
            1024 * 1024 // Default 1MB
        }
}


