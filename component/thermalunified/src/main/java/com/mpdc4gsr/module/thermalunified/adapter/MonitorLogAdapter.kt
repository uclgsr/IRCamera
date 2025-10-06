package com.mpdc4gsr.module.thermalunified.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.module.thermalunified.R
class MonitorLogAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: OnItemClickListener? = null
    var datas = arrayListOf<ThermalEntity>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return ItemView(view)
    }
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            val data = datas[position]
            holder.indexText.text = "${position + 1}"
            holder.timeText.text = TimeTools.showTimeSecond(data.createTime)
            holder.lay.setOnClickListener {
                listener?.onClick(position, data.thermalId)
            }
            holder.lay.setOnLongClickListener(
                View.OnLongClickListener {
                    listener?.onLongClick(position, data.thermalId)
                    return@OnLongClickListener true
                },
            )
        }
    }
    override fun getItemCount(): Int {
        return datas.size
    }
    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay =
            itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.item_log_lay)
        val indexText = itemView.findViewById<TextView>(R.id.item_log_index_text)
        val timeText = itemView.findViewById<TextView>(R.id.item_log_time_text)
    }
    interface OnItemClickListener {
        fun onClick(
            index: Int,
            thermalId: String,
        )
        fun onLongClick(
            index: Int,
            thermalId: String,
        )
    }
}
