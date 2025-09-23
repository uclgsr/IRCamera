package com.mpdc4gsr.libunified.ui.dialog

import android.app.Dialog
import android.content.Context

/**
 * Stub implementation for ThermalInputDialog to enable compilation
 * This is a minimal implementation for MVP - replace with actual dialog when available
 */
class ThermalInputDialog(context: Context) : Dialog(context) {
    
    class Builder(private val context: Context) {
        private var positiveListener: ((Float, Float, Float, Float) -> Unit)? = null
        
        fun setPositiveListener(listener: (Float, Float, Float, Float) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }
        
        fun create(): ThermalInputDialog {
            val dialog = ThermalInputDialog(context)
            // Stub implementation - automatically calls listener with default values
            dialog.setOnShowListener {
                // For MVP, automatically trigger with default values
                positiveListener?.invoke(0.0f, 100.0f, 20.0f, 80.0f)
                dialog.dismiss()
            }
            return dialog
        }
    }
}