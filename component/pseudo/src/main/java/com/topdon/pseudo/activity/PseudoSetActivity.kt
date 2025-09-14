package com.topdon.pseudo.activity

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.view.ColorSelectView
import com.topdon.pseudo.R
import com.topdon.pseudo.bean.CustomPseudoBean
import com.topdon.pseudo.constant.*
import com.topdon.pseudo.view.PseudoPickView
import java.lang.NumberFormatException
import java.math.BigDecimal
import java.math.RoundingMode
import com.topdon.lib.core.R as RCore // For drawable resources from libapp
import com.topdon.lib.ui.R as RUi // For string resources from libui


class PseudoSetActivity : BaseActivity(), View.OnClickListener {

    private lateinit var customPseudoBean: CustomPseudoBean

    // View references - migrated from synthetic views
    private lateinit var etMaxTemp: EditText
    private lateinit var etMinTemp: EditText
    private lateinit var tvMaxTempUnit: TextView
    private lateinit var tvMinTempUnit: TextView
    private lateinit var pseudoPickView: PseudoPickView
    private lateinit var colorSelectView: ColorSelectView
    private lateinit var ivCustomAdd: ImageView
    private lateinit var ivCustomDel: ImageView
    private lateinit var viewCustomColor1: View
    private lateinit var viewCustomColor2: View
    private lateinit var viewCustomColor3: View
    private lateinit var viewCustomColor4: View
    private lateinit var viewCustomColor5: View
    private lateinit var viewCustomColor6: View
    private lateinit var viewRecommendColor1: View
    private lateinit var viewRecommendColor2: View
    private lateinit var viewRecommendColor3: View
    private lateinit var viewRecommendColor4: View
    private lateinit var viewRecommendColor5: View
    private lateinit var viewRecommendBgColor1: View
    private lateinit var viewRecommendBgColor2: View
    private lateinit var viewRecommendBgColor3: View
    private lateinit var viewRecommendBgColor4: View
    private lateinit var viewRecommendBgColor5: View
    private lateinit var tvRecommendColor1: TextView
    private lateinit var tvRecommendColor2: TextView
    private lateinit var tvRecommendColor3: TextView
    private lateinit var tvRecommendColor4: TextView
    private lateinit var tvRecommendColor5: TextView
    private lateinit var clDynamic: ConstraintLayout
    private lateinit var clCustom: ConstraintLayout
    private lateinit var clCustomContent: ConstraintLayout
    private lateinit var clColorCustom: ConstraintLayout
    private lateinit var clColorRecommend: ConstraintLayout
    private lateinit var clOverGrey: ConstraintLayout
    private lateinit var clOverColor: ConstraintLayout
    private lateinit var tvColorCustom: TextView
    private lateinit var tvColorRecommend: TextView
    private lateinit var tvConfirm: TextView
    private lateinit var tvCancel: TextView
    private lateinit var ivDynamic: ImageView
    private lateinit var ivCustom: ImageView
    private lateinit var tvDynamicTitle: TextView
    private lateinit var tvCustomTitle: TextView
    private lateinit var ivOverGreySelect: ImageView
    private lateinit var ivOverColorSelect: ImageView
    private lateinit var tvOverGrey: TextView
    private lateinit var tvOverColor: TextView

    override fun initContentView() = R.layout.activity_pseudo_set

    override fun initView() {
    // Initialize views - migrated from synthetic views
    etMaxTemp = findViewById(R.id.et_max_temp)
    etMinTemp = findViewById(R.id.et_min_temp)
    tvMaxTempUnit = findViewById<TextView>(R.id.tv_max_temp_unit)
    tvMinTempUnit = findViewById<TextView>(R.id.tv_min_temp_unit)
    pseudoPickView = findViewById<PseudoPickView>(R.id.pseudo_pick_view)
    colorSelectView = findViewById<ColorSelectView>(R.id.color_select_view)
    ivCustomAdd = findViewById<ImageView>(R.id.iv_custom_add)
    ivCustomDel = findViewById<ImageView>(R.id.iv_custom_del)
    viewCustomColor1 = findViewById(R.id.view_custom_color1)
    viewCustomColor2 = findViewById(R.id.view_custom_color2)
    viewCustomColor3 = findViewById(R.id.view_custom_color3)
    viewCustomColor4 = findViewById(R.id.view_custom_color4)
    viewCustomColor5 = findViewById(R.id.view_custom_color5)
    viewCustomColor6 = findViewById(R.id.view_custom_color6)
    viewRecommendColor1 = findViewById(R.id.view_recommend_color1)
    viewRecommendColor2 = findViewById(R.id.view_recommend_color2)
    viewRecommendColor3 = findViewById(R.id.view_recommend_color3)
    viewRecommendColor4 = findViewById(R.id.view_recommend_color4)
    viewRecommendColor5 = findViewById(R.id.view_recommend_color5)
    viewRecommendBgColor1 = findViewById(R.id.view_recommend_bg_color1)
    viewRecommendBgColor2 = findViewById(R.id.view_recommend_bg_color2)
    viewRecommendBgColor3 = findViewById(R.id.view_recommend_bg_color3)
    viewRecommendBgColor4 = findViewById(R.id.view_recommend_bg_color4)
    viewRecommendBgColor5 = findViewById(R.id.view_recommend_bg_color5)
    tvRecommendColor1 = findViewById(R.id.tv_recommend_color1)
    tvRecommendColor2 = findViewById(R.id.tv_recommend_color2)
    tvRecommendColor3 = findViewById(R.id.tv_recommend_color3)
    tvRecommendColor4 = findViewById(R.id.tv_recommend_color4)
    tvRecommendColor5 = findViewById(R.id.tv_recommend_color5)
    clDynamic = findViewById(R.id.cl_dynamic)
    clCustom = findViewById(R.id.cl_custom)
    clCustomContent = findViewById(R.id.cl_custom_content)
    clColorCustom = findViewById(R.id.cl_color_custom)
    clColorRecommend = findViewById(R.id.cl_color_recommend)
    clOverGrey = findViewById(R.id.cl_over_grey)
    clOverColor = findViewById(R.id.cl_over_color)
    tvColorCustom = findViewById(R.id.tv_color_custom)
    tvColorRecommend = findViewById(R.id.tv_color_recommend)
    tvConfirm = findViewById(R.id.tv_confirm)
    tvCancel = findViewById(R.id.tv_cancel)
    ivDynamic = findViewById(R.id.iv_dynamic)
    ivCustom = findViewById(R.id.iv_custom)
    tvDynamicTitle = findViewById(R.id.tv_dynamic_title)
    tvCustomTitle = findViewById(R.id.tv_custom_title)
    ivOverGreySelect = findViewById(R.id.iv_over_grey_select)
    ivOverColorSelect = findViewById(R.id.iv_over_color_select)
    tvOverGrey = findViewById(R.id.tv_over_grey)
    tvOverColor = findViewById(R.id.tv_over_color)

    val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
    customPseudoBean = intent.getParcelableExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN) ?: CustomPseudoBean.loadFromShared(isTC007)
    switchDynamicCustom(customPseudoBean.isUseCustomPseudo)

//loadtemperatureconfiguration
        etMaxTemp.setText(UnitTools.showNoUnit(customPseudoBean.maxTemp))
        etMinTemp.setText(UnitTools.showNoUnit(customPseudoBean.minTemp))
        tvMaxTempUnit.text = UnitTools.showUnit()
        tvMinTempUnit.text = UnitTools.showUnit()

    switchColorType(customPseudoBean.isColorCustom)

//load自定义颜色configuration
        pseudoPickView.onSelectChangeListener = {
            reset6CustomColor()
            colorSelectView.reset()
            when (pseudoPickView.sourceColors[it]) {
                0xff0000ff.toInt() -> viewCustomColor1.isSelected = true
                0xffff0000.toInt() -> viewCustomColor2.isSelected = true
                0xff00ff00.toInt() -> viewCustomColor3.isSelected = true
                0xffffff00.toInt() -> viewCustomColor4.isSelected = true
                0xff000000.toInt() -> viewCustomColor5.isSelected = true
                0xffffffff.toInt() -> viewCustomColor6.isSelected = true
            }
            colorSelectView.selectColor(pseudoPickView.sourceColors[it])
            ivCustomAdd.isEnabled = pseudoPickView.sourceColors.size < 7
            ivCustomDel.isEnabled = pseudoPickView.sourceColors.size > 3 && !pseudoPickView.isCurrentOnlyLimit()
        }
        pseudoPickView.reset(
            customPseudoBean.selectIndex,
            customPseudoBean.getCustomColors(),
            customPseudoBean.getCustomZAltitudes(),
            customPseudoBean.getCustomPlaces(),
        )

//load推荐颜色configuration
        viewRecommendColor1.background = buildRectDrawableArray(ColorRecommend.colorList1)
        viewRecommendColor2.background = buildRectDrawableArray(ColorRecommend.colorList2)
        viewRecommendColor3.background = buildRectDrawableArray(ColorRecommend.getColorByIndex(isTC007, 2))
        viewRecommendColor4.background = buildRectDrawableArray(ColorRecommend.colorList4)
        viewRecommendColor5.background = buildRectDrawableArray(ColorRecommend.colorList5)
        switchRecommendColorIndex(customPseudoBean.customRecommendIndex)

    switchUseGray(customPseudoBean.isUseGray)

        clDynamic.setOnClickListener(this)
        clCustom.setOnClickListener(this)
        tvColorCustom.setOnClickListener(this)
        tvColorRecommend.setOnClickListener(this)
        viewCustomColor1.setOnClickListener(this)
        viewCustomColor2.setOnClickListener(this)
        viewCustomColor3.setOnClickListener(this)
        viewCustomColor4.setOnClickListener(this)
        viewCustomColor5.setOnClickListener(this)
        viewCustomColor6.setOnClickListener(this)
        ivCustomAdd.setOnClickListener(this)
        ivCustomDel.setOnClickListener(this)
        viewRecommendBgColor1.setOnClickListener(this)
        viewRecommendBgColor2.setOnClickListener(this)
        viewRecommendBgColor3.setOnClickListener(this)
        viewRecommendBgColor4.setOnClickListener(this)
        viewRecommendBgColor5.setOnClickListener(this)
        clOverGrey.setOnClickListener(this)
        clOverColor.setOnClickListener(this)
        tvConfirm.setOnClickListener(this)
        tvCancel.setOnClickListener(this)

    colorSelectView.onSelectListener = {
    reset6CustomColor()
    when (it) {
    0xff0000ff.toInt() -> viewCustomColor1.isSelected = true
    0xffff0000.toInt() -> viewCustomColor2.isSelected = true
    0xff00ff00.toInt() -> viewCustomColor3.isSelected = true
    0xffffff00.toInt() -> viewCustomColor4.isSelected = true
    0xff000000.toInt() -> viewCustomColor5.isSelected = true
    0xffffffff.toInt() -> viewCustomColor6.isSelected = true
    }
    pseudoPickView.refreshColor(it)
    }
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            clDynamic -> { // 动态渲染
                switchDynamicCustom(false)
            }
            clCustom -> { // 自定义
                switchDynamicCustom(true)
            }
            tvColorCustom -> { // 颜色-自定义
                switchColorType(true)
            }
            tvColorRecommend -> { // 颜色-推荐
                switchColorType(false)
                switchRecommendColorIndex(customPseudoBean.customRecommendIndex)
            }

            viewCustomColor1 -> { // 颜色-自定义-颜色值拾取1
                reset6CustomColor()
                viewCustomColor1.isSelected = true
                colorSelectView.selectColor(0xff0000ff.toInt())
                pseudoPickView.refreshColor(0xff0000ff.toInt())
            }
            viewCustomColor2 -> { // 颜色-自定义-颜色值拾取2
                reset6CustomColor()
                viewCustomColor2.isSelected = true
                colorSelectView.selectColor(0xffff0000.toInt())
                pseudoPickView.refreshColor(0xffff0000.toInt())
            }
            viewCustomColor3 -> { // 颜色-自定义-颜色值拾取3
                reset6CustomColor()
                viewCustomColor3.isSelected = true
                colorSelectView.selectColor(0xff00ff00.toInt())
                pseudoPickView.refreshColor(0xff00ff00.toInt())
            }
            viewCustomColor4 -> { // 颜色-自定义-颜色值拾取4
                reset6CustomColor()
                viewCustomColor4.isSelected = true
                colorSelectView.selectColor(0xffffff00.toInt())
                pseudoPickView.refreshColor(0xffffff00.toInt())
            }
            viewCustomColor5 -> { // 颜色-自定义-颜色值拾取5
                reset6CustomColor()
                viewCustomColor5.isSelected = true
                colorSelectView.selectColor(0xff000000.toInt())
                pseudoPickView.refreshColor(0xff000000.toInt())
            }
            viewCustomColor6 -> { // 颜色-自定义-颜色值拾取6
                reset6CustomColor()
                viewCustomColor6.isSelected = true
                colorSelectView.selectColor(0xffffffff.toInt())
                pseudoPickView.refreshColor(0xffffffff.toInt())
            }

            ivCustomAdd -> { // 颜色-自定义-添加
                pseudoPickView.add()
            }
            ivCustomDel -> { // 颜色-自定义-删除
                pseudoPickView.del()
            }

            viewRecommendBgColor1 -> { // 颜色-推荐-铁红
                switchRecommendColorIndex(0)
            }
            viewRecommendBgColor2 -> { // 颜色-推荐-黑红
                switchRecommendColorIndex(1)
            }
            viewRecommendBgColor3 -> { // 颜色-推荐-自然
                switchRecommendColorIndex(2)
            }
            viewRecommendBgColor4 -> { // 颜色-推荐-岩浆
                switchRecommendColorIndex(3)
            }
            viewRecommendBgColor5 -> { // 颜色-推荐-辉金
                switchRecommendColorIndex(4)
            }

            clOverGrey -> { // 灰度渐变
                switchUseGray(true)
            }
            clOverColor -> { // 等色
                switchUseGray(false)
            }

            tvConfirm -> { // 确定
                if (clCustomContent.isVisible) { // 使用自定义渲染
                    val inputMax = etMaxTemp.text.toString()
                    if (inputMax.isEmpty()) {
                        ToastUtils.showShort(RUi.string.tip_input_format)
                        return
                    }
                    val inputMin = etMinTemp.text.toString()
                    if (inputMin.isEmpty()) {
                        ToastUtils.showShort(RUi.string.tip_input_format)
                        return
                    }

                    val maxTemp =
                        try {
                            UnitTools.showToCValue(BigDecimal(inputMax).setScale(1, RoundingMode.HALF_UP).toFloat())
                        } catch (e: NumberFormatException) {
                            null
                        }
                    val minTemp =
                        try {
                            UnitTools.showToCValue(BigDecimal(inputMin).setScale(1, RoundingMode.HALF_UP).toFloat())
                        } catch (e: NumberFormatException) {
                            null
                        }
                    if (maxTemp == null || minTemp == null || maxTemp < minTemp || maxTemp > 550f || minTemp < -20f) {
                        ToastUtils.showShort(RUi.string.tip_input_format)
                        return
                    }
                    if (maxTemp - minTemp < 0.1f) {
                        ToastUtils.showShort(RUi.string.tip_input_format)
                        return
                    }
                    customPseudoBean.maxTemp = maxTemp
                    customPseudoBean.minTemp = minTemp
                    customPseudoBean.selectIndex = pseudoPickView.selectIndex
                    customPseudoBean.colors = pseudoPickView.sourceColors
                    customPseudoBean.zAltitudes = pseudoPickView.zAltitudes
                    customPseudoBean.places = pseudoPickView.places
                }

                val resultIntent = Intent()
                resultIntent.putExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN, customPseudoBean)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            tvCancel -> { // 取消
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }


    private fun switchDynamicCustom(isToCustom: Boolean) {
        customPseudoBean.isUseCustomPseudo = isToCustom
        clCustomContent.isVisible = isToCustom
        clDynamic.isSelected = !isToCustom
        clCustom.isSelected = isToCustom
        ivDynamic.setImageResource(
            if (isToCustom) R.drawable.svg_pseudo_set_dynamic_not_select else R.drawable.svg_pseudo_set_dynamic_select,
        )
        ivCustom.setImageResource(if (isToCustom) R.drawable.svg_pseudo_set_custom_select else R.drawable.svg_pseudo_set_custom_not_select)
        tvDynamicTitle.setTextColor(if (isToCustom) 0xffffffff.toInt() else 0xffffba42.toInt())
        tvCustomTitle.setTextColor(if (isToCustom) 0xffffba42.toInt() else 0xffffffff.toInt())
    }


    private fun switchColorType(isToCustom: Boolean) {
    customPseudoBean.isColorCustom = isToCustom
    clColorCustom.isVisible = isToCustom
    clColorRecommend.isVisible = !isToCustom
    tvColorCustom.setTextColor(if (isToCustom) 0xffffba42.toInt() else 0xffffffff.toInt())
    tvColorRecommend.setTextColor(if (isToCustom) 0xffffffff.toInt() else 0xffffba42.toInt())
    tvColorCustom.setBackgroundResource(if (isToCustom) RCore.drawable.bg_corners50_solid_2a183e_stroke_theme else 0)
    tvColorRecommend.setBackgroundResource(if (isToCustom) 0 else RCore.drawable.bg_corners50_solid_2a183e_stroke_theme)
    }


    private fun reset6CustomColor() {
    viewCustomColor1.isSelected = false
    viewCustomColor2.isSelected = false
    viewCustomColor3.isSelected = false
    viewCustomColor4.isSelected = false
    viewCustomColor5.isSelected = false
    viewCustomColor6.isSelected = false
    }


    private fun switchRecommendColorIndex(index: Int) {
    when (customPseudoBean.customRecommendIndex) {
    0 -> {
    tvRecommendColor1.setTextColor(0x80ffffff.toInt())
    viewRecommendBgColor1.setBackgroundResource(RCore.drawable.bg_corners04_stroke_30_ff)
    }
    1 -> {
    tvRecommendColor2.setTextColor(0x80ffffff.toInt())
    viewRecommendBgColor2.setBackgroundResource(RCore.drawable.bg_corners04_stroke_30_ff)
    }
    2 -> {
    tvRecommendColor3.setTextColor(0x80ffffff.toInt())
    viewRecommendBgColor3.setBackgroundResource(RCore.drawable.bg_corners04_stroke_30_ff)
    }
    3 -> {
    tvRecommendColor4.setTextColor(0x80ffffff.toInt())
    viewRecommendBgColor4.setBackgroundResource(RCore.drawable.bg_corners04_stroke_30_ff)
    }
    4 -> {
    tvRecommendColor5.setTextColor(0x80ffffff.toInt())
    viewRecommendBgColor5.setBackgroundResource(RCore.drawable.bg_corners04_stroke_30_ff)
    }
    }
    when (index) {
    0 -> {
    tvRecommendColor1.setTextColor(0xffffba42.toInt())
    viewRecommendBgColor1.setBackgroundResource(RCore.drawable.bg_corners04_stroke_2dp_ffba42)
    }
    1 -> {
    tvRecommendColor2.setTextColor(0xffffba42.toInt())
    viewRecommendBgColor2.setBackgroundResource(RCore.drawable.bg_corners04_stroke_2dp_ffba42)
    }
    2 -> {
    tvRecommendColor3.setTextColor(0xffffba42.toInt())
    viewRecommendBgColor3.setBackgroundResource(RCore.drawable.bg_corners04_stroke_2dp_ffba42)
    }
    3 -> {
    tvRecommendColor4.setTextColor(0xffffba42.toInt())
    viewRecommendBgColor4.setBackgroundResource(RCore.drawable.bg_corners04_stroke_2dp_ffba42)
    }
    4 -> {
    tvRecommendColor5.setTextColor(0xffffba42.toInt())
    viewRecommendBgColor5.setBackgroundResource(RCore.drawable.bg_corners04_stroke_2dp_ffba42)
    }
    }
    customPseudoBean.customRecommendIndex = index
    }

    private fun switchUseGray(isUseGray: Boolean) {
        ivOverGreySelect.isVisible = isUseGray
        ivOverColorSelect.isVisible = !isUseGray
        tvOverGrey.setTextColor(if (isUseGray) 0xffffba42.toInt() else 0xffffffff.toInt())
        tvOverColor.setTextColor(if (isUseGray) 0xffffffff.toInt() else 0xffffba42.toInt())
        clOverGrey.setBackgroundResource(
            if (isUseGray) RCore.drawable.bg_corners05_solid_2a183e_stroke_theme else RCore.drawable.bg_corners05_solid_626569,
        )
        clOverColor.setBackgroundResource(
            if (isUseGray) RCore.drawable.bg_corners05_solid_626569 else RCore.drawable.bg_corners05_solid_2a183e_stroke_theme,
        )
        customPseudoBean.isUseGray = isUseGray
    }

    private fun buildRectDrawableArray(color: IntArray): GradientDrawable {
    val drawable = GradientDrawable()
    drawable.shape = GradientDrawable.RECTANGLE
    drawable.cornerRadius = SizeUtils.dp2px(4f).toFloat()
    drawable.colors = color
    drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
    return drawable
    }
}
