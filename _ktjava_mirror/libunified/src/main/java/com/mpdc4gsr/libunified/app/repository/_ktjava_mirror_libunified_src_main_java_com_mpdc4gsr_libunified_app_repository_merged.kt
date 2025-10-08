// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\libunified_src_main_java_com_mpdc4gsr_libunified_app_repository_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\repository' subtree
// Files: 10; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\BaseRepository.kt =====

package com.mpdc4gsr.libunified.app.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

abstract class BaseRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Throwable) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }

    data class CachedData<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val ttlMs: Long = DEFAULT_CACHE_TTL
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMs
    }

    // Simple in-memory cache
    private val cache = mutableMapOf<String, CachedData<Any>>()
    protected suspend fun <T> safeCall(
        operation: suspend () -> T
    ): Result<T> {
        return withContext(ioDispatcher) {
            try {
                val result = operation()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    protected fun <T> safeFlow(
        operation: suspend () -> T
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        try {
            val result = operation()
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)

    // The unchecked cast from CachedData<Any> to CachedData<T> is safe here because
    // each cacheKey is always associated with a single type T for the lifetime of the cache entry.
    // The function contract ensures that the same key is not reused for different types.
    @Suppress("UNCHECKED_CAST")
    protected suspend fun <T> getCachedOrExecute(
        cacheKey: String,
        ttlMs: Long = DEFAULT_CACHE_TTL,
        operation: suspend () -> T
    ): T {
        val cached = cache[cacheKey] as? CachedData<T>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else {
            val result = operation()
            cache[cacheKey] = CachedData(result as Any, ttlMs = ttlMs)
            result
        }
    }

    protected fun clearCache(key: String? = null) {
        if (key != null) {
            cache.remove(key)
        } else {
            cache.clear()
        }
    }

    protected fun <T> networkBoundResource(
        query: () -> Flow<T?>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit,
        shouldFetch: (T?) -> Boolean = { true }
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        val data = query().collect { localData ->
            if (shouldFetch(localData)) {
                try {
                    val networkData = fetch()
                    saveFetchResult(networkData)
                    emit(Result.Success(networkData))
                } catch (e: Exception) {
                    if (localData != null) {
                        emit(Result.Success(localData))
                    } else {
                        emit(Result.Error(e))
                    }
                }
            } else if (localData != null) {
                emit(Result.Success(localData))
            }
        }
    }.flowOn(ioDispatcher)

    companion object {
        private const val DEFAULT_CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\FileBean.kt =====

package com.mpdc4gsr.libunified.app.repository

import android.os.Parcel
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.libunified.app.tools.VideoTools
import java.io.File
import java.util.*

open class FileBean(
    val id: Int,
    val path: String,
    val thumb: String,
    val name: String,
    val duration: Long,
    val timeMillis: Long,
    var hasDownload: Boolean,
) : Parcelable {
    constructor(file: File) : this(
        id = 0,
        path = file.absolutePath,
        thumb = file.absolutePath,
        name = file.name,
        duration = VideoTools.getLocalVideoDuration(file.absolutePath),
        timeMillis = TimeTools.updateDateTime(file),
        hasDownload = true,
    )

    constructor(isVideo: Boolean, fileBean: TS004FileBean) : this(
        id = fileBean.id,
        path = "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        thumb = if (isVideo) "http://192.168.40.1:8080/DCIM/${fileBean.thumb}" else "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        name = fileBean.name,
        duration = fileBean.duration * 1000L,
        timeMillis = fileBean.time * 1000 - TimeZone.getDefault().getOffset(fileBean.time * 1000),
        hasDownload = File(FileConfig.ts004GalleryDir, fileBean.name).exists(),
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(path)
        dest.writeString(thumb)
        dest.writeString(name)
        dest.writeLong(duration)
        dest.writeLong(timeMillis)
        dest.writeByte(if (hasDownload) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<FileBean> {
        override fun createFromParcel(parcel: Parcel): FileBean {
            return FileBean(
                id = parcel.readInt(),
                path = parcel.readString() ?: "",
                thumb = parcel.readString() ?: "",
                name = parcel.readString() ?: "",
                duration = parcel.readLong(),
                timeMillis = parcel.readLong(),
                hasDownload = parcel.readByte() != 0.toByte()
            )
        }

        override fun newArray(size: Int): Array<FileBean?> {
            return arrayOfNulls(size)
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\FirmwareRepository.kt =====

package com.mpdc4gsr.libunified.app.repository

import android.app.Application
import kotlinx.coroutines.flow.Flow
import java.io.File

class FirmwareRepository(
    private val application: Application
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
        val isUpdateAvailable: Boolean = false
    )

    data class DeviceInfo(
        val serialNumber: String,
        val randomNumber: String,
        val currentFirmwareVersion: String
    )

    fun checkFirmwareUpdate(
        isTC007: Boolean,
        deviceInfo: DeviceInfo
    ): Flow<BaseRepository.Result<FirmwareInfo?>> = safeFlow {
        val cacheKey = "${CACHE_KEY_FIRMWARE_CHECK}_${if (isTC007) "TC007" else "TS004"}"
        getCachedOrExecute(cacheKey, FIRMWARE_CACHE_TTL) {
            performFirmwareCheck(isTC007, deviceInfo)
        }
    }

    suspend fun downloadFirmware(
        firmwareInfo: FirmwareInfo,
        outputDir: File
    ): BaseRepository.Result<File> = safeCall {
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
                isUpdateAvailable = true
            )
        }

    private suspend fun performFirmwareCheck(
        isTC007: Boolean,
        deviceInfo: DeviceInfo
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
                isUpdateAvailable = true
            )
        } else {
            null
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
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

    private fun extractFileName(url: String): String {
        return url.substringAfterLast("/").ifEmpty { "firmware.zip" }
    }

    private fun getAssetFileSize(fileName: String): Long {
        return try {
            application.assets.openFd(fileName).length
        } catch (e: Exception) {
            1024 * 1024 // Default 1MB
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\GalleryRepository.kt =====

package com.mpdc4gsr.libunified.app.repository

import android.content.ContentResolver
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object GalleryRepository {
    enum class DirType {
        LINE,
        TC007,
        TS004_LOCALE,
        TS004_REMOTE,
    }

    private fun copySourDir(
        sourceDir: File,
        targetDir: File,
    ): Boolean {
        return try {
            if (!sourceDir.exists()) {
                return false
            }
            if (!sourceDir.isDirectory) {
                return false
            }
            val fileList = sourceDir.listFiles()
            if (fileList?.isEmpty() == true) {
                return false
            }
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            fileList?.forEach {
                val path = sourceDir.absolutePath + File.separator + it.name
                copyPictureFile(path, targetDir.absolutePath + File.separator + it.name)
            }
            return true
        } catch (ex: Exception) {
            false
        }
    }

    private fun copyPictureFile(
        oldPath: String,
        newPath: String,
    ): Boolean {
        return try {
            val streamFrom: InputStream = FileInputStream(oldPath)
            val streamTo: OutputStream = FileOutputStream(newPath)
            val buffer = ByteArray(1024)
            var len: Int
            while (streamFrom.read(buffer).also { len = it } > 0) {
                streamTo.write(buffer, 0, len)
            }
            streamFrom.close()
            streamTo.close()
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun readLatest(dirType: DirType): String {
        var firstPath = ""
        try {
            val path =
                if (dirType == DirType.LINE) FileConfig.lineGalleryDir else FileConfig.tc007GalleryDir
            val dirFile = File(path)
            if (dirFile.isDirectory) {
                val files = dirFile.listFiles()!!
                files.sortByDescending {
                    it.lastModified()
                }
                if (files.isNotEmpty()) {
                    firstPath = "${path}${File.separator}${files.first().name}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            XLog.e(": ${e.message}")
            return ""
        }
        return firstPath
    }

    suspend fun loadByPage(
        isVideo: Boolean,
        dirType: DirType,
        pageNum: Int,
        pageCount: Int,
    ): ArrayList<GalleryBean>? {
        return withContext(Dispatchers.IO) {
            val resultList: ArrayList<GalleryBean> = ArrayList()
            if (dirType == DirType.TS004_REMOTE) {
                // TS004Repository functionality removed
                return@withContext null
            } else {
                try {
                    val allFileList = loadAllLocale(isVideo, dirType)
                    val startIndex = pageNum * pageCount - pageCount
                    val endIndex = pageNum * pageCount
                    for (i in startIndex until endIndex) {
                        if (i >= allFileList.size) {
                            break
                        }
                        resultList.add(GalleryBean(allFileList[i]))
                    }
                    if (resultList.isNotEmpty()) {
                        resultList.sortByDescending {
                            it.timeMillis
                        }
                    }
                } catch (e: Exception) {
                    XLog.e(": ${e.message}")
                }
            }
            return@withContext resultList
        }
    }

    suspend fun loadAllReportImg(dirType: DirType): ArrayList<GalleryBean> =
        withContext(Dispatchers.IO) {
            val resultList: ArrayList<GalleryBean> = ArrayList()
            try {
                val allFileList = loadAllLocale(false, dirType)
                allFileList.forEach {
                    resultList.add(GalleryBean(it))
                }
                if (resultList.isNotEmpty()) {
                    resultList.sortByDescending {
                        it.timeMillis
                    }
                }
            } catch (e: Exception) {
                XLog.e(": ${e.message}")
            }
            return@withContext resultList
        }

    private fun loadAllLocale(
        isVideo: Boolean,
        dirType: DirType,
    ): ArrayList<File> {
        if (dirType == DirType.LINE) {
            val sourFile = File(FileConfig.gallerySourDir)
            if (sourFile.exists()) {
                val isSuccess = copySourDir(sourFile, File(FileConfig.lineGalleryDir))
                if (isSuccess) {
                    sourFile.deleteRecursively()
                    MediaScannerConnection.scanFile(
                        ContextProvider.getContext(),
                        arrayOf(FileConfig.lineGalleryDir),
                        null,
                        null
                    )
                }
            }
        }
        val dirFile =
            when (dirType) {
                DirType.LINE -> File(FileConfig.lineGalleryDir)
                DirType.TC007 -> File(FileConfig.tc007GalleryDir)
                else -> File(FileConfig.ts004GalleryDir)
            }
        var files = dirFile.listFiles { pathname -> pathname?.isFile == true }
        if (files.isNullOrEmpty()) {
            files = loadAllLocaleByMediaStore(dirType)
        }
        val resultList: ArrayList<File> = ArrayList(files.size)
        files.forEach {
            if (it.name.endsWith(if (isVideo) "MP4" else "JPG", true)) {
                resultList.add(it)
            }
        }
        resultList.sortByDescending {
            it.lastModified()
        }
        return resultList
    }

    private fun loadAllLocaleByMediaStore(dirType: DirType): Array<out File> {
        val tc001Files: MutableList<File> = ArrayList()
        val projection =
            arrayOf(
                MediaStore.Images.Media.DATA,
            )
        val selection = MediaStore.Images.Media.DATA + " LIKE ?"
        val path =
            when (dirType) {
                DirType.LINE -> "%DCIM/${CommUtils.getAppName()}%"
                DirType.TC007 -> "%DCIM/TC007%"
                else -> "%DCIM/TS004%"
            }
        val selectionArgs = arrayOf(path)
        val contentResolver: ContentResolver = ContextProvider.getContext().contentResolver
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor =
            contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                null,
            )
        cursor?.use {
            while (it.moveToNext()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val file = File(filePath)
                tc001Files.add(file)
            }
        }
        return tc001Files.toTypedArray()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\OKLogInterceptor.kt =====

package com.mpdc4gsr.libunified.app.repository

import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

class OKLogInterceptor(val isTC007: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (BuildConfig.DEBUG) {
            XLog.tag("RetrofitLog").i("--> ${request.method} ${request.url}")
            val requestBody = request.body
            val contentType = requestBody?.contentType()?.toString()
            if (requestBody != null && (contentType == null || contentType == "application/json")) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                XLog.tag("RetrofitLog").v("[ph][ph]ï¼š${buffer.readString(StandardCharsets.UTF_8)}")
            }
        }
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                XLog.tag("RetrofitLog").e("<-- HTTP FAILED: $e")
            }
            throw e
        }
        if (BuildConfig.DEBUG) {
            XLog.tag(
                "RetrofitLog",
            )
                .i("<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url}")
            val responseBody = response.body
            val contentType = response.headers["Content-Type"]
            @Suppress("SENSELESS_COMPARISON")
            if (responseBody != null && (isTC007 || contentType == null || contentType == "application/json")) {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val responseStr = source.buffer.clone().readString(StandardCharsets.UTF_8)
                if (responseStr.length > 1024) {
                    XLog.tag("RetrofitLog")
                        .v(
                            "[ph][ph]ï¼š${
                                responseStr.substring(
                                    0,
                                    1024
                                )
                            } ...[ph][ph][ph][ph][ph][ph][ph]"
                        )
                } else {
                    XLog.tag("RetrofitLog").v("[ph][ph]ï¼š$responseStr")
                }
            }
        }
        return response
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\ProductBean.kt =====

package com.mpdc4gsr.libunified.app.repository

// ProductBean data class to replace removed TC007 functionality
data class ProductBean(
    val ProductName: String = "",
    val ProductPN: String = "",
    val ProductSN: String = "",
    val Code: String = "",
    val SoftwareVersion: Version07Bean? = null
) {
    fun getVersionStr(): String =
        "${SoftwareVersion?.Major ?: "-"}.${SoftwareVersion?.Minor ?: "-"}${SoftwareVersion?.Build ?: "-"}"
}

data class Version07Bean(
    val Major: String? = "",
    val Minor: String? = "",
    val Build: String? = ""
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\ReportRepository.kt =====

package com.mpdc4gsr.libunified.app.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class ReportRepository : BaseRepository() {
    private val reportCache = ConcurrentHashMap<String, CachedReportData>()

    data class ReportData(
        val id: String,
        val title: String,
        val content: String,
        val timestamp: Long,
        val type: ReportType,
        val status: ReportStatus
    )

    enum class ReportType { GSR, THERMAL, COMBINED, ANALYSIS }
    enum class ReportStatus { DRAFT, PROCESSING, COMPLETED, ERROR }
    data class CachedReportData(
        val data: List<ReportData>,
        val cachedAt: Long,
        val page: Int
    )

    fun getReports(
        isTC007: Boolean,
        page: Int,
        pageSize: Int = 20
    ): Flow<BaseRepository.Result<List<ReportData>>> = safeFlow {
        val cacheKey = "reports_${if (isTC007) "tc007" else "ts004"}_$page"
        val cached = reportCache[cacheKey]
        // Return cached data if valid
        if (cached != null && System.currentTimeMillis() - cached.cachedAt < 60000) {
            return@safeFlow cached.data
        }
        // Simulate network call
        delay(1000)
        val reports = generateSampleReports(isTC007, page, pageSize)
        // Cache the results
        reportCache[cacheKey] = CachedReportData(
            data = reports,
            cachedAt = System.currentTimeMillis(),
            page = page
        )
        reports
    }

    private fun generateSampleReports(
        isTC007: Boolean,
        page: Int,
        pageSize: Int
    ): List<ReportData> {
        val deviceType = if (isTC007) "TC007" else "TS004"
        return (1..pageSize).map { index ->
            val id = "${page * pageSize + index}"
            ReportData(
                id = id,
                title = "$deviceType Report #$id",
                content = "Sample report content for $deviceType device",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                type = if (isTC007) ReportType.THERMAL else ReportType.GSR,
                status = ReportStatus.COMPLETED
            )
        }
    }

    fun clearCache() {
        reportCache.clear()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\TS004Repository.kt =====

package com.mpdc4gsr.libunified.app.repository

import android.net.Network
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.http.HttpClient
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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
            return HttpClient.createClient().newBuilder()
                .addInterceptor(OKLogInterceptor(false))
                .build()
        }

    private suspend inline fun <reified T> post(
        endpoint: String,
        body: Any
    ): T {
        return HttpClient.executeJsonPost(
            okHttpClient,
            "$BASE_URL$endpoint",
            body,
            T::class.java
        )
    }

    private suspend fun postOctet(
        endpoint: String,
        data: ByteArray
    ) {
        HttpClient.executeOctetPost(
            okHttpClient,
            "$BASE_URL$endpoint",
            data
        ).close()
    }

    suspend fun downloadList(
        dataMap: Map<String, File>,
        listener: ((path: String, isSuccess: Boolean) -> Unit)
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

    suspend fun download(url: String, file: File): Boolean = withContext(Dispatchers.IO) {
        val responseBody = try {
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

    suspend fun syncTime(): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun syncTimeZone(): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["timezone"] = TimeZone.getDefault().rawOffset / 1000 / 60 / 60
            post<TS004Response<Boolean>>("/api/v1/system/setTimeZone", paramMap).isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getVersion(): TS004Response<VersionBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<VersionBean>>("/api/v1/system/getVersion", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getDeviceInfo(): TS004Response<DeviceInfo>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<DeviceInfo>>("/api/v1/system/getDeviceInfo", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getFileCount(fileType: Int): Int? = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["fileType"] = fileType
            post<TS004Response<FileCountBean>>("/api/v1/system/getFileCount", paramMap).data?.fileCount ?: 0
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getNewestFile(fileType: Int): List<TS004FileBean>? = withContext(Dispatchers.IO) {
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

    suspend fun getAllFileList(fileType: Int): List<TS004FileBean> = withContext(Dispatchers.IO) {
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

    suspend fun getFileByPage(fileType: Int, pageNum: Int, pageCount: Int): List<TS004FileBean>? =
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

    data class IdData(val id: Int)

    suspend fun deleteFiles(ids: Array<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val idArray: Array<IdData> = Array(ids.size) {
                IdData(ids[it])
            }
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["filelist"] = idArray
            post<TS004Response<Boolean>>("/api/v1/system/deleteFile", paramMap).isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateFirmware(file: File): Boolean = withContext(Dispatchers.IO) {
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
            var status = post<TS004Response<UpgradeStatus>>(
                "/api/v1/system/getUpgradeStatus",
                emptyMap<String, Any>()
            ).data?.status
            while (status == 0 || status == 1 || status == 2) {
                delay(1000)
                status = post<TS004Response<UpgradeStatus>>(
                    "/api/v1/system/getUpgradeStatus",
                    emptyMap<String, Any>()
                ).data?.status
            }
            status == 4
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun sendUpgradeFileStart(file: File): Boolean = withContext(Dispatchers.IO) {
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

    private suspend fun sendUpgradeFile(file: File): Boolean = withContext(Dispatchers.IO) {
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

    private suspend fun sendUpgradeFileEnd(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["MD5"] = calculateMD5(file).lowercase(Locale.ROOT)
            post<TS004Response<Boolean>>("/api/v1/system/sendUpgradeFileEnd", paramMap).isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setPseudoColor(mode: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["enable"] = false
            paramMap["mode"] = mode
            post<TS004Response<Boolean>>("/api/v1/system/setPseudoColor", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPseudoColor(): TS004Response<PseudoColorBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<PseudoColorBean>>("/api/v1/system/getPseudoColor", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setRangeFind(state: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["state"] = state
            post<TS004Response<Boolean>>("/api/v1/system/setRangeFind", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRangeFind(): TS004Response<RangeBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<RangeBean>>("/api/v1/system/getRangeFind", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setPanelParam(brightness: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["brightness"] = brightness
            post<TS004Response<Boolean>>("/api/v1/system/setPanelParam", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPanelParam(): TS004Response<BrightnessBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<BrightnessBean>>("/api/v1/system/getPanelParam", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setPip(enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["enable"] = enable
            post<TS004Response<Boolean>>("/api/v1/system/setPip", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPip(): TS004Response<PipBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<PipBean>>("/api/v1/system/getPip", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setZoom(factor: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["enable"] = true
            paramMap["factor"] = factor
            post<TS004Response<Boolean>>("/api/v1/system/setZoom", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getZoom(): TS004Response<ZoomBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<ZoomBean>>("/api/v1/system/getZoom", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setSnapshot(): Boolean = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<Boolean>>("/api/v1/system/snapshot", emptyMap<String, Any>()).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setVideo(enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["enable"] = enable
            post<TS004Response<Boolean>>("/api/v1/system/vrecord", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRecordStatus(): TS004Response<RecordStatusBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<RecordStatusBean>>("/api/v1/system/getRecordStatus", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getFreeSpace(): FreeSpaceBean? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<FreeSpaceBean>>("/api/v1/system/getFreeSpace", emptyMap<String, Any>()).data
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getFormatStorage(): Boolean = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<Boolean>>("/api/v1/system/formatStorage", emptyMap<String, Any>()).isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getResetAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<Boolean>>("/api/v1/system/resetAll", emptyMap<String, Any>()).status == 100
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setTISR(state: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val paramMap: HashMap<String, Any> = HashMap()
            paramMap["state"] = state
            post<TS004Response<Boolean>>("/api/v1/system/setTISR", paramMap).isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTISR(): TS004Response<TISRBean>? = withContext(Dispatchers.IO) {
        try {
            post<TS004Response<TISRBean>>("/api/v1/system/getTISR", emptyMap<String, Any>())
        } catch (_: Exception) {
            null
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\TS004ResponseFile.kt =====

package com.mpdc4gsr.libunified.app.repository

data class TS004Response<T>(
    val command: Int,
    val data: T?,
    val detail: String?,
    val status: Int,
    val transmit_cast: Int,
) {
    fun isSuccess(): Boolean = status == 0
}

data class PseudoColorBean(
    val enable: Boolean?,
    val mode: Int?,
)

data class RangeBean(
    val state: Int?,
)

data class PipBean(
    val enable: Boolean?,
)

data class BrightnessBean(
    val brightness: Int
)

data class ZoomBean(
    val factor: Int?,
)

data class TISRBean(
    val enable: Int?,
)

data class VersionBean(
    val firmware: String?,
)

data class DeviceInfo(
    val code: String,
    val model: String,
    val sn: String,
    val uuid: String,
)

data class FileCountBean(
    val fileCount: Int,
)

data class FilePageBean(
    val current: Int,
    val total: Int,
    val filelist: List<TS004FileBean>,
)

data class TS004FileBean(
    val id: Int,
    val type: Int,
    val duration: Int,
    val size: Long,
    val name: String,
    val thumb: String,
    val time: Long,
    val timezone: Int,
)

data class UpgradeStatus(
    val status: Int,
    val percent: Int,
)

data class FreeSpaceBean(
    val total: Long,
    val free: Long,
    val system: Long,
    val image_size: Long,
    val video_size: Long,
) {
    fun hasUseSize(): Long = system + image_size + video_size
}

data class RecordStatusBean(
    val errCode: Int,
    val path: String,
    val pts: Int,
    val status: Boolean,
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\repository\WsResponse.kt =====

package com.mpdc4gsr.libunified.app.repository

data class WsResponse<T>(
    val cmd: Int,
    val data: T?,
    val id: String,
)

data class WsPseudoColor(
    val enable: Boolean?,
    val mode: Int?,
)

data class WsRange(
    val state: Int?,
)

data class WsLight(
    val brightness: Int?,
)

data class WsPip(
    val enable: Int?,
)

data class WsZoom(
    val enable: Boolean?,
    val factor: Int?,
)