package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context

/**
 * Stub implementation for ThermalInputDialog
 * This is a placeholder to resolve compilation issues
 */
class ThermalInputDialog {
    
    class Builder(private val context: Context) {
        private var message: String = ""
        private var positiveListener: ((Float, Float, Int, Int) -> Unit)? = null
        private var cancelListener: (() -> Unit)? = null
        
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }
        
        fun setNum(max: Float = 0f, min: Float = 0f): Builder {
            // Store max/min values if needed
            return this
        }
        
        fun setColor(maxColor: Int = 0, minColor: Int = 0): Builder {
            // Store color values if needed
            return this
        }
        
        fun setPositiveListener(textResId: Int, listener: (Float, Float, Int, Int) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }
        
        fun setCancelListener(text: String, listener: () -> Unit): Builder {
            this.cancelListener = listener
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