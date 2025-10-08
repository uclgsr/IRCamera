// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\compat' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\compat\ContextProvider.kt =====

package com.mpdc4gsr.libunified.compat

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\compat\DimensionExt.kt =====

package com.mpdc4gsr.libunified.compat

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue


fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Int.pxToDp(context: Context): Int {
    return (this / context.resources.displayMetrics.density).toInt()
}

fun Int.spToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

fun Float.pxToDp(context: Context): Float {
    return this / context.resources.displayMetrics.density
}

fun Float.spToPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics
    )
}

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)")
)
val Int.dpLegacy: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)")
)
val Int.pxLegacy: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)")
)
val Int.spLegacy: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

class ScreenDimensions(private val context: Context) {
    val screenWidthPx: Int
        get() = context.resources.displayMetrics.widthPixels
    val screenHeightPx: Int
        get() = context.resources.displayMetrics.heightPixels
    val screenDensity: Float
        get() = context.resources.displayMetrics.density
    val screenDensityDpi: Int
        get() = context.resources.displayMetrics.densityDpi
    val screenWidthDp: Int
        get() = screenWidthPx.pxToDp(context)
    val screenHeightDp: Int
        get() = screenHeightPx.pxToDp(context)
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\compat\SPUtils.kt =====

package com.mpdc4gsr.libunified.compat

import android.content.Context
import android.content.SharedPreferences

class SPUtils private constructor(private val prefs: SharedPreferences) {
    companion object {
        @Volatile
        private var defaultInstance: SPUtils? = null
        private val namedInstances = mutableMapOf<String, SPUtils>()

        @JvmStatic
        fun getInstance(): SPUtils {
            return defaultInstance ?: synchronized(this) {
                defaultInstance ?: SPUtils(
                    ContextProvider.getContext().getSharedPreferences(
                        "default_prefs",
                        Context.MODE_PRIVATE
                    )
                ).also { defaultInstance = it }
            }
        }

        @JvmStatic
        fun getInstance(name: String): SPUtils {
            return namedInstances[name] ?: synchronized(this) {
                namedInstances.getOrPut(name) {
                    SPUtils(
                        ContextProvider.getContext().getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE
                        )
                    )
                }
            }
        }
    }

    fun put(key: String, value: Any?) {
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

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}