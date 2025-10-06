package com.mpdc4gsr.module.thermalunified.adapter

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.libunified.app.tools.CoilLoader
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData

class PDFAdapter : BaseQuickAdapter<ReportData.Records?, BaseViewHolder>, LoadMoreModule {
    constructor(layoutResId: Int) : super(layoutResId) {}
    constructor(layoutResId: Int, data: MutableList<ReportData.Records?>?) : super(
        layoutResId,
        data
    ) {
    }

    var delListener: ((item: ReportData.Records, position: Int) -> Unit)? = null
    var jumpDetailListener: ((item: ReportData.Records, position: Int) -> Unit)? = null
    override fun convert(
        holder: BaseViewHolder,
        item: ReportData.Records?,
    ) {
        item?.let {
            if (it.isShowTitleTime) {
                holder.setVisible(R.id.item_message_read, true)
                holder.setGone(R.id.tv_time, false)
                holder.setText(R.id.tv_time, it.uploadTime?.split(" ")?.get(0))
            } else {
                holder.setVisible(R.id.item_message_read, false)
                holder.setGone(R.id.tv_time, true)
            }
            item?.reportContent?.infrared_data?.get(0)?.picture_url?.let { url ->
                CoilLoader.loadP(holder.getView(R.id.img_content), url)
            }
            holder.setText(
                R.id.item_pdf_title,
                item?.reportContent?.report_info?.report_name + ""
            )
            holder.setText(R.id.item_pdf_content, it.uploadTime + "")
            addChildClickViewIds(R.id.item_message_lay)
            val view = holder.itemView.findViewById<View>(R.id.tv_del)
            holder.itemView.findViewById<View>(R.id.item_message_lay).setOnClickListener {
                jumpDetailListener?.invoke(item, data.indexOf(item))
            }
            view.setOnClickListener {
                delListener?.invoke(item, data.indexOf(item))
            }
        }
    }

    override fun setNewInstance(list: MutableList<ReportData.Records?>?) {
        list?.let {
            updateTime(it)
        }
        super.setNewInstance(list)
    }

    override fun addData(newData: Collection<ReportData.Records?>) {
        this.data.addAll(newData)
        updateTime(this.data)
        notifyItemRangeInserted(this.data.size - newData.size + headerLayoutCount, newData.size)
        compatibilityDataSizeChanged(newData.size)
    }

    private fun updateTime(dataList: MutableList<ReportData.Records?>) {
        for (i in 0 until dataList.size) {
            dataList[i]?.isShowTitleTime = false
            if (i == 0) {
                dataList[i]?.isShowTitleTime = true
            } else {
                val lastTimes = dataList[i - 1]?.uploadTime?.split(" ")
                val times = dataList[i]?.uploadTime?.split(" ")
                if (lastTimes?.size!! > 1 && times?.size!! > 1) {
                    if (times[0] != lastTimes[0]) {
                        dataList[i]?.isShowTitleTime = true
                    }
                }
            }
        }
    }
}
