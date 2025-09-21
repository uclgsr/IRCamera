package com.mpdc4gsr.commons.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.util.Locale

class PreUtil private constructor(context: Context, shareName: String? = SHARE_NAME) {
    private val DATA_URL = "/data/data/"
    private val SHARED_PREFS = "/shared_prefs"
    private val mContext: WeakReference<Context?>
    private val preferences: SharedPreferences

    init {
        mContext = WeakReference<Context?>(context)
        preferences = context.getSharedPreferences(shareName, Context.MODE_PRIVATE)
    }

    fun put(key: String?, value: Boolean) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putBoolean(key, value)
            edit.commit()
        }
    }

    fun put(key: String?, value: String?) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putString(key, value)
            edit.commit()
        }
    }

    fun put(key: String?, value: Int) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putInt(key, value)
            edit.commit()
        }
    }

    fun put(key: String?, value: Float) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putFloat(key, value)
            edit.commit()
        }
    }

    fun put(key: String?, value: Long) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putLong(key, value)
            edit.commit()
        }
    }

    fun put(key: String?, value: MutableSet<String?>?) {
        var key = key
        val edit = preferences.edit()
        if (edit != null) {
            if (!TextUtils.isEmpty(key)) {
                key = key!!.lowercase(Locale.getDefault())
            }
            edit.putStringSet(key, value)
            edit.commit()
        }
    }

    fun <T> put(t: T?) {
        try {
            var methodName = ""
            var savekey = ""
            var saveValue = ""
            val edit = preferences.edit()
            val cls: Class<*> = t.javaClass

            if (edit != null) {
                val methods = cls.getDeclaredMethods()
                for (method in methods) {
                    methodName = method.getName()
                    if (methodName != null && methodName.startsWith("get")) {
                        val value = method.invoke(t)
                        if (!TextUtils.isEmpty(value.toString())) {
                            saveValue = value.toString()
                        }
                        savekey = methodName.replace("get", "")
                        savekey = savekey.lowercase(Locale.getDefault())
                        edit.putString(savekey, saveValue)
                    }
                }
                edit.commit()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun get(key: String?): String {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getString(key, "")!!
    }

    fun get(key: String?, defValue: String?): String {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getString(key, defValue)!!
    }

    fun get(key: String?, defValue: Boolean): Boolean {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getBoolean(key, defValue)
    }

    fun get(key: String?, defValue: Int): Int {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getInt(key, defValue)
    }

    fun get(key: String?, defValue: Float): Float {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getFloat(key, defValue)
    }

    fun get(key: String?, defValue: Long): Long {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getLong(key, defValue)
    }

    @SuppressLint("NewApi")
    fun get(key: String?, defValue: MutableSet<String?>?): MutableSet<String?>? {
        var key = key
        if (!TextUtils.isEmpty(key)) {
            key = key!!.lowercase(Locale.getDefault())
        }
        return preferences.getStringSet(key, defValue)
    }

    @SuppressLint("CommitPrefEdits")
    fun put(key: String?, defaultObj: Any?) {
        if (defaultObj is String) {
            preferences.edit().putString(key, defaultObj)
        } else if (defaultObj is Int) {
            preferences.edit().putInt(key, defaultObj)
        } else if (defaultObj is Boolean) {
            preferences.edit().putBoolean(key, defaultObj)
        } else if (defaultObj is Float) {
            preferences.edit().putFloat(key, defaultObj)
        } else if (defaultObj is Long) {
            preferences.edit().putLong(key, defaultObj)
        }
        preferences.edit().commit()
    }

    fun get(key: String?, defaultObj: Any?): Any? {
        if (defaultObj is String) {
            return preferences.getString(key, defaultObj)
        } else if (defaultObj is Int) {
            return preferences.getInt(key, defaultObj)
        } else if (defaultObj is Boolean) {
            return preferences.getBoolean(key, defaultObj)
        } else if (defaultObj is Float) {
            return preferences.getFloat(key, defaultObj)
        } else if (defaultObj is Long) {
            return preferences.getLong(key, defaultObj)
        }
        return null
    }

    fun <T> get(cls: Class<T?>): Any? {
        var obj: Any? = null
        var fieldName = ""
        try {
            obj = cls.newInstance()
            val fields = cls.getDeclaredFields()
            for (f in fields) {
                fieldName = f.getName()
                if ("serialVersionUID" != fieldName) {
                    f.setAccessible(true)
                    f.set(obj, get(f.getName()))
                }
            }
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return obj
    }

    fun clearAll() {
        try {
            val fileName: String = SHARE_NAME + ".xml"
            val path = StringBuilder(DATA_URL).append(mContext.get()!!.getPackageName()).append(SHARED_PREFS)
            val file = File(path.toString(), fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val SHARE_NAME = "ad900_data"
        private var instance: PreUtil? = null
        fun getInstance(context: Context): PreUtil {
            return getInstance(context, SHARE_NAME)
        }

        fun getInstance(
            context: Context,
            shareName: String?
        ): PreUtil {
            if (instance == null) {
                synchronized(PreUtil::class.java) {
                    if (instance == null) {
                        instance = PreUtil(context, shareName)
                    }
                }
            }
            return instance!!
        }
    }
}
