package com.mpdc4gsr.lib.core.tools

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.lib.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


object ToastTools {
    var mPublicToast: Toast? = null

    fun showShort(
        @StringRes textStr: Int,
    ) {
        showShort(Utils.getApp().getString(textStr))
    }

    fun showShort(textStr: String) {
        showShort(textStr, Toast.LENGTH_SHORT)
    }

    fun showShort(
        textStr: String,
        duration: Int,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val inflater =
                Utils.getApp().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.toast_tip, null)
            val text = view.findViewById(R.id.toast_tip_text) as TextView
            text.text = textStr
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                mPublicToast = Toast.makeText(Utils.getApp(), textStr, duration)
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, ScreenUtils.getScreenHeight() / 8)
            } else {

                if (mPublicToast == null) {
                    mPublicToast = Toast(Utils.getApp())
                }
                mPublicToast?.duration = duration
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, ScreenUtils.getScreenHeight() / 8)
                @Suppress("DEPRECATION")
                mPublicToast?.view = view
            }
            mPublicToast?.show()
        }
    }
}
