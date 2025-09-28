package com.mpdc4gsr.module.thermalunified.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.mpdc4gsr.module.thermalunified.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeGuideDialog(context: Context, private val currentStep: Int) :
    Dialog(context, R.style.TransparentDialog) {

    var onNextClickListener: ((step: Int) -> Unit)? = null

    var onSkinClickListener: (() -> Unit)? = null

    private lateinit var ivBlurBg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_home_guide, null))

        ivBlurBg = findViewById(R.id.iv_blur_bg)
        val clGuide1: View = findViewById(R.id.cl_guide_1)
        val clGuide2: View = findViewById(R.id.cl_guide_2)
        val clGuide3: View = findViewById(R.id.cl_guide_3)
        val tvNext1: View = findViewById(R.id.tv_next1)
        val tvNext2: View = findViewById(R.id.tv_next2)
        val tvIKnow: View = findViewById(R.id.tv_i_know)
        val tvSkin1: View = findViewById(R.id.tv_skin1)
        val tvSkin2: View = findViewById(R.id.tv_skin2)

        when (currentStep) {
            1 -> {
                clGuide1.isVisible = true
                clGuide2.isVisible = false
                clGuide3.isVisible = false
            }

            2 -> {
                clGuide1.isVisible = false
                clGuide2.isVisible = true
                clGuide3.isVisible = false
            }

            3 -> {
                clGuide1.isVisible = false
                clGuide2.isVisible = false
                clGuide3.isVisible = true
            }
        }

        tvNext1.setOnClickListener {
            onNextClickListener?.invoke(1)
            clGuide1.isVisible = false
            clGuide2.isVisible = true
        }
        tvNext2.setOnClickListener {
            onNextClickListener?.invoke(2)
            clGuide2.isVisible = false
            clGuide3.isVisible = true
        }
        tvIKnow.setOnClickListener {
            onNextClickListener?.invoke(3)
            dismiss()
        }

        tvSkin1.setOnClickListener {
            onSkinClickListener?.invoke()
            dismiss()
        }
        tvSkin2.setOnClickListener {
            onSkinClickListener?.invoke()
            dismiss()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        onSkinClickListener?.invoke()
    }

    fun blurBg(rootView: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sourceBitmap =
                    Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val outputBitmap =
                    Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sourceBitmap)
                rootView.draw(canvas)

                val renderScript = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    RenderScript.create(context)
                } else null
                val inputAllocation = if (renderScript != null) {
                    @Suppress("DEPRECATION")
                    Allocation.createFromBitmap(renderScript, sourceBitmap)
                } else null
                val outputAllocation = if (renderScript != null && inputAllocation != null) {
                    @Suppress("DEPRECATION")
                    Allocation.createTyped(renderScript, inputAllocation.type)
                } else null

                if (renderScript != null && inputAllocation != null && outputAllocation != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    val blurScript = @Suppress("DEPRECATION")
                    ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                    @Suppress("DEPRECATION")
                    blurScript.setRadius(20f)
                    @Suppress("DEPRECATION")
                    blurScript.setInput(inputAllocation)
                    @Suppress("DEPRECATION")
                    blurScript.forEach(outputAllocation)
                    @Suppress("DEPRECATION")
                    outputAllocation.copyTo(outputBitmap)
                    @Suppress("DEPRECATION")
                    renderScript.destroy()
                } else {
                    // Fallback for API 31+ where RenderScript is deprecated
                    val canvas2 = Canvas(outputBitmap)
                    val paint = Paint()
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0.8f) // Slightly desaturate for blur effect
                    paint.colorFilter = ColorMatrixColorFilter(matrix)
                    paint.alpha = 128 // Make it semi-transparent for blur effect
                    canvas2.drawBitmap(sourceBitmap, 0f, 0f, paint)
                }

                launch(Dispatchers.Main) {
                    ivBlurBg.isVisible = true
                    ivBlurBg.setImageBitmap(outputBitmap)
                }
            } catch (_: Exception) {
            }
        }
    }
}
