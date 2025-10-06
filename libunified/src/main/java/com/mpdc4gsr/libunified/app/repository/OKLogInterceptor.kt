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
                XLog.tag("RetrofitLog").v("[ph][ph]：${buffer.readString(StandardCharsets.UTF_8)}")
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
                            "[ph][ph]：${
                                responseStr.substring(
                                    0,
                                    1024
                                )
                            } ...[ph][ph][ph][ph][ph][ph][ph]"
                        )
                } else {
                    XLog.tag("RetrofitLog").v("[ph][ph]：$responseStr")
                }
            }
        }
        return response
    }
}
