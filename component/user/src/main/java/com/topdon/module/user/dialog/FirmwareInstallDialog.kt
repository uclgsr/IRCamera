package com.topdon.module.user.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.module.user.R
import com.topdon.lib.core.R as LibAppR


class FirmwareInstallDialog(context: Context) : Dialog(context, LibAppR.style.TransparentDialog) {
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.dialog_firmware_install, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(rootView)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.3).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
}
