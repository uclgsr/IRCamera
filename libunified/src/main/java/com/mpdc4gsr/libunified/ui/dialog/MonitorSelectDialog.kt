package com.mpdc4gsr.libunified.ui.dialog

import android.app.Dialog
import android.content.Context

/**
 * Stub implementation for MonitorSelectDialog to enable compilation
 * This is a minimal implementation for MVP - replace with actual dialog when available
 */
class MonitorSelectDialog(context: Context) : Dialog(context) {

    class Builder(private val context: Context) {
        private var positiveListener: ((Int) -> Unit)? = null

        fun setPositiveListener(listener: (Int) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }

        fun create(): MonitorSelectDialog {
            val dialog = MonitorSelectDialog(context)
            // Stub implementation - automatically calls listener with default value 1
            dialog.setOnShowListener {
                // For MVP, automatically trigger first option
                positiveListener?.invoke(1)
                dialog.dismiss()
            }
            return dialog
        }
    }
}