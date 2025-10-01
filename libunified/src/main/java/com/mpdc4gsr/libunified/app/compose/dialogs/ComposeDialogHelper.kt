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

class ProgressDialogState(private val context: Context) {
    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    private val progressState = mutableStateOf(-1f)

    fun show(message: String = "", progress: Float = -1f, cancelable: Boolean = true) {
        dismiss()
        messageState.value = message
        progressState.value = progress
        dialog = ComposeDialogWrapper(context) {
            ProgressDialog(
                message = messageState.value,
                progress = progressState.value,
                cancelable = cancelable,
                onDismiss = { dismiss() }
            )
        }.apply {
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            show()
        }
    }

    fun updateProgress(progress: Float) {
        progressState.value = progress
    }

    fun updateMessage(message: String) {
        messageState.value = message
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}

class MessageDialogState(private val context: Context) {
    fun showLongText(
        title: String,
        content: String,
        buttonText: String = "I Know",
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            LongTextDialog(
                title = title,
                content = content,
                buttonText = buttonText,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }

    fun showNotification(
        message: String,
        showCheckbox: Boolean = true,
        checkboxLabel: String = "Don't show again",
        buttonText: String = "I Know",
        onConfirm: (dontShowAgain: Boolean) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = ComposeDialogWrapper(context) {
            NotificationDialog(
                message = message,
                showCheckbox = showCheckbox,
                checkboxLabel = checkboxLabel,
                buttonText = buttonText,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
        dialog.show()
    }
}

class FirmwareDialogState(private val context: Context) {
    fun show(
        title: String,
        size: String = "",
        content: String,
        showRestartTips: Boolean = false,
        restartTipsText: String = "Device will restart after update",
        showCancel: Boolean = true,
        cancelText: String = "Cancel",
        confirmText: String = "Confirm",
        onCancel: () -> Unit = {},
        onConfirm: () -> Unit
    ) {
        val dialog = ComposeDialogWrapper(context) {
            FirmwareUpdateDialog(
                title = title,
                size = size,
                content = content,
                showRestartTips = showRestartTips,
                restartTipsText = restartTipsText,
                showCancel = showCancel,
                cancelText = cancelText,
                confirmText = confirmText,
                onCancel = {
                    onCancel()
                },
                onConfirm = {
                    onConfirm()
                }
            )
        }
        dialog.show()
    }
}
