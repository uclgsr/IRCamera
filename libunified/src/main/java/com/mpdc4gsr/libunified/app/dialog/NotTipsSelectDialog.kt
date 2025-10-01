package com.mpdc4gsr.libunified.app.dialog

/*
 * DEPRECATED: This file has been deprecated as part of migration to Jetpack Compose.
 * This code is commented out to avoid compilation errors when dataBinding is disabled.
 * See COMPOSE_MIGRATION.md for alternatives.
 */

/*

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ScreenUtil
import com.mpdc4gsr.libunified.databinding.DialogNotTipsSelectBinding

class NotTipsSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    @StringRes
    private var tipsResId: Int = 0
    private var onConfirmListener: ((isSelect: Boolean) -> Unit)? = null

    private val binding: DialogNotTipsSelectBinding =
        DialogNotTipsSelectBinding.inflate(layoutInflater)

    fun setTipsResId(
        @StringRes tipsResId: Int,
    ): NotTipsSelectDialog {
        this.tipsResId = tipsResId
        return this
    }

    fun setOnConfirmListener(l: ((isSelect: Boolean) -> Unit)?): NotTipsSelectDialog {
        onConfirmListener = l
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        if (tipsResId != 0) {
            binding.tvMessage.setText(tipsResId)
        }
        binding.tvSelect.setOnClickListener {
            it.isSelected = !it.isSelected
        }
        binding.tvIKnow.setOnClickListener {
            onConfirmListener?.invoke(binding.tvSelect.isSelected)
            dismiss()
        }

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.73f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
}
*/
