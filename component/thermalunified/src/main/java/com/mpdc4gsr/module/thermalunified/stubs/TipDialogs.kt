package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Stub implementations for missing dialogs
 * These are placeholders to resolve compilation issues
 */

class TipGuideDialog : DialogFragment() {
    
    var closeEvent: ((Boolean) -> Unit)? = null
    
    companion object {
        fun newInstance(): TipGuideDialog {
            return TipGuideDialog()
        }
    }
    
    fun show(fragmentManager: FragmentManager, tag: String) {
        super.show(fragmentManager, tag)
    }
}

class TipPreviewDialog : DialogFragment() {
    
    var closeEvent: ((Boolean) -> Unit)? = null
    
    companion object {
        fun newInstance(): TipPreviewDialog {
            return TipPreviewDialog()
        }
    }
    
    fun show(fragmentManager: FragmentManager, tag: String) {
        super.show(fragmentManager, tag)
    }
}

class TipObserveDialog {
    class Builder(private val context: Context) {
        private var title: String = ""
        private var message: String = ""
        private var cancelListener: ((Boolean) -> Unit)? = null
        
        fun setTitle(resId: Int): Builder {
            this.title = context.getString(resId)
            return this
        }
        
        fun setMessage(resId: Int): Builder {
            this.message = context.getString(resId)
            return this
        }
        
        fun setCancelListener(listener: (Boolean) -> Unit): Builder {
            this.cancelListener = listener
            return this
        }
        
        fun create(): TipObserveDialog = TipObserveDialog()
    }
    
    fun show() {
        // Stub implementation
    }
}

class TipDialog {
    class Builder(private val context: Context) {
        private var title: String = ""
        private var message: String = ""
        private var positiveListener: (() -> Unit)? = null
        private var negativeListener: (() -> Unit)? = null
        
        fun setTitle(resId: Int): Builder {
            this.title = context.getString(resId)
            return this
        }
        
        fun setMessage(resId: Int): Builder {
            this.message = context.getString(resId)
            return this
        }
        
        fun setPositiveListener(resId: Int, listener: () -> Unit): Builder {
            this.positiveListener = listener
            return this
        }
        
        fun setNegativeListener(resId: Int, listener: () -> Unit): Builder {
            this.negativeListener = listener
            return this
        }
        
        fun create(): TipDialog = TipDialog()
    }
    
    fun show() {
        // Stub implementation
    }
}

class TempAlarmSetDialog {
    class Builder(private val context: Context) {
        fun setNum(num: String): Builder = this
        fun create(): TempAlarmSetDialog = TempAlarmSetDialog()
    }
    
    fun show() {
        // Stub implementation
    }
}