package mpdc4gsr.gsr.network

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Handles post-session data upload to the PC orchestrator (FR10). Files are uploaded via HTTPS
 * with chunk retry logic.
 */
class TransferClient(
    private val okHttpClient: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val endpoint: String,
) {

    suspend fun uploadFile(
        sessionId: String,
        deviceId: String,
        file: File,
        modality: String,
    ) = withContext(dispatcher) {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sessionId", sessionId)
                .addFormDataPart("deviceId", deviceId)
                .addFormDataPart("modality", modality)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
                )
                .build()
        val request =
            Request.Builder()
                .url("$endpoint/upload")
                .post(requestBody)
                .build()
        retry {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Upload failed with code ${response.code}")
                }
            }
        }
    }

    private suspend fun retry(block: suspend () -> Unit) {
        var attempt = 0
        var delayMs = INITIAL_DELAY_MS
        while (true) {
            try {
                block()
                return
            } catch (ex: Exception) {
                attempt++
                if (attempt >= MAX_RETRIES) throw ex
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 5
        private val INITIAL_DELAY_MS = TimeUnit.SECONDS.toMillis(2)
        private val MAX_DELAY_MS = TimeUnit.SECONDS.toMillis(30)
    }
}
