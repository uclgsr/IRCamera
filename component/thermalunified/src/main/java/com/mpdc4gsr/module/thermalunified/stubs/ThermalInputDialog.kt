package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context

/**
 * Stub implementation for ThermalInputDialog
 * This is a placeholder to resolve compilation issues
 */
class ThermalInputDialog {
    
    class Builder(private val context: Context) {
        private var message: String = ""
        private var positiveListener: (() -> Unit)? = null
        
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }
        
        fun setPositiveListener(listener: () -> Unit): Builder {
            this.positiveListener = listener
            return this
        }
        
        fun create(): ThermalInputDialog {
            return ThermalInputDialog()
        }
    }
    
    fun show() {
        // Stub implementation
    }
}