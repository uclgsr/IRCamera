// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\config\router\DegradeServiceImpl.kt =====

package com.mpdc4gsr.libunified.app.config.router

import android.content.Context
import android.widget.Toast
import com.elvishew.xlog.XLog

class DegradeServiceImpl {
    fun init(context: Context?) {
    }

    fun onLost(
        context: Context?,
        path: String?,
    ) {
        if (context != null) {
            Toast.makeText(context, "Navigation failed: $path", Toast.LENGTH_SHORT).show()
            XLog.e("Navigation failed to path: $path")
        }
    }
}