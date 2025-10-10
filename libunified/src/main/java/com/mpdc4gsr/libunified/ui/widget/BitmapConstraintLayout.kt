package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.libunified.app.utils.LibraryLogger

open class BitmapConstraintLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    @Volatile
    var viewBitmap: Bitmap? = null

    fun updateBitmap() {
        if (!isShown) {
            return
        }
        try {
            viewBitmap = this.drawToBitmap()
        } catch (exception: Exception) {
            com.mpdc4gsr.libunified.app.utils.LibraryLogger.e(
                "BitmapConstraintLayout",
                "Unexpected Exception in BitmapConstraintLayout catch block",
                exception,
            )
        }
    }
}
