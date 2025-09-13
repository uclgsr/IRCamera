package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogLoadingBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * 新版 UI 的那个 LMS 的load中弹框，由于 LMS 的弹框没有text，只好自己再搞一个了。
 *
 * Created by LCG on 2024/4/12.
 */
class LoadingDialog(context: Context) : Dialog(context, R.style.TransparentDialog) {
    private val binding: DialogLoadingBinding = DialogLoadingBinding.inflate(LayoutInflater.from(context))

    fun setTips(
        @StringRes resId: Int,
    ) {
        binding.tvTips.setText(resId)
        binding.tvTips.isVisible = true
    }

    fun setTips(text: CharSequence?) {
        binding.tvTips.text = text
        binding.tvTips.isVisible = text?.isNotEmpty() == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (ScreenUtil.isPortrait(context)) 0.3 else 0.15).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
}
