package com.mpdc4gsr.component.shared.compat

import android.content.Context
import android.content.SharedPreferences

class SPUtils private constructor(
    private val prefs: SharedPreferences,
) {
    companion object {
        @Volatile
        private var defaultInstance: SPUtils? = null
        private val namedInstances = mutableMapOf<String, SPUtils>()

        @JvmStatic
        fun getInstance(): SPUtils =
            defaultInstance ?: synchronized(this) {
                defaultInstance ?: SPUtils(
                    ContextProvider.getContext().getSharedPreferences(
                        "default_prefs",
                        Context.MODE_PRIVATE,
                    ),
                ).also { defaultInstance = it }
            }

        @JvmStatic
        fun getInstance(name: String): SPUtils =
            namedInstances[name] ?: synchronized(this) {
                namedInstances.getOrPut(name) {
                    SPUtils(
                        ContextProvider.getContext().getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE,
                        ),
                    )
                }
            }
    }

    fun put(
        key: String,
        value: Any?,
    ) {
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
            else -> editor.putString(key, value.toString())
        }
        editor.apply()
    }

    fun getString(
        key: String,
        defaultValue: String = "",
    ): String = prefs.getString(key, defaultValue) ?: defaultValue

    fun getInt(
        key: String,
        defaultValue: Int = 0,
    ): Int = prefs.getInt(key, defaultValue)

    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Boolean = prefs.getBoolean(key, defaultValue)

    fun getFloat(
        key: String,
        defaultValue: Float = 0f,
    ): Float = prefs.getFloat(key, defaultValue)

    fun getLong(
        key: String,
        defaultValue: Long = 0L,
    ): Long = prefs.getLong(key, defaultValue)

    fun contains(key: String): Boolean = prefs.contains(key)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}


