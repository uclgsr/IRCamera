package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R // Import R from libapp for strings
import com.topdon.menu.R as MenuR // Import R from libmenu for drawables
import com.topdon.menu.constant.MenuType
import com.topdon.menu.constant.TwoLightType

/**
 * temperature measurementmode-menu3-dual light menuAdapter used for.
 *
 * - Single light: Picture-in-picture, Fusion level
 * - Lite: Picture-in-picture, Fusion level
 * - Dual light: Dual light 1, Dual light 2, Infrared, Visible light, registration, picture-in-picture, fusion level
 * - TC007: dual light, infrared, visible light, registration, picture-in-picture, fusion level
 * - 2D editing: No such menu
 *
 * Single light, Lite: picture-in-picture, fusion level independently selectable
 *
 * Dual light: dual light1, dual light2, infrared, visible light mutually exclusive; registration, picture-in-picture, fusion level independently selectable
 *
 * TC007: Dual light, Infrared, Visible light, Picture-in-picture mutually exclusive; registration, fusion level independently selectable
 *
 * Created by LCG on 2024/11/20.
 */
@SuppressLint("NotifyDataSetChanged")
internal class TwoLightAdapter(private val menuType: MenuType) : BaseMenuAdapter() {
    /**
     * Dual light menu click event listener.
     */
    var onTwoLightListener: ((twoLightType: TwoLightType, isSelected: Boolean) -> Unit)? = null

    /**
     * Currently selected dual light type
     * - Single light: Should not use this property
     * - Lite: Should not use this property
     * - Dual light: Dual light 1, Dual light 2, Infrared, Visible light
     * - TC007: Dual light, Infrared, Visible light, Picture-in-picture
     */
    var twoLightType: TwoLightType
        get() {
            for (data in dataList) {
                if (data.isSingle && data.isSelected) {
                    return data.twoLightType
                }
            }
            return TwoLightType.TWO_LIGHT_1
        }
        set(value) {
            if (value == TwoLightType.CORRECT || value == TwoLightType.BLEND_EXTENT) {
                return
            }
            if (value == TwoLightType.P_IN_P && menuType != MenuType.TC007) {
                return
            }
            for (data in dataList) {
                if (data.isSingle) {
                    if (menuType == MenuType.TC007 && value == TwoLightType.TWO_LIGHT_1) {
                        // In TC007, both dual light1 and dual light2 are treated as dual light
                        data.isSelected = data.twoLightType == TwoLightType.TWO_LIGHT_2
                    } else {
                        data.isSelected = data.twoLightType == value
                    }
                }
            }
            notifyDataSetChanged()
        }

    /**
     * Settings multi-select state
     * - Single light: Picture-in-picture, Fusion level
     * - Lite: Picture-in-picture, Fusion level
     * - Dual light: Registration, Picture-in-picture, Fusion level
     * - TC007: Registration, Fusion level
     */
    fun setSelected(
        twoLightType: TwoLightType,
        isSelected: Boolean,
    ) {
        if (twoLightType == TwoLightType.TWO_LIGHT_1 || twoLightType == TwoLightType.TWO_LIGHT_2) { // dual light1, dual light2
            return
        }
        if (twoLightType == TwoLightType.IR || twoLightType == TwoLightType.LIGHT) { // single infrared, visible light
            return
        }
        if (menuType == MenuType.TC007 && twoLightType == TwoLightType.P_IN_P) { // picture-in-picture in TC007
            return
        }
        for (data in dataList) {
            if (data.twoLightType == twoLightType) {
                data.isSelected = isSelected
            }
        }
        notifyDataSetChanged()
    }

    private val dataList: ArrayList<Data> = ArrayList(7)

    init {
        if (menuType == MenuType.DOUBLE_LIGHT || menuType == MenuType.TC007) {
            if (menuType == MenuType.DOUBLE_LIGHT) {
                dataList.add(Data(R.string.dual_menu_1, MenuR.drawable.selector_menu2_two_light_1, TwoLightType.TWO_LIGHT_1, true))
                dataList.add(Data(R.string.dual_menu_2, MenuR.drawable.selector_menu2_two_light_2, TwoLightType.TWO_LIGHT_2, true))
            } else {
                dataList.add(Data(R.string.menu_thermal_merge, MenuR.drawable.selector_menu2_two_light_2, TwoLightType.TWO_LIGHT_2, true))
            }
            dataList.add(Data(R.string.menu_thermal_imaging, MenuR.drawable.selector_menu2_two_light_3, TwoLightType.IR, true))
            dataList.add(Data(R.string.menu_thermal_visible_light, MenuR.drawable.selector_menu2_two_light_4, TwoLightType.LIGHT, true))
            dataList.add(Data(R.string.menu_thermal_registration, MenuR.drawable.selector_menu2_two_light_5, TwoLightType.CORRECT, false))
        }
        dataList.add(
            Data(
                R.string.thermal_picture_in_camera,
                MenuR.drawable.selector_menu2_two_light_6,
                TwoLightType.P_IN_P,
                menuType == MenuType.TC007,
            ),
        )
        dataList.add(Data(R.string.ios_double_light, MenuR.drawable.selector_menu2_two_light_7, TwoLightType.BLEND_EXTENT, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val data: Data = dataList[position]
        holder.binding.ivIcon.setImageResource(data.drawableId)
        holder.binding.tvText.setText(data.stringId)
        holder.binding.ivIcon.isSelected = data.isSelected
        holder.binding.tvText.isSelected = data.isSelected
        holder.binding.clRoot.setOnClickListener {
            if (data.isSingle) { // single selection
                if (!data.isSelected) { // repeated clicks ignored in single selection mode
                    twoLightType = data.twoLightType
                    onTwoLightListener?.invoke(data.twoLightType, true)
                }
            } else { 
                data.isSelected = !data.isSelected
                holder.binding.ivIcon.isSelected = data.isSelected
                holder.binding.tvText.isSelected = data.isSelected
                onTwoLightListener?.invoke(data.twoLightType, data.isSelected)
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    /**
     * @param isSingle whether single selection. Currently only 1 group of mutually exclusive single selection, so Boolean is sufficient
     * @param isSelected whether currently selected
     */
/**
 * Custom Data view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes val drawableId: Int,
        val twoLightType: TwoLightType,
        val isSingle: Boolean,
        var isSelected: Boolean = false,
    )
}
