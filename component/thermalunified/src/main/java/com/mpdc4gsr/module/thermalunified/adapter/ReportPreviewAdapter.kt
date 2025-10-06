package com.mpdc4gsr.module.thermalunified.adapter
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.bean.HouseRepPreviewItemBean
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.module.thermalunified.R
@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAdapter(private val cxt: Context, var dataList: List<HouseRepPreviewItemBean>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return position
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return ItemView(
            LayoutInflater.from(parent.context).inflate(R.layout.item_report_floor, parent, false),
        )
    }
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val data = dataList[position]
        if (holder is ItemView) {
            holder.tvFloorNumber.text = data.itemName
            holder.rcyReport.layoutManager = LinearLayoutManager(cxt)
            val reportPreviewAdapter =
                ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
            holder.rcyReport.adapter = reportPreviewAdapter
            if (!data.projectItemBeans.isNullOrEmpty()) {
                holder.flyProject.visibility = View.VISIBLE
                holder.rcyCategory.layoutManager = LinearLayoutManager(cxt)
                val reportCategoryAdapter =
                    ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
                holder.rcyCategory.adapter = reportCategoryAdapter
            } else {
                holder.flyProject.visibility = View.GONE
            }
            if (!data.albumItemBeans.isNullOrEmpty()) {
                holder.llyAlbum.visibility = View.VISIBLE
                holder.rcyAlbum.layoutManager = GridLayoutManager(cxt, 3)
                val albumAdapter = ReportPreviewAlbumAdapter(cxt, data.albumItemBeans)
                holder.rcyAlbum.adapter = albumAdapter
                albumAdapter.jumpListener = { _, position ->
                    TToast.shortToast(cxt, "Image detail view disabled - house module removed")
                }
            } else {
                holder.llyAlbum.visibility = View.GONE
            }
            holder.hsvReport.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                }
                false
            }
        }
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFloorNumber: TextView = itemView.findViewById(R.id.tv_floor_number)
        val rcyReport: RecyclerView = itemView.findViewById(R.id.rcy_report)
        val rcyCategory: RecyclerView = itemView.findViewById(R.id.rcy_category)
        val llyAlbum: LinearLayout = itemView.findViewById(R.id.lly_album)
        val rcyAlbum: RecyclerView = itemView.findViewById(R.id.rcy_album)
        val flyProject: View = itemView.findViewById(R.id.fly_project)
        val hsvReport: View = itemView.findViewById(R.id.hsv_report)
        val viewCategoryMask: View = itemView.findViewById(R.id.view_category_mask)
    }
}
