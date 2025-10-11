package com.mpdc4gsr.component.shared.app.repository

import okhttp3.Interceptor
import okhttp3.Response

class OKLogInterceptor(
    val isTC007: Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}


