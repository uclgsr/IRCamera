// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:42


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_adapter_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter' subtree
// Files: 14; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\CameraItemAdapter.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.ui.listener.SingleClickListener
import com.mpdc4gsr.libunified.ui.widget.CountDownView
import com.mpdc4gsr.module.thermalunified.R

class CameraItemAdapter(
    data: MutableList<CameraItemBean>? = null
) : BaseQuickAdapter<CameraItemBean, BaseViewHolder>(R.layout.item_camera, data) {
    var listener: ((index: Int, item: CameraItemBean) -> Unit)? = null
    override fun convert(holder: BaseViewHolder, item: CameraItemBean) {
        holder.setVisible(R.id.img, true)
        holder.setGone(R.id.count_down_view, true)
        holder?.itemView?.setOnClickListener(object : SingleClickListener() {
            override fun onSingleClick() {
                listener?.invoke(data.indexOf(item), item)
            }
        })
        when (item.type) {
            CameraItemBean.TYPE_DELAY -> {
                holder.setImageResource(R.id.img, R.drawable.svg_camera_delay_0)
                if (CameraItemBean.DELAY_TIME_0 == item.time) {
                    holder.setVisible(R.id.img, true)
                    holder.setGone(R.id.count_down_view, true)
                } else {
                    holder.setVisible(R.id.img, false)
                    holder.setGone(R.id.count_down_view, false)
                    val countDownView = holder.getView<CountDownView>(R.id.count_down_view)
                    holder.setGone(R.id.count_down_view, false)
                    countDownView.setCountdownTime(item.time)
                }
            }

            CameraItemBean.TYPE_ZDKM -> {
                holder.setImageResource(
                    R.id.img, if (item.isSel) R.drawable.svg_camera_auto_select_yes
                    else R.drawable.svg_camera_auto_select_not
                )
            }

            CameraItemBean.TYPE_SDKM -> {
                holder.setImageResource(
                    R.id.img, if (item.isSel) R.drawable.svg_camera_shutter_select_yes
                    else R.drawable.svg_camera_shutter_select_not
                )
            }

            CameraItemBean.TYPE_AUDIO -> {
                holder.setImageResource(
                    R.id.img, if (item.isSel) R.drawable.svg_camera_audio_select_yes
                    else R.drawable.svg_camera_audio_select_not
                )
            }

            else -> {
                holder.setImageResource(R.id.img, R.drawable.svg_camera_setting)
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\ConfigEmAdapter.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.utils.IRConfigData

class ConfigEmAdapter(val context: Context) : RecyclerView.Adapter<ConfigEmAdapter.ViewHolder>() {
    private val dataList: ArrayList<IRConfigData> = IRConfigData.irConfigData(context)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_ir_config_emissivity, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.tvEmName.text = dataList[position].name
        holder.tvEmNum.text = dataList[position].value
        holder.tvEmName.background = EmBgDrawable(false, position == dataList.size - 1)
        holder.tvEmNum.background = EmBgDrawable(true, position == dataList.size - 1)
    }

    override fun getItemCount(): Int = dataList.size
    class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val tvEmName: TextView = rootView.findViewById(R.id.tv_em_name)
        val tvEmNum: TextView = rootView.findViewById(R.id.tv_em_num)
    }

    private inner class EmBgDrawable(val drawRight: Boolean, val drawBottom: Boolean) : Drawable() {
        private val paint = Paint()

        init {
            paint.color = 0xff5b5961.toInt()
            paint.strokeWidth = 1f.dpToPx(context).coerceAtLeast(1f).toFloat()
        }

        override fun draw(canvas: Canvas) {
            canvas.drawLine(0f, 0f, 0f, bounds.bottom.toFloat(), paint)
            canvas.drawLine(0f, 0f, bounds.right.toFloat(), 0f, paint)
            if (drawRight) {
                canvas.drawLine(
                    bounds.right.toFloat(),
                    0f,
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    paint
                )
            }
            if (drawBottom) {
                canvas.drawLine(
                    0f,
                    bounds.bottom.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    paint
                )
            }
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        @Deprecated("This method is no longer used in graphics optimizations")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\GalleryAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\MeasureItemAdapter.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.bean.TargetColorBean
import com.mpdc4gsr.module.thermalunified.R

class MeasureItemAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: ((index: Int, code: Int) -> Unit)? = null
    private var type = 0
    private var selected = -1
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    private val secondBean =
        arrayListOf(
            TargetColorBean(R.drawable.ic_info_svg, "1.8m", ObserveBean.TYPE_MEASURE_PERSON),
            TargetColorBean(R.drawable.ic_info_svg, "1.0m", ObserveBean.TYPE_MEASURE_SHEEP),
            TargetColorBean(R.drawable.ic_info_svg, "0.5m", ObserveBean.TYPE_MEASURE_DOG),
            TargetColorBean(R.drawable.ic_info_svg, "0.2m", ObserveBean.TYPE_MEASURE_BIRD),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.itme_target_mode, parent, false)
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            val bean = secondBean[position]
            holder.img.setImageResource(bean.res)
            holder.lay.setOnClickListener {
                listener?.invoke(position, bean.code)
                selected(bean.code)
            }
            holder.img.isSelected = bean.code == selected
            holder.name.visibility = View.VISIBLE
            holder.name.text = bean.name
            holder.name.isSelected = bean.code == selected
            holder.name.setTextColor(
                ContextCompat.getColor(context, R.color.white),
            )
        }
    }

    override fun getItemCount(): Int {
        return secondBean.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay: View = itemView.findViewById(R.id.item_menu_tab_lay)
        val img: ImageView = itemView.findViewById(R.id.item_menu_tab_img)
        val name: TextView = itemView.findViewById(R.id.item_menu_tab_text)
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\MenuRecyclerView.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MenuRecyclerView : RecyclerView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    fun initType(type: Int) {
        val span =
            when (type) {
                1 -> 2
                2 -> 6
                4 -> 4
                else -> 4
            }
        if (span == 2) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val horizontalPadding = (screenWidth / 3.5f).toInt()
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
        } else {
            setPadding(0, 0, 0, 0)
        }
        layoutManager =
            if (type == 3) {
                LinearLayoutManager(context, HORIZONTAL, false)
            } else {
                GridLayoutManager(context, span)
            }
        val menuTabAdapter = adapter
        if (menuTabAdapter is MenuTabAdapter) {
            (menuTabAdapter as MenuTabAdapter).initType(type)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\MenuTabAdapter.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.libunified.R as LibUiR

class MenuTabAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: OnItemClickListener? = null
    private var type = 0
    private var datas = arrayListOf<Int>()
    private var dataStrList = arrayListOf<String>()
    private var selected = -1

    companion object {
        private const val TYPE_ITEM = 300
        private const val TYPE_ITEM_MORE = 301
    }

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    private val firstMenus =
        arrayListOf<Int>(
            LibUiR.drawable.ic_menu_thermal7001_svg,
            LibUiR.drawable.ic_menu_thermal7002_svg,
        )
    private val secondMenus =
        arrayListOf<Int>(
            LibUiR.drawable.ic_menu_thermal6001,
            LibUiR.drawable.ic_menu_thermal6003,
            LibUiR.drawable.ic_menu_thermal7001,
            LibUiR.drawable.ic_menu_thermal7002,
            LibUiR.drawable.ic_menu_thermal7003,
            LibUiR.drawable.ic_menu_thermal7004,
        )
    private val secondMenusStr =
        arrayListOf(
            "[ph]",
            "[ph]",
            "[ph]",
            "[ph][ph]",
            "[ph][ph]",
            "[ph][ph]",
        )
    private val fourthMenusStr =
        arrayListOf(
            "[ph][ph]",
            "Enhance",
            "[ph][ph][ph]",
            "[ph][ph]",
        )
    private val thirdMenus =
        arrayListOf<Int>(
            LibUiR.drawable.ic_menu_thermal5003,
            LibUiR.drawable.ic_menu_thermal6001,
            LibUiR.drawable.ic_menu_thermal6002,
            LibUiR.drawable.ic_menu_thermal6003,
            LibUiR.drawable.ic_menu_thermal7001,
            LibUiR.drawable.ic_menu_thermal7002,
            LibUiR.drawable.ic_menu_thermal7003,
            LibUiR.drawable.ic_menu_thermal7004,
            LibUiR.drawable.ic_menu_thermal5003_selected_svg,
            LibUiR.drawable.ic_menu_thermal6003_svg,
        )
    private val fourthMenus =
        arrayListOf<Int>(
            LibUiR.drawable.ic_menu_thermal7001_svg,
            LibUiR.drawable.ic_menu_thermal7002_svg,
            LibUiR.drawable.ic_menu_thermal7003_svg,
            LibUiR.drawable.ic_menu_thermal7004_svg,
        )

    fun initType(type: Int) {
        this.type = type
        datas =
            when (type) {
                1 -> firstMenus
                2 -> secondMenus
                3 -> thirdMenus
                4 -> fourthMenus
                else -> thirdMenus
            }
        dataStrList =
            when (type) {
                2 -> secondMenusStr
                4 -> fourthMenusStr
                else -> secondMenusStr
            }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_menu_tab_view, parent, false)
            ItemView(view)
        } else {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_menu_tab_more_view, parent, false)
            ItemMoreView(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is BaseItemView) {
            holder.img.setImageResource(datas[position])
            holder.lay.setOnClickListener {
                val index = type * 1000 + position + 1
                listener?.onClick(index)
                selected(position)
            }
            holder.img.isSelected = position == selected
            if (holder is ItemView) {
                holder.name.text = dataStrList[position]
                holder.name.isSelected = position == selected
                holder.name.setTextColor(
                    if (position == selected) {
                        ContextCompat.getColor(context, com.mpdc4gsr.libunified.R.color.white)
                    } else {
                        ContextCompat.getColor(
                            context,
                            com.mpdc4gsr.libunified.R.color.font_third_color
                        )
                    },
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (type == 3) {
            TYPE_ITEM_MORE
        } else {
            TYPE_ITEM
        }
    }

    open class BaseItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var lay: View
        lateinit var img: ImageView
    }

    inner class ItemView(itemView: View) : BaseItemView(itemView) {
        var name: TextView

        init {
            lay =
                itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.item_menu_tab_lay)
            img = itemView.findViewById<ImageView>(R.id.item_menu_tab_img)
            name = itemView.findViewById<TextView>(R.id.item_menu_tab_text)
        }
    }

    inner class ItemMoreView(itemView: View) : BaseItemView(itemView) {
        init {
            lay =
                itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.item_menu_tab_more_lay)
            img = itemView.findViewById<ImageView>(R.id.item_menu_tab_more_img)
        }
    }

    interface OnItemClickListener {
        fun onClick(index: Int)
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\MonitorLogAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\PDFAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\ReportPreviewAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\ReportPreviewAlbumAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\ReportPreviewFloorAdapter.kt =====

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
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return ItemView(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_report_floor_child, parent, false),
        )
    }

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

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\SettingCheckAdapter.kt =====

// kotlin
package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.libunified.R as LibR

class SettingCheckAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var datas = arrayOf("1s", "5s", "10s", "30s", "1min", "5min")
    private var dataTimes = arrayOf(1, 5, 10, 30, 60, 300)
    var listener: OnItemClickListener? = null
    var selectTime = 0
    fun setCheck(index: Int) {
        this.selectTime = index
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_setting_check, parent, false)
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            holder.btn.text = datas[position]
            if (position == selectTime) {
                holder.btn.setBackgroundResource(LibR.drawable.ic_menu_thermal7001_svg)
                holder.btn.setTextColor(
                    ContextCompat.getColor(
                        context,
                        LibR.color.white
                    )
                )
            } else {
                holder.btn.setBackgroundResource(LibR.drawable.ic_menu_thermal7002_svg)
                holder.btn.setTextColor(
                    ContextCompat.getColor(
                        context,
                        LibR.color.font_third_color
                    )
                )
            }
            holder.btn.setOnClickListener {
                Log.w("123", ": ${datas[position]}")
                listener?.onClick(position, dataTimes[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btn: Button = itemView.findViewById(R.id.item_setting_check_btn)
    }

    interface OnItemClickListener {
        fun onClick(
            index: Int,
            time: Int,
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\SettingTimeAdapter.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\adapter\TargetItemAdapter.kt =====

package com.mpdc4gsr.module.thermalunified.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.bean.TargetColorBean
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.libunified.R as LibR

class TargetItemAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: ((index: Int, code: Int) -> Unit)? = null
    private var type = 0
    private var selected = -1
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    fun getSelected(): Int {
        return selected
    }

    private val secondBean =
        arrayListOf(
            TargetColorBean(
                LibR.drawable.ic_menu_thermal6002,
                "",
                ObserveBean.TYPE_TARGET_HORIZONTAL
            ),
            TargetColorBean(
                LibR.drawable.ic_menu_thermal6001,
                "",
                ObserveBean.TYPE_TARGET_VERTICAL
            ),
            TargetColorBean(LibR.drawable.ic_menu_thermal6003, "", ObserveBean.TYPE_TARGET_CIRCLE),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.itme_target_mode, parent, false)
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            val bean = secondBean[position]
            holder.img.setImageResource(bean.res)
            holder.lay.setOnClickListener {
                listener?.invoke(position, bean.code)
                selected(bean.code)
            }
            holder.img.isSelected = bean.code == selected
            holder.name.text = bean.name
            holder.name.isSelected = bean.code == selected
            holder.name.setTextColor(
                if (position == selected) {
                    ContextCompat.getColor(context, R.color.white)
                } else {
                    ContextCompat.getColor(context, R.color.font_third_color)
                },
            )
        }
    }

    override fun getItemCount(): Int {
        return secondBean.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay: View = itemView.findViewById(R.id.item_menu_tab_lay)
        val img: ImageView = itemView.findViewById(R.id.item_menu_tab_img)
        val name: TextView = itemView.findViewById(R.id.item_menu_tab_text)
    }
}