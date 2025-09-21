package com.mpdc4gsr.commons.util

import android.content.Context

object MPDC4GSR {
    var app: Context? = null
        private set

    fun init(context: Context) {
        app = context.getApplicationContext()
    }
}
