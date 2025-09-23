package com.mpdc4gsr.libunified.app.http.api

import com.okhttp3.ResponseBody
import com.retrofit2.http.GET
import com.retrofit2.http.Streaming
import com.retrofit2.http.Url

interface DownloadApiService {

    @GET
    @Streaming
    suspend fun download(
        @Url url: String,
    ): ResponseBody
}
