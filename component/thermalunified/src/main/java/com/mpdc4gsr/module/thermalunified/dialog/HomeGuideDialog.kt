package com.mpdc4gsr.module.thermalunified.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.kotlinx.coroutines.CoroutineScope
import com.kotlinx.coroutines.Dispatchers
import com.kotlinx.coroutines.launch


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

    override fun onBackPressed() {
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

                val renderScript = RenderScript.create(context)
                val inputAllocation = Allocation.createFromBitmap(renderScript, sourceBitmap)
                val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)

                val blurScript =
                    ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                blurScript.setRadius(20f)
                blurScript.setInput(inputAllocation)
                blurScript.forEach(outputAllocation)
                outputAllocation.copyTo(outputBitmap)
                renderScript.destroy()

                launch(Dispatchers.Main) {
                    ivBlurBg.isVisible = true
                    ivBlurBg.setImageBitmap(outputBitmap)
                }
            } catch (_: Exception) {
            }
        }
    }
}
