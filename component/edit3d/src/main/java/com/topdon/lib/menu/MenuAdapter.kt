package com.topdon.lib.menu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.core.R as RCore

/**
 * 二级菜单 RecyclerView 所用 Adapter.
 */
class MenuAdapter(val context: Context, val type: Type) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {
    /**
     * 当前选中 item index，用于实现选中效果.
     */
    private var selectIndex = 0

    /**
     * item 点击事件监听.
     */
    var onItemClickListener: ((position: Int) -> Unit)? = null

    init {
        if (type == Type.VISUAL){
//            selectIndex = 1
        }else if (type == Type.MARK){
            selectIndex = -1
        } else if (type == Type.MODE) {
            selectIndex = 2
        }
    }


    companion object {
        private val VISUAL_ARRAY = arrayOf(
            ItemData(RCore.string.menu_3d_visual_3d, R.drawable.selector_menu2_visual_3d),
            ItemData(RCore.string.menu_3d_visual_top, R.drawable.selector_menu2_visual_top),
            ItemData(RCore.string.menu_3d_visual_left, R.drawable.selector_menu2_visual_left),
            ItemData(RCore.string.menu_3d_visual_right, R.drawable.selector_menu2_visual_right),
            ItemData(RCore.string.menu_3d_visual_center, R.drawable.selector_menu2_visual_center),
        )
        private val MARK_ARRAY = arrayOf(
            ItemData(RCore.string.pseudo_custom_title, R.drawable.selector_menu2_mark_custom),
            ItemData(RCore.string.thermal_high_temperature, R.drawable.selector_menu2_mark_max_temp),
            ItemData(RCore.string.app_Low_temperature, R.drawable.selector_menu2_mark_min_temp),
//            ItemData(RCore.string.menu_3d_range_temp, R.drawable.selector_menu2_mark_range_temp),
            ItemData(RCore.string.report_delete, R.drawable.selector_menu2_mark_del),
        )
        private val PSEUDO_ARRAY = arrayOf(
            ItemData(RCore.string.color_p3),
            ItemData(RCore.string.pseudo_type_black_red),
            ItemData(RCore.string.pseudo_type_nature),
            ItemData(RCore.string.pseudo_type_magma),
            ItemData(RCore.string.color_p8_ios),
        )
        private val MODE_ARRAY = arrayOf(
            ItemData(RCore.string.thermal_point, R.drawable.selector_menu2_mode_point),
            ItemData(RCore.string.thermal_line, R.drawable.selector_menu2_mode_line),
            ItemData(RCore.string.thermal_rect, R.drawable.selector_menu2_mode_rect),
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_menu_3d, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
         if(type == Type.PSEUDO){
            holder.tvMenu.visibility = View.GONE
            holder.ivPseudo.visibility = View.VISIBLE
        }else if(type == Type.MODE){
            holder.tvMenu.visibility = View.VISIBLE
            holder.ivPseudo.visibility = View.GONE
        }
        val itemArray = when (type) {
            Type.VISUAL -> VISUAL_ARRAY
            Type.MARK -> MARK_ARRAY
            Type.PSEUDO -> PSEUDO_ARRAY
            Type.MODE -> MODE_ARRAY
        }
        holder.tvMenu.isSelected = position == selectIndex
        holder.ivMenu.isSelected = position == selectIndex
        holder.tvMenu.setText(itemArray[position].textResId)
        holder.ivMenu.setImageResource(itemArray[position].imgResId)

        if (type == Type.PSEUDO) {
            val drawable = when (position) {
                0 -> buildRectDrawable(selectIndex == 0, 0xff0000ff.toInt(), 0xffff0000.toInt(), 0xffffff00.toInt())
                1 -> buildRectDrawable(selectIndex == 1, 0xff000000.toInt(), 0xffffffff.toInt(), 0xffff0000.toInt())
                2 -> buildRectDrawable(selectIndex == 2, 0xff0000ff.toInt(), 0xff00ff00.toInt(), 0xffffff00.toInt(), 0xffff0000.toInt())
                3 -> buildRectDrawable(selectIndex == 3, 0xff000000.toInt(), 0xffff0000.toInt())
                else  -> buildRectDrawable(selectIndex == position, 0xff0000ff.toInt(), 0xffffff00.toInt())
            }
            holder.viewPseudo.background = drawable
            holder.ivPseudo.visibility = if(position == selectIndex) View.VISIBLE else View.GONE
        }

        //单独设置删除文本颜色
        if (type == Type.MARK && position == MARK_ARRAY.size - 1) {
            holder.tvMenu.setTextColor(0x66ffffff)
        }
    }

    private fun buildRectDrawable(isSelect: Boolean, vararg color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = SizeUtils.dp2px(10f).toFloat()
        drawable.colors = color
        drawable.orientation = GradientDrawable.Orientation.BOTTOM_TOP

        if (isSelect) {
            val strokeWidth = SizeUtils.dp2px(2f)
            val width = SizeUtils.dp2px(50f)
            val height = SizeUtils.dp2px(60f )
            val selectDrawable = GradientDrawable()
            selectDrawable.shape = GradientDrawable.RECTANGLE
            selectDrawable.setSize(width, height)
            selectDrawable.cornerRadius = SizeUtils.dp2px(10f).toFloat()
            selectDrawable.setColor(0xffffffff.toInt())

            val layerDrawable = LayerDrawable(arrayOf(selectDrawable, drawable))
            layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
            return layerDrawable
        }
        return drawable
    }

    override fun getItemCount() = when (type) {
        Type.VISUAL -> VISUAL_ARRAY.size
        Type.MARK -> MARK_ARRAY.size
        Type.PSEUDO -> PSEUDO_ARRAY.size
        Type.MODE -> MODE_ARRAY.size
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        
        // View references - migrated from synthetic views
        val tvMenu: TextView = rootView.findViewById(R.id.tv_menu)
        val ivMenu: ImageView = rootView.findViewById(R.id.iv_menu)
        val ivPseudo: ImageView = rootView.findViewById(R.id.iv_pseudo)
        val viewPseudo: View = rootView.findViewById(R.id.view_pseudo)
        val clRoot: ConstraintLayout = rootView.findViewById(R.id.cl_root)
        
        init {
            val canSeeCount = itemCount.toFloat() //一屏可见的 item 数量，目前都是全都显示完
            if (ScreenUtil.isPortrait(context)) {
                val with = (ScreenUtil.getScreenWidth(context)/ canSeeCount).toInt()
                rootView.layoutParams = ViewGroup.LayoutParams(with, ViewGroup.LayoutParams.WRAP_CONTENT)

                val imageSize = (ScreenUtil.getScreenWidth(context) * 62 / 375f).toInt()
                val layoutParams = ivMenu.layoutParams
                layoutParams.width = imageSize
                layoutParams.height = imageSize
                ivMenu.layoutParams = layoutParams
            } else {
                val constraintSet = ConstraintSet()
                constraintSet.clone(clRoot)
                constraintSet.setDimensionRatio(R.id.view_bg, (136 * canSeeCount / 436).toString())
                constraintSet.applyTo(clRoot)
            }

            rootView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectIndex = position
                    notifyDataSetChanged()
                    onItemClickListener?.invoke(position)
                }
            }
        }
    }

    data class ItemData(@StringRes val textResId: Int, @DrawableRes val imgResId: Int = 0)

    enum class Type {
        /**
         * 视觉.
         */
        VISUAL,

        /**
         * 标定.
         */
        MARK,

        /**
         * 伪彩.
         */
        PSEUDO,

        /**
         * 模式.
         */
        MODE,
    }
}