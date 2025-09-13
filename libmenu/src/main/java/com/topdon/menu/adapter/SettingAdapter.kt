package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.menu.R as MenuR
import com.topdon.menu.constant.MenuType
import com.topdon.menu.constant.SettingType

/**
 * Settings menu adapter. All options are independent and support multiple selection.
 *
 * - Single light: pseudo color bar, contrast, sharpness, warning, rotation, font, mirror
 * - Dual light: pseudo color bar, contrast, sharpness, warning, rotation, font
 * - Lite: pseudo color bar, contrast, warning, rotation, font, mirror
 * - TC007: pseudo color bar, contrast, sharpness, warning, font, mirror
 * - 2D editing: warning, font, watermark
 *
 * - TS001 observation: compass, rotation, mirror, contrast
 *
 * Created by LCG on 2024/11/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class SettingAdapter(menuType: MenuType = MenuType.SINGLE_LIGHT, isObserver: Boolean = false) : BaseMenuAdapter() {
    /**
     * Settings menu click event listener.
     * isSelected: Whether in selected state when clicked
     */
    var onSettingListener: ((settingType: SettingType, isSelected: Boolean) -> Unit)? = null

    /**
     * Several technical considerations here:
     * - For the core, 256x192 landscape size is the unrotated state with rotation angle 0.
     * For the APP, 192x256 portrait size (core rotation angle 270) is the unrotated state with rotation angle 0.
     * - For certain suppliers, rotation angle in core is counterclockwise, not the commonly understood clockwise rotation angle.
     *
     * Considering compatibility with old code, this property is used for **core rotation angle**
     */
    var rotateAngle: Int = 270
        set(value) {
            if (field != value) {
                field = value
                setSelected(SettingType.ROTATE, value != 270)
            }
        }

    /**
     * Settings specified option selected state. Do not call this method for rotation as it has 4 states.
     */
    fun setSelected(
        settingType: SettingType,
        isSelected: Boolean,
    ) {
        for (i in dataList.indices) {
            if (dataList[i].settingType == settingType) {
                dataList[i].isSelected = isSelected
                notifyItemChanged(i)
                break
            }
        }
    }

    private val dataList: ArrayList<Data> = ArrayList(7)

    init {
        if (isObserver) {
            dataList.add(Data(R.string.main_tab_second_compass, MenuR.drawable.selector_menu2_setting_8, SettingType.COMPASS))
            dataList.add(Data(R.string.thermal_rotate, MenuR.drawable.selector_menu2_setting_4, SettingType.ROTATE))
            dataList.add(Data(R.string.mirror, MenuR.drawable.selector_menu2_setting_5, SettingType.MIRROR))
            dataList.add(Data(R.string.thermal_contrast, MenuR.drawable.selector_menu2_setting_2, SettingType.CONTRAST))
        } else {
            if (menuType == MenuType.GALLERY_EDIT) { 
                dataList.add(Data(R.string.temp_alarm_alarm, MenuR.drawable.selector_menu2_setting_6, SettingType.ALARM))
                dataList.add(Data(R.string.menu_thermal_font, MenuR.drawable.selector_menu2_setting_7, SettingType.FONT))
                dataList.add(Data(R.string.app_watemarking, MenuR.drawable.selector_menu2_setting_9, SettingType.WATERMARK))
            } else {
                dataList.add(Data(R.string.thermal_pseudo, MenuR.drawable.selector_menu2_setting_1, SettingType.PSEUDO_BAR))
                dataList.add(Data(R.string.thermal_contrast, MenuR.drawable.selector_menu2_setting_2, SettingType.CONTRAST))
                if (menuType != MenuType.Lite) { // Lite has no detail (sharpness)
                    dataList.add(Data(R.string.thermal_sharpen, MenuR.drawable.selector_menu2_setting_3, SettingType.DETAIL))
                }
                dataList.add(Data(R.string.temp_alarm_alarm, MenuR.drawable.selector_menu2_setting_6, SettingType.ALARM))
                if (menuType != MenuType.TC007) { // TC007 has no rotation
                    dataList.add(Data(R.string.thermal_rotate, MenuR.drawable.selector_menu2_setting_4, SettingType.ROTATE))
                }
                dataList.add(Data(R.string.menu_thermal_font, MenuR.drawable.selector_menu2_setting_7, SettingType.FONT))
                if (menuType != MenuType.DOUBLE_LIGHT) { // TC001 Plus has no mirror
                    dataList.add(Data(R.string.mirror, MenuR.drawable.selector_menu2_setting_5, SettingType.MIRROR))
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val data: Data = dataList[position]
        holder.binding.ivIcon.setImageResource(data.drawableId)
        holder.binding.tvText.setText(data.stringId)
        if (data.settingType == SettingType.ROTATE) {
            when (rotateAngle) {
                0 -> holder.binding.ivIcon.setImageLevel(270)
                90 -> holder.binding.ivIcon.setImageLevel(180)
                180 -> holder.binding.ivIcon.setImageLevel(90)
                270 -> holder.binding.ivIcon.setImageLevel(0)
            }
        } else {
            holder.binding.ivIcon.isSelected = data.isSelected
        }
        holder.binding.tvText.isSelected = data.isSelected
        holder.binding.clRoot.setOnClickListener {
            // Warning, font, watermark are only considered highlight selected when effective. Maintain original code logic here.
            // Menu selection refresh left to upper-layer listener to handle. Consider changes later when time permits.
//            data.isSelected = !data.isSelected
//            holder.binding.ivIcon.isSelected = data.isSelected
//            holder.binding.tvText.isSelected = data.isSelected
            onSettingListener?.invoke(data.settingType, data.isSelected)
        }
    }

    override fun getItemCount(): Int = dataList.size

/**
 * Custom Data view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes val drawableId: Int,
        val settingType: SettingType,
        var isSelected: Boolean = false,
    )
}
