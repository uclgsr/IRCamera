// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\http\tool\DownloadTools.kt =====

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