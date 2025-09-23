package com.mpdc4gsr.libunified.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.mpdc4gsr.libunified.R

class ProgressDialog(private val context: Context) {
    private var dialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var titleTextView: TextView? = null
    
    var max: Int = 100
        set(value) {
            field = value
            progressBar?.max = value
        }
    
    var progress: Int = 0
        set(value) {
            field = value
            progressBar?.progress = value
        }
    
    init {
        createDialog()
    }
    
    private fun createDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        progressBar = view.findViewById(R.id.progress_bar)
        titleTextView = view.findViewById(R.id.tv_title)
        
        progressBar?.max = max
        progressBar?.progress = progress
        
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()
    }
    
    fun show() {
        dialog?.show()
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
    
    fun setMessage(message: String) {
        titleTextView?.text = message
    }
    
    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}