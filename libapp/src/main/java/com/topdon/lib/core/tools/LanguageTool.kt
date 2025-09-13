package com.topdon.lib.core.tools

import android.content.Context
import com.topdon.lib.core.R

object LanguageTool {
    /**
     * Get display language - English only
     */
    fun showLanguage(context: Context): String {
        return context.getString(R.string.english)
    }

    /**
     * Get language code for server communication - English only
     */
    fun useLanguage(context: Context): String {
        return "en-WW"
    }

    /**
     * Get language code for statement interface - English only
     */
    fun useStatementLanguage(): String {
        return "EN"
    }
}
