package com.mpdc4gsr.component.shared.app.compose.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import kotlinx.coroutines.delay

@Composable
fun ComposeToast(
    message: String,
    duration: Long = 2000L,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(duration)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 80.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xCC000000),
            shadowElevation = 8.dp,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

object ComposeToastHelper {
    private var currentToast: android.app.Dialog? = null

    fun show(
        context: Context,
        message: String,
        duration: Long = 2000L,
    ) {
        dismiss()
        currentToast =
            android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar).apply {
                val composeView =
                    ComposeView(context).apply {
                        setContent {
                            LibSharedTheme {
                                ComposeToast(
                                    message = message,
                                    duration = duration,
                                    onDismiss = { dismiss() },
                                )
                            }
                        }
                    }
                setContentView(composeView)
                window?.apply {
                    setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundDrawableResource(android.R.color.transparent)
                    clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    fun show(
        context: Context,
        @StringRes resId: Int,
        duration: Long = 2000L,
    ) {
        show(context, context.getString(resId), duration)
    }

    fun dismiss() {
        currentToast?.dismiss()
        currentToast = null
    }
}



