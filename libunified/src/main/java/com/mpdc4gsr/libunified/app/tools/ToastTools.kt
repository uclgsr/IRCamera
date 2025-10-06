package com.mpdc4gsr.libunified.app.tools
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R
object ToastTools {
    var mPublicToast: Toast? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    fun showShort(
        @StringRes textStr: Int,
    ) {
        showShort(ContextProvider.getContext().getString(textStr))
    }
    fun showShort(textStr: String) {
        showShort(textStr, Toast.LENGTH_SHORT)
    }
    fun showShort(
        textStr: String,
        duration: Int,
    ) {
        mainHandler.post {
            val context = ContextProvider.getContext()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.toast_tip, null)
            val text = view.findViewById(R.id.toast_tip_text) as TextView
            text.text = textStr
            val screenHeight = context.resources.displayMetrics.heightPixels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mPublicToast = Toast.makeText(context, textStr, duration)
                mPublicToast?.setGravity(Gravity.BOTTOM, 0, screenHeight / 8)
            } else {
                if (mPublicToast == null) {
                    mPublicToast = Toast(context)
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
