package com.mpdc4gsr.libunified.ui.dialog

import android.app.Dialog
import android.content.Context
import androidx.fragment.app.DialogFragment

/**
 * Stub implementation for TipGuideDialog to enable compilation
 * This is a minimal implementation for MVP - replace with actual dialog when available
 */
class TipGuideDialog : DialogFragment() {
    
    companion object {
        fun newInstance(): TipGuideDialog {
            return TipGuideDialog()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): Dialog {
        return Dialog(requireContext())
    }
}