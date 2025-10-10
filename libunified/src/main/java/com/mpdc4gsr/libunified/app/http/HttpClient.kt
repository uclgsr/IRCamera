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
        retryOnConnectionFailure: Boolean = false,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
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
        headers: Map<String, String> = emptyMap(),
    ): T {
        val jsonBody = gson.toJson(body)
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder =
            Request
                .Builder()
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
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            try {
                                if (!response.isSuccessful) {
                                    throw IOException("HTTP ${response.code}: ${response.message}")
                                }
                                val responseBody =
                                    response.body?.string()
                                        ?: throw IOException("Empty response body")
                                val result = gson.fromJson(responseBody, responseType)
                                continuation.resume(result)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                },
            )
        }
    }

    suspend fun executeOctetPost(
        client: OkHttpClient,
        url: String,
        data: ByteArray,
        headers: Map<String, String> = emptyMap(),
    ): ResponseBody {
        val requestBody = data.toRequestBody(OCTET_STREAM_MEDIA_TYPE)
        val requestBuilder =
            Request
                .Builder()
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
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            try {
                                if (!response.isSuccessful) {
                                    throw IOException("HTTP ${response.code}: ${response.message}")
                                }
                                val body =
                                    response.body
                                        ?: throw IOException("Empty response body")
                                continuation.resume(body)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                },
            )
        }
    }

    suspend fun executeGet(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): ResponseBody {
        val requestBuilder =
            Request
                .Builder()
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
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            try {
                                if (!response.isSuccessful) {
                                    throw IOException("HTTP ${response.code}: ${response.message}")
                                }
                                val body =
                                    response.body
                                        ?: throw IOException("Empty response body")
                                continuation.resume(body)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                },
            )
        }
    }
}
