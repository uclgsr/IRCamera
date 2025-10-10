package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.tools.CoilLoader
import com.mpdc4gsr.module.thermalunified.R

class GalleryAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: OnItemClickListener? = null

    // Properties needed by ReportPickImgActivity
    var isEditMode: Boolean = false
    var selectList = mutableListOf<GalleryBean>()
    var dataList = arrayListOf<Any>()  // Can contain String paths or GalleryTitle objects
    var onLongEditListener: (() -> Unit)? = null
    var selectCallback: ((List<GalleryBean>) -> Unit)? = null
    var itemClickCallback: ((Int) -> Unit)? = null
    var isTS004Remote: Boolean = false
    var datas = arrayListOf<String>()
        set(value) {
            field = value
            dataList.clear()
            dataList.addAll(value)
            notifyDataSetChanged()
        }

    fun refreshList(data: List<Any>) {
        dataList.clear()
        dataList.addAll(data)
        datas.clear()
        // Filter only String paths for backward compatibility
        datas.addAll(data.filterIsInstance<String>())
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectList.clear()
        // Convert string paths to GalleryBean objects for compatibility
        selectList.addAll(datas.map { path ->
            GalleryBean(
                id = 0,
                path = path,
                thumb = path,
                name = java.io.File(path).name,
                duration = 0L,
                timeMillis = System.currentTimeMillis(),
                hasDownload = true
            )
        })
        selectCallback?.invoke(selectList)
    }

    fun buildSelectList(): List<GalleryBean> {
        return selectList.toList()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false)
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            CoilLoader.load(holder.img, datas[position])
            holder.lay.setOnClickListener {
                Log.w("123", ": ${datas[position]}")
                listener?.onClick(position, datas[position])
            }
            holder.lay.setOnLongClickListener(
                View.OnLongClickListener {
                    Log.w("123", ": ${datas[position]}")
                    listener?.onLongClick(position, datas[position])
                    return@OnLongClickListener true
                },
            )
        }
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay = itemView.findViewById<ConstraintLayout>(R.id.item_gallery_lay)
        val img = itemView.findViewById<ImageView>(R.id.item_gallery_img)
    }

    interface OnItemClickListener {
        fun onClick(
            index: Int,
            path: String,
        )

        fun onLongClick(
            index: Int,
            path: String,
        )
    }
}
