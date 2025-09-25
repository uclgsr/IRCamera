package com.mpdc4gsr.libunified.ui.dialog

import android.app.Dialog
import androidx.fragment.app.DialogFragment

/**
 * Stub implementation for TipPreviewDialog to enable compilation
 * This is a minimal implementation for MVP - replace with actual dialog when available
 */
class TipPreviewDialog : DialogFragment() {

    companion object {
        fun newInstance(): TipPreviewDialog {
            return TipPreviewDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): Dialog {
        return Dialog(requireContext())
    }
}