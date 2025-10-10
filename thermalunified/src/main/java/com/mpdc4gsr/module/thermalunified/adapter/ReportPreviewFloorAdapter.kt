package com.mpdc4gsr.module.thermalunified.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.bean.HouseRepPreviewProjectItemBean
import com.mpdc4gsr.module.thermalunified.R

@SuppressLint("NotifyDataSetChanged")
class ReportPreviewFloorAdapter(
    val cxt: Context,
    var dataList: List<HouseRepPreviewProjectItemBean>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        ItemView(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_report_floor_child, parent, false),
        )

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val bean = dataList[position]
        if (holder is ItemView) {
            holder.ivProblemState.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            holder.ivRepairState.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            holder.ivReplaceState.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            holder.tvProblem.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
            holder.tvRepair.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
            holder.tvReplace.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
            holder.rlyParent.setBackgroundColor(
                if (position == 0) {
                    Color.parseColor("#393643")
                } else {
                    Color.parseColor(
                        "#23202E",
                    )
                },
            )
            if (position == 0) {
                holder.tvProject.text = cxt.getString(R.string.pdf_project_item)
                holder.tvRemark.text = cxt.getString(R.string.report_remark)
            } else {
                holder.tvProject.text = bean.projectName
                holder.tvRemark.text = bean.remark
                when (bean.state) {
                    1 -> {
                        holder.ivProblemState.visibility = View.VISIBLE
                        holder.ivRepairState.visibility = View.INVISIBLE
                        holder.ivReplaceState.visibility = View.INVISIBLE
                    }

                    2 -> {
                        holder.ivProblemState.visibility = View.INVISIBLE
                        holder.ivRepairState.visibility = View.VISIBLE
                        holder.ivReplaceState.visibility = View.INVISIBLE
                    }

                    3 -> {
                        holder.ivProblemState.visibility = View.INVISIBLE
                        holder.ivRepairState.visibility = View.INVISIBLE
                        holder.ivReplaceState.visibility = View.VISIBLE
                    }

                    else -> {
                        holder.ivProblemState.visibility = View.INVISIBLE
                        holder.ivRepairState.visibility = View.INVISIBLE
                        holder.ivReplaceState.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    inner class ItemView(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val tvProject: TextView = itemView.findViewById(R.id.tv_project)
        val tvProblem: TextView = itemView.findViewById(R.id.tv_problem)
        val ivProblemState: ImageView = itemView.findViewById(R.id.iv_problem)
        val tvRepair: TextView = itemView.findViewById(R.id.tv_repair)
        val ivRepairState: ImageView = itemView.findViewById(R.id.iv_repair)
        val tvReplace: TextView = itemView.findViewById(R.id.tv_replace)
        val ivReplaceState: ImageView = itemView.findViewById(R.id.iv_replace)
        val tvRemark: TextView = itemView.findViewById(R.id.tv_remark)
        val rlyParent: View = itemView.findViewById(R.id.rly_parent)
    }
}
