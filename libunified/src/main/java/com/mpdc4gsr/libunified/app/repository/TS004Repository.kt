package com.mpdc4gsr.libunified.app.repository

import android.net.Network
import com.mpdc4gsr.libunified.app.http.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.*

object TS004Repository {
    private const val BASE_URL = "http://192.168.40.1:8080"

    private fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }

    var netWork: Network? = null
        set(value) {
            field = value
            HttpClient.network = value
        }
    private val okHttpClient: OkHttpClient
        get() {
            return HttpClient
                .createClient()
                .newBuilder()
                .addInterceptor(OKLogInterceptor(false))
                .build()
        }

    private suspend inline fun <reified T> post(
        endpoint: String,
        body: Any,
    ): T =
        HttpClient.executeJsonPost(
            okHttpClient,
            "$BASE_URL$endpoint",
            body,
            T::class.java,
        )

    private suspend fun postOctet(
        endpoint: String,
        data: ByteArray,
    ) {
        HttpClient
            .executeOctetPost(
                okHttpClient,
                "$BASE_URL$endpoint",
                data,
            ).close()
    }

    suspend fun downloadList(
        dataMap: Map<String, File>,
        listener: ((path: String, isSuccess: Boolean) -> Unit),
    ): Int {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            dataMap.forEach {
                val isSuccess = download(it.key, it.value)
                launch(Dispatchers.Main) {
                    listener.invoke(it.key, isSuccess)
                }
                if (isSuccess) {
                    successCount++
                }
            }
            return@withContext successCount
        }
    }

    suspend fun download(
        url: String,
        file: File,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val responseBody =
                try {
                    HttpClient.executeGet(okHttpClient, url)
                } catch (_: Exception) {
                    return@withContext false
                }
            var inputStream: InputStream? = null
            var fileOutputString: FileOutputStream? = null
            try {
                inputStream = responseBody.byteStream()
                fileOutputString = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var readLength = inputStream.read(buffer)
                while (readLength != -1) {
                    fileOutputString.write(buffer, 0, readLength)
                    readLength = inputStream.read(buffer)
                }
                fileOutputString.flush()
                return@withContext true
            } catch (_: Exception) {
                return@withContext false
            } finally {
                inputStream?.close()
                fileOutputString?.close()
            }
        }

    suspend fun syncTime(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["year"] = calendar.get(Calendar.YEAR)
                paramMap["month"] = calendar.get(Calendar.MONTH) + 1
                paramMap["day"] = calendar.get(Calendar.DAY_OF_MONTH)
                paramMap["hour"] = calendar.get(Calendar.HOUR_OF_DAY)
                paramMap["min"] = calendar.get(Calendar.MINUTE)
                paramMap["sec"] = calendar.get(Calendar.SECOND)
                paramMap["usec"] = calendar.get(Calendar.MILLISECOND)
                post<TS004Response<Boolean>>("/api/v1/system/setDateTime", paramMap).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    suspend fun syncTimeZone(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["timezone"] = TimeZone.getDefault().rawOffset / 1000 / 60 / 60
                post<TS004Response<Boolean>>("/api/v1/system/setTimeZone", paramMap).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    suspend fun getVersion(): TS004Response<VersionBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<VersionBean>>("/api/v1/system/getVersion", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun getDeviceInfo(): TS004Response<DeviceInfo>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<DeviceInfo>>("/api/v1/system/getDeviceInfo", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun getFileCount(fileType: Int): Int? =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["fileType"] = fileType
                post<TS004Response<FileCountBean>>("/api/v1/system/getFileCount", paramMap).data?.fileCount ?: 0
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getNewestFile(fileType: Int): List<TS004FileBean>? =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["pageNum"] = 1
                paramMap["pageCount"] = 1
                paramMap["fileType"] = fileType
                post<TS004Response<FilePageBean>>("/api/v1/system/getFileList", paramMap).data?.filelist
                    ?: return@withContext ArrayList()
            } catch (_: Exception) {
                null
            }
        }

    suspend fun getAllFileList(fileType: Int): List<TS004FileBean> =
        withContext(Dispatchers.IO) {
            try {
                val fileCount = getFileCount(fileType) ?: return@withContext ArrayList()
                if (fileCount < 1) {
                    return@withContext ArrayList()
                }
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["pageNum"] = 1
                paramMap["pageCount"] = fileCount
                paramMap["fileType"] = fileType
                post<TS004Response<FilePageBean>>("/api/v1/system/getFileList", paramMap).data?.filelist ?: ArrayList()
            } catch (_: Exception) {
                ArrayList()
            }
        }

    suspend fun getFileByPage(
        fileType: Int,
        pageNum: Int,
        pageCount: Int,
    ): List<TS004FileBean>? =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["pageNum"] = pageNum
                paramMap["pageCount"] = pageCount
                paramMap["fileType"] = fileType
                post<TS004Response<FilePageBean>>("/api/v1/system/getFileList", paramMap).data?.filelist ?: ArrayList()
            } catch (_: Exception) {
                null
            }
        }

    data class IdData(
        val id: Int,
    )

    suspend fun deleteFiles(ids: Array<Int>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val idArray: Array<IdData> =
                    Array(ids.size) {
                        IdData(ids[it])
                    }
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["filelist"] = idArray
                post<TS004Response<Boolean>>("/api/v1/system/deleteFile", paramMap).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    suspend fun updateFirmware(file: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val isStartSuccess =
                    post<TS004Response<Boolean>>("/api/v1/system/remoteUpgrade", emptyMap<String, Any>()).isSuccess()
                if (!isStartSuccess) {
                    return@withContext false
                }
                val isSendStartSuccess = sendUpgradeFileStart(file)
                if (!isSendStartSuccess) {
                    return@withContext false
                }
                val isSendFileSuccess = sendUpgradeFile(file)
                if (!isSendFileSuccess) {
                    return@withContext false
                }
                val isEndSuccess = sendUpgradeFileEnd(file)
                if (!isEndSuccess) {
                    return@withContext false
                }
                var status =
                    post<TS004Response<UpgradeStatus>>(
                        "/api/v1/system/getUpgradeStatus",
                        emptyMap<String, Any>(),
                    ).data?.status
                while (status == 0 || status == 1 || status == 2) {
                    delay(1000)
                    status =
                        post<TS004Response<UpgradeStatus>>(
                            "/api/v1/system/getUpgradeStatus",
                            emptyMap<String, Any>(),
                        ).data?.status
                }
                status == 4
            } catch (_: Exception) {
                false
            }
        }

    private suspend fun sendUpgradeFileStart(file: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["saveAsFile"] = true
                paramMap["MD5"] = calculateMD5(file).lowercase(Locale.ROOT)
                paramMap["length"] = file.length()
                post<TS004Response<Boolean>>("/api/v1/system/sendUpgradeFileStart", paramMap).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    private suspend fun sendUpgradeFile(file: File): Boolean =
        withContext(Dispatchers.IO) {
            var fileInputStream: FileInputStream? = null
            try {
                fileInputStream = FileInputStream(file)
                var hasReadCount = 0
                var byteArray = ByteArray(1024 * 1024 * 5)
                var readCount = fileInputStream.read(byteArray)
                while (readCount != -1) {
                    hasReadCount += readCount
                    if (hasReadCount == 1024 * 1024 * 5) {
                        postOctet("/api/v1/system/sendUpgradeFileData", byteArray)
                        hasReadCount = 0
                        byteArray = ByteArray(1024 * 1024 * 5)
                    }
                    readCount =
                        fileInputStream.read(byteArray, hasReadCount, byteArray.size - hasReadCount)
                }
                if (hasReadCount > 0) {
                    val lastArray = ByteArray(hasReadCount)
                    System.arraycopy(byteArray, 0, lastArray, 0, hasReadCount)
                    postOctet("/api/v1/system/sendUpgradeFileData", lastArray)
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                fileInputStream?.close()
            }
        }

    private suspend fun sendUpgradeFileEnd(file: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["MD5"] = calculateMD5(file).lowercase(Locale.ROOT)
                post<TS004Response<Boolean>>("/api/v1/system/sendUpgradeFileEnd", paramMap).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    suspend fun setPseudoColor(mode: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["enable"] = false
                paramMap["mode"] = mode
                post<TS004Response<Boolean>>("/api/v1/system/setPseudoColor", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getPseudoColor(): TS004Response<PseudoColorBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<PseudoColorBean>>("/api/v1/system/getPseudoColor", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun setRangeFind(state: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["state"] = state
                post<TS004Response<Boolean>>("/api/v1/system/setRangeFind", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getRangeFind(): TS004Response<RangeBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<RangeBean>>("/api/v1/system/getRangeFind", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun setPanelParam(brightness: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["brightness"] = brightness
                post<TS004Response<Boolean>>("/api/v1/system/setPanelParam", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getPanelParam(): TS004Response<BrightnessBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<BrightnessBean>>("/api/v1/system/getPanelParam", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun setPip(enable: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["enable"] = enable
                post<TS004Response<Boolean>>("/api/v1/system/setPip", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getPip(): TS004Response<PipBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<PipBean>>("/api/v1/system/getPip", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun setZoom(factor: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["enable"] = true
                paramMap["factor"] = factor
                post<TS004Response<Boolean>>("/api/v1/system/setZoom", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getZoom(): TS004Response<ZoomBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<ZoomBean>>("/api/v1/system/getZoom", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun setSnapshot(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<Boolean>>("/api/v1/system/snapshot", emptyMap<String, Any>()).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun setVideo(enable: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["enable"] = enable
                post<TS004Response<Boolean>>("/api/v1/system/vrecord", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getRecordStatus(): TS004Response<RecordStatusBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<RecordStatusBean>>("/api/v1/system/getRecordStatus", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }

    suspend fun getFreeSpace(): FreeSpaceBean? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<FreeSpaceBean>>("/api/v1/system/getFreeSpace", emptyMap<String, Any>()).data
            } catch (_: Exception) {
                null
            }
        }

    suspend fun getFormatStorage(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<Boolean>>("/api/v1/system/formatStorage", emptyMap<String, Any>()).isSuccess()
            } catch (_: Exception) {
                false
            }
        }

    suspend fun getResetAll(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<Boolean>>("/api/v1/system/resetAll", emptyMap<String, Any>()).status == 100
            } catch (_: Exception) {
                false
            }
        }

    suspend fun setTISR(state: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val paramMap: HashMap<String, Any> = HashMap()
                paramMap["state"] = state
                post<TS004Response<Boolean>>("/api/v1/system/setTISR", paramMap).isSuccess()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getTISR(): TS004Response<TISRBean>? =
        withContext(Dispatchers.IO) {
            try {
                post<TS004Response<TISRBean>>("/api/v1/system/getTISR", emptyMap<String, Any>())
            } catch (_: Exception) {
                null
            }
        }
}
