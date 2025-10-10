package com.mpdc4gsr.libunified.app.config.router

import android.content.Context
import android.widget.Toast

class DegradeServiceImpl {
    fun init(context: Context?) {
    }

    fun onLost(
        context: Context?,
        path: String?,
    ) {
        if (context != null) {
            Toast.makeText(context, "Navigation failed: $path", Toast.LENGTH_SHORT).show()
        }
    }
}
