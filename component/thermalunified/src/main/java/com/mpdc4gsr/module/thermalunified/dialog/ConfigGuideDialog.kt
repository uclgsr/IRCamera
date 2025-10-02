@file:Suppress("DEPRECATION")

package com.mpdc4gsr.module.thermalunified.dialog

import android.annotation.SuppressLint
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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.tools.NumberTools
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ui.widget.MyItemDecoration
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.adapter.ConfigEmAdapter
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @deprecated This XML-based dialog is deprecated. Use ConfigGuideDialogCompose instead.
 * See docs/XML_TO_COMPOSE_MIGRATION.md for migration guide.
 */
@Deprecated(
    message = "Use ConfigGuideDialogCompose from compose package instead",
    replaceWith = ReplaceWith(
        "ConfigGuideDialogCompose",
        "com.mpdc4gsr.module.thermalunified.compose.ConfigGuideDialogCompose"
    )
)
class ConfigGuideDialog(context: Context, val isTC007: Boolean, val dataBean: DataBean) :
    Dialog(context, R.style.TransparentDialog) {

    private lateinit var tvDefaultTempTitle: TextView
    private lateinit var tvDefaultDisTitle: TextView
    private lateinit var tvSpaceEmTitle: TextView
    private lateinit var tvDefaultEmTitle: TextView
    private lateinit var tvDefaultEmValue: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var clStep1: ConstraintLayout
    private lateinit var clStep2Top: ConstraintLayout
    private lateinit var clStep2Bottom: ConstraintLayout
    private lateinit var tvNext: TextView
    private lateinit var tvIKnow: TextView
    private lateinit var ivBlurBg: ImageView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_config_guide, null))

        tvDefaultTempTitle = findViewById(R.id.tv_default_temp_title)
        tvDefaultDisTitle = findViewById(R.id.tv_default_dis_title)
        tvSpaceEmTitle = findViewById(R.id.tv_space_em_title)
        tvDefaultEmTitle = findViewById(R.id.tv_default_em_title)
        tvDefaultEmValue = findViewById(R.id.tv_default_em_value)
        recyclerView = findViewById(R.id.recycler_view)
        clStep1 = findViewById(R.id.cl_step1)
        clStep2Top = findViewById(R.id.cl_step2_top)
        clStep2Bottom = findViewById(R.id.cl_step2_bottom)
        tvNext = findViewById(R.id.tv_next)
        tvIKnow = findViewById(R.id.tv_i_know)
        ivBlurBg = findViewById(R.id.iv_blur_bg)

        tvDefaultTempTitle.text = "${context.getString(R.string.thermal_config_environment)} ${
            UnitTools.showConfigC(
                -10,
                if (isTC007) 50 else 55
            )
        }"
        tvDefaultDisTitle.text =
            "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
        tvSpaceEmTitle.text =
            "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"

        tvDefaultEmTitle.text =
            "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
        tvDefaultEmValue.text = NumberTools.to02(dataBean.radiation)

        val itemDecoration = MyItemDecoration(context)
        itemDecoration.wholeBottom = 20f

        recyclerView.addItemDecoration(itemDecoration)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ConfigEmAdapter(context)

        clStep1.isVisible = SharedManager.configGuideStep == 1
        clStep2Top.isVisible = SharedManager.configGuideStep == 2
        clStep2Bottom.isVisible = SharedManager.configGuideStep == 2

        tvNext.setOnClickListener {
            clStep1.isVisible = false
            clStep2Top.isVisible = true
            clStep2Bottom.isVisible = true
            SharedManager.configGuideStep = 2
        }
        tvIKnow.setOnClickListener {
            dismiss()
            SharedManager.configGuideStep = 0
        }
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
