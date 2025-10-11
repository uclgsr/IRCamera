package com.mpdc4gsr.component.thermal.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mpdc4gsr.component.shared.app.bean.HouseRepPreviewAlbumItemBean
import com.mpdc4gsr.component.thermal.R

@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAlbumAdapter(
    private val cxt: Context,
    private var dataList: List<HouseRepPreviewAlbumItemBean>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var jumpListener: ((item: HouseRepPreviewAlbumItemBean, position: Int) -> Unit)? = null

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        ItemView(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_report_album_child, parent, false),
        )

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val bean = dataList[position]
        if (holder is ItemView) {
            holder.rivPhoto.load(bean.photoPath)
            holder.tvName.text = bean.title
            holder.rivPhoto.setOnClickListener {
                jumpListener?.invoke(bean, position)
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    inner class ItemView(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val rivPhoto: ImageView = itemView.findViewById(R.id.riv_photo)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
    }
}



