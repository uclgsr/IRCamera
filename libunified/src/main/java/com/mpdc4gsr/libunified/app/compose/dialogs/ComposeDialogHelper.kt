package com.mpdc4gsr.libunified.app.compose.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

class ComposeDialogWrapper(
    context: Context,
    private val content: @Composable () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val composeView = ComposeView(context).apply {
            setContent {
                LibUnifiedTheme {
                    content()
                }
            }
        }
        
        setContentView(composeView)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}

class LoadingDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")

    fun show(message: String = "") {
        dismiss()
        messageState.value = message
        dialog = ComposeDialogWrapper(context) {
            LoadingDialog(
                message = messageState.value,
                onDismissRequest = {}
            )
        }.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun setMessage(message: String) {
        messageState.value = message
    }
}

class ConfirmDialogState(private val context: Context) {
    fun show(
        title: String,
        message: String = "",
        showIcon: Boolean = true,
        showCancel: Boolean = true,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel",
        showCheckbox: Boolean = false,
        checkboxLabel: String = "",
        onConfirm: (isChecked: Boolean) -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            ConfirmDialog(
                title = title,
                message = message,
                showIcon = showIcon,
                showCancel = showCancel,
                confirmText = confirmText,
                cancelText = cancelText,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                onConfirm = {
                    onConfirm(it)
                },
                onDismiss = {}
            )
        }
        dialog.show()
    }
}
