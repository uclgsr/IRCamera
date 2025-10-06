package com.mpdc4gsr.module.thermalunified.compat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * AndroidX alternative to utilcode Utils.getApp()
 * Provides global context access without using hidden APIs
 *
 * Initialize in Application.onCreate():
 * ContextProvider.init(this)
 */
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
                "ContextProvider not initialized. Call ContextProvider.init() in Application.onCreate()"
            )
        }
        return applicationContext
    }

    @JvmStatic
    fun getApplication(): Application {
        return getContext() as Application
    }
}
