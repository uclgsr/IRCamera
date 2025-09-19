package com.mpdc4gsr.lib.core.tools

import android.content.Context
import com.mpdc4gsr.lib.core.R

object LanguageTool {

    fun showLanguage(context: Context): String {
        return context.getString(R.string.english)
    }

    fun useLanguage(context: Context): String {
        return "en-WW"
    }

    fun useStatementLanguage(): String {
        return "EN"
    }
}
