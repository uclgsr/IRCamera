package com.topdon.module.thermal.ir.dialog

import android.annotation.SuppressLint
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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.ui.widget.MyItemDecoration
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.adapter.ConfigEmAdapter
import com.topdon.module.thermal.ir.bean.DataBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 温度修正操作指引.
 *
 * Created by LCG on 2024/11/13.
 */
class ConfigGuideDialog(context: Context, val isTC007: Boolean, val dataBean: DataBean) : Dialog(context, R.style.TransparentDialog) {

    // Initialize views with findViewById
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

        // Initialize views
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

        tvDefaultTempTitle.text = "${context.getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
        tvDefaultDisTitle.text = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
        tvSpaceEmTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"

        tvDefaultEmTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
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
                val sourceBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val outputBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sourceBitmap)
                rootView.draw(canvas)

                val renderScript = RenderScript.create(context)
                val inputAllocation = Allocation.createFromBitmap(renderScript, sourceBitmap)
                val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)

                val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
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