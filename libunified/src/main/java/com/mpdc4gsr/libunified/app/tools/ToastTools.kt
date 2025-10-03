package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ToastTools {
    var mPublicToast: Toast? = null

    fun showShort(
        @StringRes textStr: Int,
    ) {
        showShort(ContextProvider.INSTANCE.getContext().getString(textStr))
    }

    fun showShort(textStr: String) {
        showShort(textStr, Toast.LENGTH_SHORT)
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun showShort(
        textStr: String,
        duration: Int,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val inflater =
                ContextProvider.INSTANCE.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.toast_tip, null)
            val text = view.findViewById(R.id.toast_tip_text) as TextView
            text.text = textStr
            val screenHeight = ContextProvider.getContext().resources.displayMetrics.heightPixels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                mPublicToast = Toast.makeText(ContextProvider.getContext(), textStr, duration)
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, screenHeight / 8)
            } else {

                if (mPublicToast == null) {
                    mPublicToast = Toast(ContextProvider.INSTANCE.getContext())
                }
                mPublicToast?.duration = duration
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, screenHeight / 8)
                @Suppress("DEPRECATION")
                mPublicToast?.view = view
            }
            mPublicToast?.show()
        }
    }
}
