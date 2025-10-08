// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http\libunified_src_main_java_com_mpdc4gsr_libunified_app_http_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\http' subtree
// Files: 3; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\HttpClient.kt =====

package com.mpdc4gsr.libunified.app.http

import android.net.Network
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object HttpClient {
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()
    var network: Network? = null
    fun createClient(
        connectTimeout: Long = 15,
        readTimeout: Long = 15,
        writeTimeout: Long = 15,
        retryOnConnectionFailure: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .retryOnConnectionFailure(retryOnConnectionFailure)
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        network?.socketFactory?.let {
            builder.socketFactory(it)
        }
        return builder.build()
    }

    suspend fun <T> executeJsonPost(
        client: OkHttpClient,
        url: String,
        body: Any,
        responseType: Class<T>,
        headers: Map<String, String> = emptyMap()
    ): T {
        val jsonBody = gson.toJson(body)
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}: ${response.message}")
                            }
                            val responseBody = response.body?.string()
                                ?: throw IOException("Empty response body")
                            val result = gson.fromJson(responseBody, responseType)
                            continuation.resume(result)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }
    }

    suspend fun executeOctetPost(
        client: OkHttpClient,
        url: String,
        data: ByteArray,
        headers: Map<String, String> = emptyMap()
    ): ResponseBody {
        val requestBody = data.toRequestBody(OCTET_STREAM_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}: ${response.message}")
                            }
                            val body = response.body
                                ?: throw IOException("Empty response body")
                            continuation.resume(body)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }
    }

    suspend fun executeGet(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ResponseBody {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}: ${response.message}")
                            }
                            val body = response.body
                                ?: throw IOException("Empty response body")
                            continuation.resume(body)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository\LmsRepository.kt =====

package com.mpdc4gsr.libunified.app.http.repository

import android.text.TextUtils
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mpdc4gsr.libunified.app.bean.base.Resp
import com.mpdc4gsr.libunified.app.bean.json.CheckVersionJson
import com.mpdc4gsr.libunified.app.bean.json.StatementJson
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

object LmsRepository {
    suspend fun getVersionInfo(): CheckVersionJson? {
        var result: CheckVersionJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().checkAppUpdate { response ->
            try {
                val responseBean = Gson().fromJson(response, ResponseBean::class.java)
                if (responseBean.code == "2000") {
                    result =
                        Gson().fromJson(responseBean.data.toString(), CheckVersionJson::class.java)
                }
            } catch (e: Exception) {
                XLog.e("version json[ph][ph][ph][ph]: ${e.message}")
            }
            downLatch.countDown()
        }
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }

    suspend fun getStatementUrl(type: String): StatementJson? {
        var result: StatementJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().getStatement(
            type,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    try {
                        val typeOfT = object : TypeToken<Resp<StatementJson>>() {}.type
                        val json = Gson().fromJson<Resp<StatementJson>>(p0, typeOfT)
                        if (json.code == "2000") {
                            result = json.data
                        }
                    } catch (e: Exception) {
                        XLog.e("json[ph][ph][ph][ph]: ${e.message}")
                    }
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    downLatch.countDown()
                    XLog.w("onFail: $result")
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils.getResString(
                            LMS.mContext,
                            if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                        ).let {
                            TToast.shortToast(LMS.mContext, it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool\DownloadTools.kt =====

package com.mpdc4gsr.libunified.app.http.tool

import com.mpdc4gsr.libunified.app.http.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object DownloadTools {
    private val okHttpClient: OkHttpClient by lazy {
        HttpClient.createClient(
            connectTimeout = 10,
            readTimeout = 10,
            writeTimeout = 10,
            retryOnConnectionFailure = false
        )
    }

    suspend fun download(
        url: String,
        file: File,
        listener: (cur: Long, total: Long) -> Unit,
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
                val totalCount = responseBody.contentLength()
                val buffer = ByteArray(4096)
                var hasReadCount = 0L
                var lastReadCount = 0L
                var readLength = inputStream.read(buffer)
                while (readLength != -1) {
                    hasReadCount += readLength
                    fileOutputString.write(buffer, 0, readLength)
                    if (hasReadCount - lastReadCount > 100 * 1024) {
                        lastReadCount = hasReadCount
                        launch(Dispatchers.Main) {
                            listener.invoke(hasReadCount, totalCount)
                        }
                    }
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
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository\libunified_src_main_java_com_mpdc4gsr_libunified_app_http_repository_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\repository\LmsRepository.kt =====

package com.mpdc4gsr.libunified.app.http.repository

import android.text.TextUtils
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mpdc4gsr.libunified.app.bean.base.Resp
import com.mpdc4gsr.libunified.app.bean.json.CheckVersionJson
import com.mpdc4gsr.libunified.app.bean.json.StatementJson
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

object LmsRepository {
    suspend fun getVersionInfo(): CheckVersionJson? {
        var result: CheckVersionJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().checkAppUpdate { response ->
            try {
                val responseBean = Gson().fromJson(response, ResponseBean::class.java)
                if (responseBean.code == "2000") {
                    result =
                        Gson().fromJson(responseBean.data.toString(), CheckVersionJson::class.java)
                }
            } catch (e: Exception) {
                XLog.e("version json[ph][ph][ph][ph]: ${e.message}")
            }
            downLatch.countDown()
        }
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }

    suspend fun getStatementUrl(type: String): StatementJson? {
        var result: StatementJson? = null
        val downLatch = CountDownLatch(1)
        LMS.getInstance().getStatement(
            type,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    try {
                        val typeOfT = object : TypeToken<Resp<StatementJson>>() {}.type
                        val json = Gson().fromJson<Resp<StatementJson>>(p0, typeOfT)
                        if (json.code == "2000") {
                            result = json.data
                        }
                    } catch (e: Exception) {
                        XLog.e("json[ph][ph][ph][ph]: ${e.message}")
                    }
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    downLatch.countDown()
                    XLog.w("onFail: $result")
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils.getResString(
                            LMS.mContext,
                            if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                        ).let {
                            TToast.shortToast(LMS.mContext, it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool\libunified_src_main_java_com_mpdc4gsr_libunified_app_http_tool_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool\DownloadTools.kt =====

package com.mpdc4gsr.libunified.app.http.tool

import com.mpdc4gsr.libunified.app.http.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object DownloadTools {
    private val okHttpClient: OkHttpClient by lazy {
        HttpClient.createClient(
            connectTimeout = 10,
            readTimeout = 10,
            writeTimeout = 10,
            retryOnConnectionFailure = false
        )
    }

    suspend fun download(
        url: String,
        file: File,
        listener: (cur: Long, total: Long) -> Unit,
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
                val totalCount = responseBody.contentLength()
                val buffer = ByteArray(4096)
                var hasReadCount = 0L
                var lastReadCount = 0L
                var readLength = inputStream.read(buffer)
                while (readLength != -1) {
                    hasReadCount += readLength
                    fileOutputString.write(buffer, 0, readLength)
                    if (hasReadCount - lastReadCount > 100 * 1024) {
                        lastReadCount = hasReadCount
                        launch(Dispatchers.Main) {
                            listener.invoke(hasReadCount, totalCount)
                        }
                    }
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
}