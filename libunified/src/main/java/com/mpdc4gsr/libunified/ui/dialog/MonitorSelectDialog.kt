package com.mpdc4gsr.libunified.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog

class MonitorSelectDialog private constructor(private val context: Context) {

    private var positiveListener: ((Int) -> Unit)? = null

    fun show() {
        val options = arrayOf(
            "Point Temperature",
            "Line Temperature", 
            "Rectangle Temperature"
        )

        AlertDialog.Builder(context)
            .setTitle("Select Monitor Type")
            .setItems(options) { _, which ->
                positiveListener?.invoke(which + 1)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    class Builder(private val context: Context) {
        private val dialog = MonitorSelectDialog(context)

        fun setPositiveListener(listener: (Int) -> Unit): Builder {
            dialog.positiveListener = listener
            return this
        }

        fun create(): MonitorSelectDialog {
            return dialog
        }
    }
}