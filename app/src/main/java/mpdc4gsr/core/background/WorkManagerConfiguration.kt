package mpdc4gsr.core.background

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import mpdc4gsr.core.utils.AppLogger

object WorkManagerConfiguration {
    private const val TAG = "WorkManagerConfig"

    fun initialize(context: Context) {
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
            WorkManager.initialize(context, config)
            AppLogger.i(TAG, "WorkManager initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize WorkManager", e)
        }
    }


}



