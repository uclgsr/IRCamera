package com.topdon.lib.core.config.router

import android.content.Context
import android.widget.Toast
import com.elvishew.xlog.XLog

/**
 * Legacy degrade service - no longer used with NavigationManager
 * create by fylder on 2018/7/23
 **/
class DegradeServiceImpl {
    fun init(context: Context?) {
        // No longer needed with NavigationManager
    }

    // Legacy method for handling navigation failures
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
