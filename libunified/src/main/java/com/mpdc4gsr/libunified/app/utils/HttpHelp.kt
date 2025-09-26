package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import com.mpdc4gsr.libunified.app.config.AppConfig
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtil
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import com.mpdc4gsr.libunified.app.lms.xutils.x

/**
 * HTTP Helper utility for network operations
 * Based on existing LMS framework integration
 */
object HttpHelp {

    /**
     * Make a POST request with the given parameters
     */
    fun post(
        context: Context,
        url: String,
        params: Map<String, Any>? = null,
        callback: IResponseCallback
    ) {
        try {
            val requestParams = RequestParams(url).apply {
                // Add language parameter
                addParameter("language", LanguageUtil.getLanguageId(context))
                
                // Add custom parameters
                params?.forEach { (key, value) ->
                    addParameter(key, value)
                }
            }

            // Use LMS instant for HTTP operations
            val lmsInstance = LMS.getInstance()
            x.http().post(requestParams, object : com.mpdc4gsr.libunified.app.lms.xutils.common.Callback.CommonCallback<String> {
                override fun onSuccess(result: String?) {
                    result?.let { callback.onResponse(it) }
                }

                override fun onError(ex: Throwable?, isOnCallback: Boolean) {
                    callback.onFail(Exception(ex?.message ?: "Unknown error", ex))
                }

                override fun onCancelled(cex: com.mpdc4gsr.libunified.app.lms.xutils.common.Callback.CancelledException?) {
                    callback.onFail("Request cancelled", "CANCELLED")
                }
            })
        } catch (e: Exception) {
            callback.onFail(e)
        }
    }

    /**
     * Make a GET request
     */
    fun get(
        context: Context,
        url: String,
        params: Map<String, Any>? = null,
        callback: IResponseCallback
    ) {
        try {
            val fullUrl = buildUrlWithParams(url, params)
            val requestParams = RequestParams(fullUrl).apply {
                addParameter("language", LanguageUtil.getLanguageId(context))
            }

            x.http().post(requestParams, object : com.mpdc4gsr.libunified.app.lms.xutils.common.Callback.CommonCallback<String> {
                override fun onSuccess(result: String?) {
                    result?.let { callback.onResponse(it) }
                }

                override fun onError(ex: Throwable?, isOnCallback: Boolean) {
                    callback.onFail(Exception(ex?.message ?: "Unknown error", ex))
                }
            })
        } catch (e: Exception) {
            callback.onFail(e)
        }
    }

    /**
     * Build URL with query parameters
     */
    private fun buildUrlWithParams(baseUrl: String, params: Map<String, Any>?): String {
        if (params.isNullOrEmpty()) return baseUrl
        
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=$value"
        }
        
        return if (baseUrl.contains("?")) {
            "$baseUrl&$queryString"
        } else {
            "$baseUrl?$queryString"
        }
    }

    /**
     * Get base URL from configuration
     */
    fun getBaseUrl(): String {
        return UrlConstant.getBaseUrl()
    }

    /**
     * Set base URL
     */
    fun setBaseUrl(url: String) {
        UrlConstant.setBaseUrl(url, true)
    }

    /**
     * Create request parameters with common headers
     */
    fun createRequestParams(context: Context, url: String): RequestParams {
        return RequestParams(url).apply {
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
            addParameter("language", LanguageUtil.getLanguageId(context))
        }
    }

    /**
     * Check if URL is valid
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Build full API URL
     */
    fun buildApiUrl(endpoint: String): String {
        val baseUrl = getBaseUrl()
        return if (endpoint.startsWith("/")) {
            "$baseUrl${endpoint.substring(1)}"
        } else {
            "$baseUrl$endpoint"
        }
    }
}