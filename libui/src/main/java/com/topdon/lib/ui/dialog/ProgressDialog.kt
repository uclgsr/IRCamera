package com.topdon.lib.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import com.topdon.lib.core.R
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.databinding.DialogProgressBinding

/**
 * 带进度条的提示弹框.
 */
class ProgressDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    private val binding: DialogProgressBinding = DialogProgressBinding.inflate(LayoutInflater.from(context))

    var max: Int = 100
        set(value) {
            binding.progressBar.max = value
            field = value
        }

    var progress: Int = 0
        set(value) {
            binding.progressBar.progress = value
            field = value
        }

    init {
        // Binding is initialized in constructor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (ScreenUtil.isPortrait(context)) 0.8 else 0.45).toInt()
            layoutParams.height = LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    override fun show() {
        super.show()
        binding.progressBar.max = max
        binding.progressBar.progress = progress
    }
}
