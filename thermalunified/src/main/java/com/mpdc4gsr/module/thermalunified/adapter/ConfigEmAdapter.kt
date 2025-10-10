package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.utils.IRConfigData

class ConfigEmAdapter(
    val context: Context,
) : RecyclerView.Adapter<ConfigEmAdapter.ViewHolder>() {
    private val dataList: ArrayList<IRConfigData> = IRConfigData.irConfigData(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_ir_config_emissivity, parent, false),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.tvEmName.text = dataList[position].name
        holder.tvEmNum.text = dataList[position].value
        holder.tvEmName.background = EmBgDrawable(false, position == dataList.size - 1)
        holder.tvEmNum.background = EmBgDrawable(true, position == dataList.size - 1)
    }

    override fun getItemCount(): Int = dataList.size

    class ViewHolder(
        rootView: View,
    ) : RecyclerView.ViewHolder(rootView) {
        val tvEmName: TextView = rootView.findViewById(R.id.tv_em_name)
        val tvEmNum: TextView = rootView.findViewById(R.id.tv_em_num)
    }

    private inner class EmBgDrawable(
        val drawRight: Boolean,
        val drawBottom: Boolean,
    ) : Drawable() {
        private val paint = Paint()

        init {
            paint.color = 0xff5b5961.toInt()
            paint.strokeWidth = 1f.dpToPx(context).coerceAtLeast(1f).toFloat()
        }

        override fun draw(canvas: Canvas) {
            canvas.drawLine(0f, 0f, 0f, bounds.bottom.toFloat(), paint)
            canvas.drawLine(0f, 0f, bounds.right.toFloat(), 0f, paint)
            if (drawRight) {
                canvas.drawLine(
                    bounds.right.toFloat(),
                    0f,
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    paint,
                )
            }
            if (drawBottom) {
                canvas.drawLine(
                    0f,
                    bounds.bottom.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    paint,
                )
            }
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        @Deprecated("This method is no longer used in graphics optimizations")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
