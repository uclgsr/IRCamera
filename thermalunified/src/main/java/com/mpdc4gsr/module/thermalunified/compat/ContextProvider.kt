package com.mpdc4gsr.module.thermalunified.compat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

@SuppressLint("StaticFieldLeak")
object ContextProvider {
    private lateinit var applicationContext: Context

    @JvmStatic
    fun init(application: Application) {
        applicationContext = application.applicationContext
    }

    @JvmStatic
    fun getContext(): Context {
        if (!::applicationContext.isInitialized) {
            throw IllegalStateException(
                "ContextProvider not initialized. Call ContextProvider.init() in Application.onCreate()",
            )
        }
        return applicationContext
    }

    @JvmStatic
    fun getApplication(): Application = getContext() as Application
}
