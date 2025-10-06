package com.mpdc4gsr.module.thermalunified.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.module.thermalunified.R
class SettingTimeAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var datas = arrayOf("1min", "5min", "10min", "30min")
    private var dataTimes = arrayOf(60, 300, 600, 1800)
    var listener: OnItemClickListener? = null
    var select = 0
    fun setCheck(index: Int) {
        this.select = index
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_setting_time, parent, false)
        return ItemView(view)
    }
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            holder.btn.text = datas[position]
            if (position == select) {
                holder.btn.setBackgroundResource(R.drawable.ui_btn_round_theme)
                holder.btn.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.mpdc4gsr.libunified.R.color.white
                    )
                )
            } else {
                holder.btn.background = null
                holder.btn.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.mpdc4gsr.libunified.R.color.font_third_color
                    )
                )
            }
            holder.btn.setOnClickListener {
                listener?.onClick(position, dataTimes[position])
                setCheck(position)
            }
        }
    }
    override fun getItemCount(): Int {
        return datas.size
    }
    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btn: Button = itemView.findViewById(R.id.item_setting_time_btn)
    }
    interface OnItemClickListener {
        fun onClick(
            index: Int,
            time: Int,
        )
    }
}
