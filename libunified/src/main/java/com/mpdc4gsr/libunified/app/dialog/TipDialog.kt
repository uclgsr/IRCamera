package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

/**
 * TipDialog - Migrated to Jetpack Compose
 * Maintains Builder API compatibility with the old databinding version
 */
class TipDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    @Deprecated("This method is deprecated")
    override fun onBackPressed() {
    }

    class Builder(private val context: Context) {
        var dialog: TipDialog? = null

        private var message: String? = null
        private var titleMessage: String? = null
        private var positiveStr: String? = null
        private var cancelStr: String? = null
        private var positiveEvent: (() -> Unit)? = null
        private var cancelEvent: (() -> Unit)? = null
        private var canceled = false
        private var isShowRestartTips = false

        fun setTitleMessage(message: String): Builder {
            this.titleMessage = message
            return this
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setMessage(@StringRes message: Int): Builder {
            this.message = context.getString(message)
            return this
        }

        fun setPositiveListener(@StringRes strRes: Int, event: (() -> Unit)? = null): Builder {
            return setPositiveListener(context.getString(strRes), event)
        }

        fun setPositiveListener(str: String, event: (() -> Unit)? = null): Builder {
            this.positiveStr = str
            this.positiveEvent = event
            return this
        }

        fun setCancelListener(@StringRes strRes: Int, event: (() -> Unit)? = null): Builder {
            return setCancelListener(context.getString(strRes), event)
        }

        fun setCancelListener(str: String, event: (() -> Unit)? = null): Builder {
            this.cancelStr = str
            this.cancelEvent = event
            return this
        }

        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        fun setShowRestartTops(isShowRestartTips: Boolean): Builder {
            this.isShowRestartTips = isShowRestartTips
            return this
        }

        fun dismiss() {
            this.dialog?.dismiss()
        }

        fun create(): TipDialog {
            if (dialog == null) {
                dialog = TipDialog(context, R.style.InfoDialog)
            }

            val isPortrait =
                context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val widthPixels = context.resources.displayMetrics.widthPixels
            
            val composeView = ComposeView(context).apply {
                setContent {
                    LibUnifiedTheme {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!titleMessage.isNullOrEmpty()) {
                                    Text(
                                        text = titleMessage,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (!message.isNullOrEmpty()) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (isShowRestartTips) {
                                    Text(
                                        text = context.getString(R.string.ts004_store_reset),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!cancelStr.isNullOrEmpty()) {
                                        TextButton(onClick = {
                                            dismiss()
                                            cancelEvent?.invoke()
                                        }) {
                                            Text(cancelStr)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Button(onClick = {
                                        dismiss()
                                        positiveEvent?.invoke()
                                    }) {
                                        Text(positiveStr ?: context.getString(android.R.string.ok))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            dialog!!.setContentView(composeView)
            dialog!!.setCanceledOnTouchOutside(canceled)
            
            val lp = dialog!!.window!!.attributes
            lp.width = (widthPixels * if (isPortrait) 0.85 else 0.35).toInt()
            dialog!!.window!!.attributes = lp

            return dialog as TipDialog
        }
    }
}

