package com.mpdc4gsr.module.thermalunified.adapter
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mpdc4gsr.libunified.app.bean.HouseRepPreviewAlbumItemBean
import com.mpdc4gsr.module.thermalunified.R
@SuppressLint("NotifyDataSetChanged")
class ReportPreviewAlbumAdapter(
    private val cxt: Context,
    private var dataList: List<HouseRepPreviewAlbumItemBean>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var jumpListener: ((item: HouseRepPreviewAlbumItemBean, position: Int) -> Unit)? = null
    override fun getItemViewType(position: Int): Int {
        return position
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return ItemView(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_report_album_child, parent, false),
        )
    }
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
    override fun getItemCount(): Int {
        return dataList.size
    }
    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rivPhoto: ImageView = itemView.findViewById(R.id.riv_photo)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
    }
}
