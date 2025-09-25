package com.mpdc4gsr.libunified.ui.dialog

import android.content.Context

class ThermalInputDialog private constructor(private val builder: Builder) {

    fun show() {
        // Basic implementation for MVP - real implementation would show a dialog
    }

    class Builder(private val context: Context) {
        private var message: String = ""
        private var positiveListener: ((Float, Float, Float, Float) -> Unit)? = null
        private var cancelListener: Int? = null

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setPositiveListener(textResId: Int, listener: (Float, Float, Float, Float) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }

        fun setCancelListener(textResId: Int): Builder {
            this.cancelListener = textResId
            return this
        }

        fun create(): ThermalInputDialog {
            return ThermalInputDialog(this)
        }
    }
}