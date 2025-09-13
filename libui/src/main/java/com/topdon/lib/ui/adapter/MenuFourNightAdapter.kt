package com.topdon.lib.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.R
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.utils.Constants
import com.topdon.lib.core.utils.Constants.IR_OBSERVE_MODE
import com.topdon.lib.core.utils.Constants.IR_TC007_MODE
import com.topdon.lib.core.utils.Constants.IR_TCPLUS_MODE
import com.topdon.lib.core.utils.Constants.IR_TEMPERATURE_LITE
import com.topdon.lib.core.utils.Constants.IR_TEMPERATURE_MODE
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.lib.ui.config.CameraHelp
import com.topdon.lib.ui.listener.SingleClickListener
import com.topdon.lib.ui.R as UiR
import com.topdon.menu.R as MenuR

/**
 * Custom Menu four night view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
@Deprecated("旧的settingsmenu，已重构过了")
@SuppressLint("NotifyDataSetChanged")
/**
 * MenuFourNightAdapter provides data binding between data source and UI components.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MenuFourNightAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: ((index: Int, code: Int) -> Unit)? = null

    private var colorEnable = false // pseudo color条
    private var contrastEnable = false 
    private var ddeEnable = false 
    private var alarmEnable = false 
    private var textColorEnable = false 
    private var mirrorEnable = false 
    private var waterMarkEnable = false 
    private var compassEnable = false 
    private var rotateAngle = DeviceConfig.S_ROTATE_ANGLE 
    /**
     * Executes selectrotate functionality.
     */
    fun selectRotate(rotateAngle: Int) {
        this.rotateAngle = rotateAngle
        notifyDataSetChanged()
    }

    /**
     * Executes encolor functionality.
     */
    fun enColor(colorEnable: Boolean) {
        this.colorEnable = colorEnable
        notifyDataSetChanged()
    }

    /**
     * Executes encontrast functionality.
     */
    fun enContrast(param: Boolean) {
        this.contrastEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes endde functionality.
     */
    fun enDde(param: Boolean) {
        this.ddeEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes enalarm functionality.
     */
    fun enAlarm(param: Boolean) {
        this.alarmEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes entextcolor functionality.
     */
    fun enTextColor(param: Boolean) {
        this.textColorEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes enmirror functionality.
     */
    fun enMirror(param: Boolean) {
        this.mirrorEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes encompass functionality.
     */
    fun enCompass(param: Boolean) {
        this.compassEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes enwatermark functionality.
     */
    fun enWaterMark(param: Boolean) {
        this.waterMarkEnable = param
        notifyDataSetChanged()
    }

    /**
     * 不知道干嘛的
     * parameter [Constants.IR_TEMPERATURE_MODE] = 1 temperature measurementmode   pseudo color条、contrast、锐度、warning、旋转、font、镜像
     * parameter [Constants.IR_TCPLUS_MODE] = 5 dual lightdevice        pseudo color条、contrast、锐度、warning、旋转、font、
     * parameter [Constants.IR_TEMPERATURE_LITE] = 7 Litedevice  pseudo color条、contrast、warning、旋转、font、镜像
     * parameter [Constants.IR_TC007_MODE] = 6 TC007          pseudo color条、contrast、锐度、warning、font、镜像
     * else - 2D编辑menu                                  warning、font、watermark
     * parameter [Constants.IR_OBSERVE_MODE] = 2 observationmode  指南针、旋转、镜像、contrast
     */
    fun setShowMenuFour(modeType: Int) {
        fourBean.clear()
        when (modeType) {
            IR_TEMPERATURE_MODE -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_1,
                        context.getString(R.string.thermal_pseudo),
                        CameraHelp.TYPE_SET_PSEUDOCOLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_2,
                        context.getString(R.string.thermal_contrast),
                        CameraHelp.TYPE_SET_ParamLevelContrast,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_3,
                        context.getString(R.string.thermal_sharpen),
                        CameraHelp.TYPE_SET_ParamLevelDde,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_6,
                        context.getString(R.string.temp_alarm_alarm),
                        CameraHelp.TYPE_SET_ALARM,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_4,
                        context.getString(R.string.thermal_rotate),
                        CameraHelp.TYPE_SET_ROTATE,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_7,
                        context.getString(R.string.menu_thermal_font),
                        CameraHelp.TYPE_SET_COLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(MenuR.drawable.selector_menu2_setting_5, context.getString(R.string.mirror), CameraHelp.TYPE_SET_MIRROR),
                )
            }
            IR_TCPLUS_MODE -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_1,
                        context.getString(R.string.thermal_pseudo),
                        CameraHelp.TYPE_SET_PSEUDOCOLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_2,
                        context.getString(R.string.thermal_contrast),
                        CameraHelp.TYPE_SET_ParamLevelContrast,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_3,
                        context.getString(R.string.thermal_sharpen),
                        CameraHelp.TYPE_SET_ParamLevelDde,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_6,
                        context.getString(R.string.temp_alarm_alarm),
                        CameraHelp.TYPE_SET_ALARM,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_4,
                        context.getString(R.string.thermal_rotate),
                        CameraHelp.TYPE_SET_ROTATE,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_7,
                        context.getString(R.string.menu_thermal_font),
                        CameraHelp.TYPE_SET_COLOR,
                    ),
                )
            }
            IR_TEMPERATURE_LITE -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_1,
                        context.getString(R.string.thermal_pseudo),
                        CameraHelp.TYPE_SET_PSEUDOCOLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_2,
                        context.getString(R.string.thermal_contrast),
                        CameraHelp.TYPE_SET_ParamLevelContrast,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_6,
                        context.getString(R.string.temp_alarm_alarm),
                        CameraHelp.TYPE_SET_ALARM,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_4,
                        context.getString(R.string.thermal_rotate),
                        CameraHelp.TYPE_SET_ROTATE,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_7,
                        context.getString(R.string.menu_thermal_font),
                        CameraHelp.TYPE_SET_COLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(MenuR.drawable.selector_menu2_setting_5, context.getString(R.string.mirror), CameraHelp.TYPE_SET_MIRROR),
                )
            }
            IR_TC007_MODE -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_1,
                        context.getString(R.string.thermal_pseudo),
                        CameraHelp.TYPE_SET_PSEUDOCOLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_2,
                        context.getString(R.string.thermal_contrast),
                        CameraHelp.TYPE_SET_ParamLevelContrast,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_3,
                        context.getString(R.string.thermal_sharpen),
                        CameraHelp.TYPE_SET_ParamLevelDde,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_6,
                        context.getString(R.string.temp_alarm_alarm),
                        CameraHelp.TYPE_SET_ALARM,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_7,
                        context.getString(R.string.menu_thermal_font),
                        CameraHelp.TYPE_SET_COLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(MenuR.drawable.selector_menu2_setting_5, context.getString(R.string.mirror), CameraHelp.TYPE_SET_MIRROR),
                )
            }
            IR_OBSERVE_MODE -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_8,
                        context.getString(R.string.main_tab_second_compass),
                        CameraHelp.TYPE_SET_COMPASS,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_4,
                        context.getString(R.string.thermal_rotate),
                        CameraHelp.TYPE_SET_ROTATE,
                    ),
                )
                fourBean.add(
                    ColorBean(MenuR.drawable.selector_menu2_setting_5, context.getString(R.string.mirror), CameraHelp.TYPE_SET_MIRROR),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_2,
                        context.getString(R.string.thermal_contrast),
                        CameraHelp.TYPE_SET_ParamLevelContrast,
                    ),
                )
            }
            else -> {
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_6,
                        context.getString(R.string.temp_alarm_alarm),
                        CameraHelp.TYPE_SET_ALARM,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_7,
                        context.getString(R.string.menu_thermal_font),
                        CameraHelp.TYPE_SET_COLOR,
                    ),
                )
                fourBean.add(
                    ColorBean(
                        MenuR.drawable.selector_menu2_setting_9,
                        context.getString(R.string.app_watemarking),
                        CameraHelp.TYPE_SET_WATERMARK,
                    ),
                )
            }
        }
        notifyDataSetChanged()
    }

    private val fourBean =
        arrayListOf(
            ColorBean(MenuR.drawable.selector_menu2_setting_1, context.getString(R.string.thermal_pseudo), CameraHelp.TYPE_SET_PSEUDOCOLOR),
            ColorBean(
                MenuR.drawable.selector_menu2_setting_2,
                context.getString(R.string.thermal_contrast),
                CameraHelp.TYPE_SET_ParamLevelContrast,
            ),
            ColorBean(
                MenuR.drawable.selector_menu2_setting_3,
                context.getString(R.string.thermal_sharpen),
                CameraHelp.TYPE_SET_ParamLevelDde,
            ),
            ColorBean(MenuR.drawable.selector_menu2_setting_6, context.getString(R.string.temp_alarm_alarm), CameraHelp.TYPE_SET_ALARM),
            ColorBean(MenuR.drawable.selector_menu2_setting_4, context.getString(R.string.thermal_rotate), CameraHelp.TYPE_SET_ROTATE),
            ColorBean(MenuR.drawable.selector_menu2_setting_7, context.getString(R.string.menu_thermal_font), CameraHelp.TYPE_SET_COLOR),
            ColorBean(MenuR.drawable.selector_menu2_setting_5, context.getString(R.string.mirror), CameraHelp.TYPE_SET_MIRROR),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(UiR.layout.ui_item_menu_second_view, parent, false)
        compassEnable = SaveSettingUtil.isOpenCompass
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        @SuppressLint("RecyclerView") position: Int,
    ) {
        if (holder is ItemView) {
            
            updateViewWidth(holder.itemView, holder.img)
            val bean = fourBean[position]
            holder.name.text = bean.name
            if (bean.code == CameraHelp.TYPE_SET_ROTATE) {
                when (rotateAngle) {
                    0 -> {
                        holder.img.setImageResource(MenuR.drawable.svg_menu2_setting_4_rotate270)
                    }
                    90 -> {
                        holder.img.setImageResource(MenuR.drawable.svg_menu2_setting_4_rotate180)
                    }
                    180 -> {
                        holder.img.setImageResource(MenuR.drawable.svg_menu2_setting_4_rotate90)
                    }
                    270 -> {
                        holder.img.setImageResource(MenuR.drawable.svg_menu2_setting_4_rotate0)
                    }
                }
            } else {
                holder.img.setImageResource(bean.res)
            }
            holder.lay.setOnClickListener(
                object : SingleClickListener() {
                    override fun onSingleClick() {
                        listener?.invoke(position, bean.code)
                    }
                },
            )
            when (bean.code) {
                CameraHelp.TYPE_SET_ROTATE -> {
                    when (rotateAngle) {
                        0 -> {
                            holder.name.setTextColor(ContextCompat.getColor(context, UiR.color.white))
                        }
                        90 -> {
                            holder.name.setTextColor(ContextCompat.getColor(context, UiR.color.white))
                        }
                        180 -> {
                            holder.name.setTextColor(ContextCompat.getColor(context, UiR.color.white))
                        }
                        270 -> {
                            holder.name.setTextColor(ContextCompat.getColor(context, UiR.color.font_third_color))
                        }
                    }
                }
                CameraHelp.TYPE_SET_ParamLevelDde -> {
                    iconUI(ddeEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_ParamLevelContrast -> {
                    iconUI(contrastEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_PSEUDOCOLOR -> {
                    iconUI(colorEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_ALARM -> {
                    iconUI(alarmEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_COLOR -> {
                    iconUI(textColorEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_MIRROR -> {
                    iconUI(mirrorEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_COMPASS -> {
                    iconUI(compassEnable, holder.img, holder.name)
                }
                CameraHelp.TYPE_SET_WATERMARK -> {
                    iconUI(waterMarkEnable, holder.img, holder.name)
                }
            }
        }
    }

    
    private fun iconUI(
        isActive: Boolean,
        img: ImageView,
        nameText: TextView,
    ) {
        img.isSelected = isActive
        if (isActive) {
            nameText.setTextColor(ContextCompat.getColor(context, UiR.color.white))
        } else {
            nameText.setTextColor(ContextCompat.getColor(context, UiR.color.font_third_color))
        }
    }

    override fun getItemCount(): Int {
        return fourBean.size
    }

    /**
     * Updates the viewwidth with new data.
     */
    private fun updateViewWidth(
        itemView: View,
        itemMenu: ImageView,
    ) {
        if (fourBean.size <= 4) {
            itemView.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            itemView.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
//        if (fourBean.size <= 4) {  //item少于4个，每个占1/4
//            val canSeeCount = fourBean.size 
//            val with = (ScreenUtils.getScreenWidth() / canSeeCount)
//            itemView.layoutParams =
//                ViewGroup.LayoutParams(with, ViewGroup.LayoutParams.WRAP_CONTENT)
//            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
//            val layoutParams = itemMenu.layoutParams
//            layoutParams.width = imageSize
//            layoutParams.height = imageSize
//            itemMenu.layoutParams = layoutParams
//        } else {    //item大于4个，每屏4.5个item
//            val canSeeCount = 4.5 
//            val with = (ScreenUtils.getScreenWidth() / canSeeCount).toInt()
//            itemView.layoutParams =
//                ConstraintLayout.LayoutParams(with, ConstraintLayout.LayoutParams.WRAP_CONTENT)
//            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
//            val layoutParams = itemMenu.layoutParams
//            layoutParams.width = imageSize
//            layoutParams.height = imageSize
//            itemMenu.layoutParams = layoutParams
//        }
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay: View = itemView.findViewById(UiR.id.item_menu_tab_lay)
        val img: ImageView = itemView.findViewById(UiR.id.item_menu_tab_img)
        val name: TextView = itemView.findViewById(UiR.id.item_menu_tab_text)
    }
}
