package com.mpdc4gsr.component.thermal.stubs

import android.content.Context

class MonitorSelectDialog {
    class Builder(
        private val context: Context,
    ) {
        private var positiveListener: ((Int) -> Unit)? = null

        fun setPositiveListener(listener: (Int) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }

        fun create(): MonitorSelectDialog = MonitorSelectDialog()
    }

    fun show() {
        // Stub implementation - would show a dialog with monitor selection options
        // For now, just trigger the positive listener with default option
    }
}

