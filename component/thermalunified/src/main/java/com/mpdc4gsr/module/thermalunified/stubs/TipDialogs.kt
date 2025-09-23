package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context

/**
 * Stub implementations for missing dialogs
 * These are placeholders to resolve compilation issues
 */

class TipGuideDialog {
    class Builder(private val context: Context) {
        fun create(): TipGuideDialog = TipGuideDialog()
    }
    
    fun show() {
        // Stub implementation
    }
}

class TipPreviewDialog {
    class Builder(private val context: Context) {
        fun create(): TipPreviewDialog = TipPreviewDialog()
    }
    
    fun show() {
        // Stub implementation
    }
}