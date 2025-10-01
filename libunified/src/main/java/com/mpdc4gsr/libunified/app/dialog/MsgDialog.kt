package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.utils.ScreenUtil

/**
 * MsgDialog - Migrated to Jetpack Compose
 * Maintains Builder API compatibility with the old databinding version
 */
class MsgDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder {
        var dialog: MsgDialog? = null

        private var context: Context? = null
        private var imgRes: Int = 0
        private var message: String? = null
        private var positiveClickListener: OnClickListener? = null

        constructor(context: Context) {
            this.context = context
        }

        fun setImg(@DrawableRes res: Int): Builder {
            this.imgRes = res
            return this
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setMessage(@StringRes message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        fun setCloseListener(listener: OnClickListener): Builder {
            this.positiveClickListener = listener
            return this
        }

        fun dismiss() {
            this.dialog?.dismiss()
        }

        fun create(): MsgDialog {
            if (dialog == null) {
                dialog = MsgDialog(context!!, R.style.InfoDialog)
            }

            val composeView = ComposeView(context!!).apply {
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        dismiss()
                                        dialog?.let { positiveClickListener?.onClick(it) }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close"
                                        )
                                    }
                                }

                                if (imgRes != 0) {
                                    Image(
                                        painter = painterResource(id = imgRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (!message.isNullOrEmpty()) {
                                    Text(
                                        text = message!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            dialog!!.setContentView(composeView)
            dialog!!.setCanceledOnTouchOutside(false)

            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    0.9
                } else {
                    0.3
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            return dialog as MsgDialog
        }
    }

    interface OnClickListener {
        fun onClick(dialog: DialogInterface)
    }
}

