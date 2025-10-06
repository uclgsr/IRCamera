package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.SurfaceView
import java.nio.ByteBuffer

class LiteSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {
    var mFinalImageWidth = 0
    var mFinalImageHeight = 0
    var tmpData: ByteArray? = null
    var mIrRotateData: ByteArray? = null
    var imageBitmap: Bitmap? = null
    fun scaleBitmap(): Bitmap {
        try {
            val irData =
                mIrRotateData ?: return Bitmap.createBitmap(
                    measuredWidth,
                    measuredHeight,
                    Bitmap.Config.ARGB_8888
                )
            if (tmpData == null) {
                tmpData = ByteArray(irData.size)
            }
            val tempData = tmpData ?: return Bitmap.createBitmap(
                measuredWidth,
                measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            System.arraycopy(irData, 0, tempData, 0, irData.size)
            if (imageBitmap == null || imageBitmap!!.getWidth() != mFinalImageWidth) {
                imageBitmap =
                    Bitmap.createBitmap(
                        mFinalImageWidth,
                        mFinalImageHeight,
                        Bitmap.Config.ARGB_8888
                    )
            }
            imageBitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(tempData))
            return Bitmap.createScaledBitmap(
                imageBitmap!!,
                measuredWidth, measuredHeight, true
            )
        } catch (e: Exception) {
            return Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        }
    }
}