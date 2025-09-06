package com.topdon.module.thermal.ir.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.CollectionUtils
import com.topdon.house.activity.ImagesDetailActivity
import com.topdon.lib.core.bean.HouseRepPreviewItemBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.view.DetectHorizontalScrollView.OnScrollStopListner


@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAdapter(private val cxt: Context, var dataList: List<HouseRepPreviewItemBean>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemView(
            LayoutInflater.from(parent.context).inflate(R.layout.item_report_floor, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataList[position]
        if (holder is ItemView) {
            holder.tvFloorNumber.text = data.itemName

            holder.rcyReport.layoutManager = LinearLayoutManager(cxt)
            val reportPreviewAdapter =
                ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
            holder.rcyReport.adapter = reportPreviewAdapter

            if (CollectionUtils.isNotEmpty(data.projectItemBeans)) {
                holder.flyProject.visibility = View.VISIBLE
                holder.rcyCategory.layoutManager = LinearLayoutManager(cxt)
                val reportCategoryAdapter =
                    ReportPreviewFloorAdapter(cxt, data.projectItemBeans)
                holder.rcyCategory.adapter = reportCategoryAdapter
            } else {
                holder.flyProject.visibility = View.GONE
            }

            if (CollectionUtils.isNotEmpty(data.albumItemBeans)) {
                holder.llyAlbum.visibility = View.VISIBLE
                holder.rcyAlbum.layoutManager = GridLayoutManager(cxt, 3)
                val albumAdapter = ReportPreviewAlbumAdapter(cxt, data.albumItemBeans)
                holder.rcyAlbum.adapter = albumAdapter
            albumAdapter.jumpListener = { _, position ->
                var intent = Intent(cxt, ImagesDetailActivity::class.java)
                var photos = ArrayList<String>()
                data.albumItemBeans.forEach {
                    photos.add(it.photoPath)
                }
                intent.putExtra(ExtraKeyConfig.IMAGE_PATH_LIST, photos)
                intent.putExtra(ExtraKeyConfig.CURRENT_ITEM, position)
                cxt.startActivity(intent)
            }
        } else {
            holder.llyAlbum.visibility = View.GONE
        }

        holder.hsvReport.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Generic view doesn't have startScrollerTask method
                // holder.hsvReport.startScrollerTask()
            }
            false
        }

        // Scroll listener commented out due to type issues - would need proper MHorizontalScrollView import
        /*
        holder.hsvReport.setOnScrollStopListner(object : OnScrollStopListner {
            override fun onScrollToRightEdge() {
                holder.viewCategoryMask.visibility = View.VISIBLE
            }

            override fun onScrollToMiddle() {
                holder.viewCategoryMask.visibility = View.VISIBLE
            }

            override fun onScrollToLeftEdge() {
                holder.viewCategoryMask.visibility = View.GONE
            }

            override fun onScrollStoped() {
            }

            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                if (holder.viewCategoryMask.visibility == View.VISIBLE) {
                    return
                }
                holder.viewCategoryMask.visibility = View.VISIBLE
            }
        })
        */
        } // End of if (holder is ItemView) block
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