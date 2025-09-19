package com.topdon.module.thermal.ir.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.bean.GalleryTitle
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.tools.TimeTool
import com.topdon.module.thermal.ir.R

@SuppressLint("NotifyDataSetChanged")
class GalleryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEAD = 0
        private const val TYPE_DATA = 1
    }

    val dataList: ArrayList<GalleryBean> = ArrayList()

    val selectList: ArrayList<Int> = ArrayList()

    var isTS004Remote = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var isEditMode = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    selectList.clear()
                    selectCallback?.invoke(selectList)
                }
                notifyDataSetChanged()
            }
        }

    var onLongEditListener: (() -> Unit)? = null

    
    var selectCallback: ((data: ArrayList<Int>) -> Unit)? = null

    var itemClickCallback: ((position: Int) -> Unit)? = null

    fun refreshList(newList: List<GalleryBean>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    fun buildSelectList(): ArrayList<GalleryBean> {
        val resultList: ArrayList<GalleryBean> = ArrayList()
        selectList.forEach {
            resultList.add(dataList[it])
        }
        return resultList
    }

    fun selectAll() {
        var dataCount = 0
        dataList.forEach {
            if (it !is GalleryTitle) {
                dataCount++
            }
        }
        if (selectList.size >= dataCount) {
            selectList.clear()
        } else {
            selectList.clear()
            for (i in 0 until dataList.size) {
                if (dataList[i] !is GalleryTitle) {
                    selectList.add(i)
                }
            }
        }
        selectCallback?.invoke(selectList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataList[position] is GalleryTitle) {
            TYPE_HEAD
        } else {
            TYPE_DATA
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEAD) {
            ItemHeadView(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_gallery_head_lay, parent, false)
            )
        } else {
            ItemView(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_gallery_lay, parent, false)
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val data = dataList[position]
        if (holder is ItemView) {
            GlideLoader.load(holder.img, data.thumb)
            if (data.name.uppercase().endsWith(".MP4")) {
                holder.info.text = TimeTool.showVideoTime(data.duration)
                holder.ivVideoTime.isVisible = true
            } else {
                holder.info.text = ""
                holder.ivVideoTime.isVisible = false
            }

            holder.ivHasDownload.isVisible = isTS004Remote && data.hasDownload

            holder.ivCheck.isVisible = isEditMode
            holder.ivCheck.isSelected = selectList.contains(position)

            holder.img.setOnClickListener {
                if (isEditMode) {
                    if (selectList.contains(position)) {
                        selectList.remove(position)
                    } else {
                        selectList.add(position)
                    }
                    selectCallback?.invoke(selectList)

                    holder.ivCheck.isSelected = selectList.contains(position)
                } else {
                    itemClickCallback?.invoke(position)
                }
            }
            holder.img.setOnLongClickListener {
                if (!isEditMode) {
                    selectList.add(position)
                    selectCallback?.invoke(selectList)
                    holder.ivCheck.isVisible = true
                    holder.ivCheck.isSelected = true
                    isEditMode = true
                    onLongEditListener?.invoke()
                }
                return@setOnLongClickListener true
            }
        } else if (holder is ItemHeadView) {
            holder.name.text = TimeTool.showDateType(data.timeMillis, 4)
            holder.name.setTextColor(0x80ffffff.toInt())
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ItemHeadView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_gallery_head_text)
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.item_gallery_img)
        val info: TextView = itemView.findViewById(R.id.item_gallery_text)
        val ivVideoTime: ImageView = itemView.findViewById(R.id.iv_video_time)
        val ivHasDownload: ImageView = itemView.findViewById(R.id.iv_has_download)
        val ivCheck: ImageView = itemView.findViewById(R.id.iv_check)
    }
}
