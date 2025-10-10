package com.mpdc4gsr.module.thermalunified.stubs

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class TipGuideDialog : DialogFragment() {
    var closeEvent: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(): TipGuideDialog = TipGuideDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog =
        AlertDialog
            .Builder(requireContext())
            .setTitle("Guide")
            .setMessage("This is a guide dialog for thermal camera functionality.")
            .setPositiveButton("Got it") { _, _ ->
                closeEvent?.invoke(false)
            }.setNegativeButton("Don't show again") { _, _ ->
                closeEvent?.invoke(true)
            }.create()
}

class TipPreviewDialog : DialogFragment() {
    var closeEvent: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(): TipPreviewDialog = TipPreviewDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog =
        AlertDialog
            .Builder(requireContext())
            .setTitle("Preview Tip")
            .setMessage("This is a preview tip dialog for thermal camera preview.")
            .setPositiveButton("Continue") { _, _ ->
                closeEvent?.invoke(false)
            }.setNegativeButton("Don't show again") { _, _ ->
                closeEvent?.invoke(true)
            }.create()
}

class TipObserveDialog {
    class Builder(
        private val context: Context,
    ) {
        private var title: String = "Tip"
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

        fun create(): TipObserveDialog =
            TipObserveDialog().apply {
                this.context = this@Builder.context
                this.title = this@Builder.title
                this.message = this@Builder.message
                this.cancelListener = this@Builder.cancelListener
            }
    }

    private lateinit var context: Context
    private var title: String = ""
    private var message: String = ""
    private var cancelListener: ((Boolean) -> Unit)? = null

    fun show() {
        val dialog =
            AlertDialog
                .Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    cancelListener?.invoke(false)
                }.setNegativeButton("Don't show again") { _, _ ->
                    cancelListener?.invoke(true)
                }.create()
        dialog.show()
    }
}

class TipDialog {
    class Builder(
        private val context: Context,
    ) {
        private var title: String = "Tip"
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

        fun setPositiveListener(
            resId: Int,
            listener: () -> Unit,
        ): Builder {
            this.positiveListener = listener
            return this
        }

        fun setNegativeListener(
            resId: Int,
            listener: () -> Unit,
        ): Builder {
            this.negativeListener = listener
            return this
        }

        fun create(): TipDialog =
            TipDialog().apply {
                this.context = this@Builder.context
                this.title = this@Builder.title
                this.message = this@Builder.message
                this.positiveListener = this@Builder.positiveListener
                this.negativeListener = this@Builder.negativeListener
            }
    }

    private lateinit var context: Context
    private var title: String = ""
    private var message: String = ""
    private var positiveListener: (() -> Unit)? = null
    private var negativeListener: (() -> Unit)? = null

    fun show() {
        val builder =
            AlertDialog
                .Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes") { _, _ ->
                    positiveListener?.invoke()
                }
        negativeListener?.let {
            builder.setNegativeButton("No") { _, _ ->
                it.invoke()
            }
        }
        builder.create().show()
    }
}

class TempAlarmSetDialog {
    class Builder(
        private val context: Context,
    ) {
        private var numText: String = ""

        fun setNum(num: String): Builder {
            numText = num
            return this
        }

        fun create(): TempAlarmSetDialog =
            TempAlarmSetDialog().apply {
                this.context = this@Builder.context
                this.numText = this@Builder.numText
            }
    }

    private lateinit var context: Context
    private var numText: String = ""

    fun show() {
        AlertDialog
            .Builder(context)
            .setTitle("Temperature Alarm")
            .setMessage("Set temperature alarm: $numText")
            .setPositiveButton("OK") { _, _ -> }
            .setNegativeButton("Cancel") { _, _ -> }
            .create()
            .show()
    }
}
