package com.topdon.module.thermal.ir.report.activity

import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.*
import com.topdon.module.thermal.ir.report.view.ReportIRInputView
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 生成报告第2步（共2步）.
 *
 * 需要传递
 * - 必选：是否 TC007: [ExtraKeyConfig.IS_TC007] 透传，再次拾取图片时进入目录不同，上传报告参数不同
 * - 必选：当前编辑的图片绝对路径 [ExtraKeyConfig.FILE_ABSOLUTE_PATH]
 * - 必选：当前编辑的图片点线面全图温度数据 [ExtraKeyConfig.IMAGE_TEMP_BEAN]
 * - 必选：报告信息 [ExtraKeyConfig.REPORT_INFO]
 * - 可选：检测条件 [ExtraKeyConfig.REPORT_CONDITION]
 * - 可选：当前已确认的图片信息列表 [ExtraKeyConfig.REPORT_IR_LIST]
 */
// Legacy ARouter route annotation - now using NavigationManager
class ReportCreateSecondActivity: BaseActivity(), View.OnClickListener {

    // View references using findViewById with lazy initialization
    private val tvAddImage: TextView by lazy { findViewById(R.id.tv_add_image) }
    private val tvPreview: TextView by lazy { findViewById(R.id.tv_preview) }
    private val scrollView: ScrollView by lazy { findViewById(R.id.scroll_view) }
    private val ivImage: ImageView by lazy { findViewById(R.id.iv_image) }
    private val reportTempViewFull: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_full) }
    private val reportTempViewPoint1: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_point1) }
    private val reportTempViewPoint2: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_point2) }
    private val reportTempViewPoint3: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_point3) }
    private val reportTempViewPoint4: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_point4) }
    private val reportTempViewPoint5: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_point5) }
    private val reportTempViewLine1: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_line1) }
    private val reportTempViewLine2: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_line2) }
    private val reportTempViewLine3: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_line3) }
    private val reportTempViewLine4: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_line4) }
    private val reportTempViewLine5: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_line5) }
    private val reportTempViewRect1: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_rect1) }
    private val reportTempViewRect2: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_rect2) }
    private val reportTempViewRect3: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_rect3) }
    private val reportTempViewRect4: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_rect4) }
    private val reportTempViewRect5: ReportIRInputView by lazy { findViewById(R.id.report_temp_view_rect5) }

    /**
     * 当前已添加的图片信息列表.
     */
    private var reportIRList: ArrayList<ReportIRBean> = ArrayList(0)


    /**
     * 从上一界面传递过来的，添加的图片绝对路径.
     */
    private var currentFilePath: String = ""
    /**
     * 从上一界面传递过来的，当前编辑的图片点线面全图温度数据
     */
    private var imageTempBean: ImageTempBean? = null



    override fun initContentView() = R.layout.activity_report_create_second

    override fun initView() {
        // Views are now initialized using lazy properties - no manual findViewById needed
        
        currentFilePath = intent.getStringExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)!!
        imageTempBean = intent.getParcelableExtra(ExtraKeyConfig.IMAGE_TEMP_BEAN)
        reportIRList = intent.getParcelableArrayListExtra(ExtraKeyConfig.REPORT_IR_LIST) ?: ArrayList(10)

        refreshImg(currentFilePath)
        refreshData(imageTempBean)

        tvAddImage.setOnClickListener(this)
        tvPreview.setOnClickListener(this)
    }

    override fun initData() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: ReportCreateEvent) {
        finish()
    }

    private fun refreshImg(absolutePath: String?) {
        lifecycleScope.launch {
            val drawable = GlideLoader.getDrawable(this@ReportCreateSecondActivity, absolutePath)
            val isLand = (drawable?.intrinsicWidth ?: 0) > (drawable?.intrinsicHeight ?: 0)
            val width = (ScreenUtil.getScreenWidth(this@ReportCreateSecondActivity) * (if (isLand) 234 else 175) / 375f).toInt()
            val layoutParams = ivImage.layoutParams
            layoutParams.width = width
            layoutParams.height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth ?: 1)).toInt()
            ivImage.layoutParams = layoutParams
            ivImage.setImageDrawable(drawable)
        }
    }

    private fun refreshData(imageTempBean: ImageTempBean?) {
        scrollView.scrollTo(0, 0)

        reportTempViewFull.isVisible = imageTempBean?.full != null
        reportTempViewFull.refreshData(imageTempBean?.full)

        reportTempViewPoint1.isVisible = (imageTempBean?.pointList?.size ?: 0) > 0
        if ((imageTempBean?.pointList?.size ?: 0) > 0) {
            reportTempViewPoint1.refreshData(imageTempBean?.pointList?.get(0))
        }
        reportTempViewPoint2.isVisible = (imageTempBean?.pointList?.size ?: 0) > 1
        if ((imageTempBean?.pointList?.size ?: 0) > 1) {
            reportTempViewPoint2.refreshData(imageTempBean?.pointList?.get(1))
        }
        reportTempViewPoint3.isVisible = (imageTempBean?.pointList?.size ?: 0) > 2
        if ((imageTempBean?.pointList?.size ?: 0) > 2) {
            reportTempViewPoint3.refreshData(imageTempBean?.pointList?.get(2))
        }
        reportTempViewPoint4.isVisible = (imageTempBean?.pointList?.size ?: 0) > 3
        if ((imageTempBean?.pointList?.size ?: 0) > 3) {
            reportTempViewPoint4.refreshData(imageTempBean?.pointList?.get(3))
        }
        reportTempViewPoint5.isVisible = (imageTempBean?.pointList?.size ?: 0) > 4
        if ((imageTempBean?.pointList?.size ?: 0) > 4) {
            reportTempViewPoint5.refreshData(imageTempBean?.pointList?.get(4))
        }

        reportTempViewLine1.isVisible = (imageTempBean?.lineList?.size ?: 0) > 0
        if ((imageTempBean?.lineList?.size ?: 0) > 0) {
            reportTempViewLine1.refreshData(imageTempBean?.lineList?.get(0))
        }
        reportTempViewLine2.isVisible = (imageTempBean?.lineList?.size ?: 0) > 1
        if ((imageTempBean?.lineList?.size ?: 0) > 1) {
            reportTempViewLine2.refreshData(imageTempBean?.lineList?.get(1))
        }
        reportTempViewLine3.isVisible = (imageTempBean?.lineList?.size ?: 0) > 2
        if ((imageTempBean?.lineList?.size ?: 0) > 2) {
            reportTempViewLine3.refreshData(imageTempBean?.lineList?.get(2))
        }
        reportTempViewLine4.isVisible = (imageTempBean?.lineList?.size ?: 0) > 3
        if ((imageTempBean?.lineList?.size ?: 0) > 3) {
            reportTempViewLine4.refreshData(imageTempBean?.lineList?.get(3))
        }
        reportTempViewLine5.isVisible = (imageTempBean?.lineList?.size ?: 0) > 4
        if ((imageTempBean?.lineList?.size ?: 0) > 4) {
            reportTempViewLine5.refreshData(imageTempBean?.lineList?.get(4))
        }

        reportTempViewRect1.isVisible = (imageTempBean?.rectList?.size ?: 0) > 0
        if ((imageTempBean?.rectList?.size ?: 0) > 0) {
            reportTempViewRect1.refreshData(imageTempBean?.rectList?.get(0))
        }
        reportTempViewRect2.isVisible = (imageTempBean?.rectList?.size ?: 0) > 1
        if ((imageTempBean?.rectList?.size ?: 0) > 1) {
            reportTempViewRect2.refreshData(imageTempBean?.rectList?.get(1))
        }
        reportTempViewRect3.isVisible = (imageTempBean?.rectList?.size ?: 0) > 2
        if ((imageTempBean?.rectList?.size ?: 0) > 2) {
            reportTempViewRect3.refreshData(imageTempBean?.rectList?.get(2))
        }
        reportTempViewRect4.isVisible = (imageTempBean?.rectList?.size ?: 0) > 3
        if ((imageTempBean?.rectList?.size ?: 0) > 3) {
            reportTempViewRect4.refreshData(imageTempBean?.rectList?.get(3))
        }
        reportTempViewRect5.isVisible = (imageTempBean?.rectList?.size ?: 0) > 4
        if ((imageTempBean?.rectList?.size ?: 0) > 4) {
            reportTempViewRect5.refreshData(imageTempBean?.rectList?.get(4))
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            tvAddImage -> {//添加图片
                if (reportIRList.size >= 9) {
                    ToastUtils.showShort(R.string.album_report_max_image_tips)
                    return
                }
                val reportIRBeanList = ArrayList<ReportIRBean>(reportIRList)
                reportIRBeanList.add(buildReportIr(currentFilePath))
                val reportInfo = intent.getParcelableExtra<ReportInfoBean>(ExtraKeyConfig.REPORT_INFO)
                val reportCondition = intent.getParcelableExtra<ReportConditionBean>(ExtraKeyConfig.REPORT_CONDITION)
                if (reportInfo != null && reportCondition != null) {
                    NavigationManager.getInstance()
                        .build(RouterConfig.REPORT_PICK_IMG)
                        .withBoolean(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
                        .withParcelable(ExtraKeyConfig.REPORT_INFO, reportInfo)
                        .withParcelable(ExtraKeyConfig.REPORT_CONDITION, reportCondition)
                        .withParcelableArrayList(ExtraKeyConfig.REPORT_IR_LIST, reportIRBeanList)
                        .navigation(this)
                }
            }
            tvPreview -> {//预览
                val appLanguage = ConstantLanguages.ENGLISH
                val sdkVersion = "1.2.8_23050619"
                val reportInfoBean: ReportInfoBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO)
                val conditionBean: ReportConditionBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION)
                val reportIRBeanList = ArrayList<ReportIRBean>(reportIRList)
                reportIRBeanList.add(buildReportIr(currentFilePath))
                val reportBean = ReportBean(SoftwareInfo(appLanguage, sdkVersion), reportInfoBean!!, conditionBean!!, reportIRBeanList)
                NavigationManager.getInstance().build(RouterConfig.REPORT_PREVIEW_SECOND)
                    .withBoolean(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
                    .withParcelable(ExtraKeyConfig.REPORT_BEAN, reportBean)
                    .navigation(this)
            }
        }
    }

    private fun buildReportIr(filePath: String): ReportIRBean {
        val full: ReportTempBean? = if (imageTempBean?.full != null) {
            ReportTempBean(
                if ((reportTempViewFull as? ReportIRInputView)?.getMaxInput()?.isNotEmpty() == true) (reportTempViewFull as ReportIRInputView).getMaxInput() + UnitTools.showUnit() else "",
                if ((reportTempViewFull as? ReportIRInputView)?.isSwitchMaxCheck() == true && (reportTempViewFull as ReportIRInputView).getMaxInput().isNotEmpty()) 1 else 0,
                if ((reportTempViewFull as? ReportIRInputView)?.getMinInput()?.isNotEmpty() == true) (reportTempViewFull as ReportIRInputView).getMinInput() + UnitTools.showUnit() else "",
                if ((reportTempViewFull as? ReportIRInputView)?.isSwitchMinCheck() == true && (reportTempViewFull as ReportIRInputView).getMinInput().isNotEmpty()) 1 else 0,
                (reportTempViewFull as? ReportIRInputView)?.getExplainInput() ?: "",
                if ((reportTempViewFull as? ReportIRInputView)?.isSwitchExplainCheck() == true && (reportTempViewFull as ReportIRInputView).getExplainInput().isNotEmpty()) 1 else 0
            )
        } else {
            null
        }

        val pointList = buildReportTempBeanList(1)
        val lienList = buildReportTempBeanList(2)
        val rectList = buildReportTempBeanList(3)
        return ReportIRBean("", filePath, full, pointList, lienList, rectList)
    }

    /**
     * 构建报告点线面数据列表.
     * @param type 1-点 2-线 3-面
     */
    private fun buildReportTempBeanList(type: Int): ArrayList<ReportTempBean> {
        val size = when (type) {
            1 -> imageTempBean?.pointList?.size ?: 0
            2 -> imageTempBean?.lineList?.size ?: 0
            else -> imageTempBean?.rectList?.size ?: 0
        }
        val resultList = ArrayList<ReportTempBean>(size)
        for (i in 0 until size) {
            val reportTempView = when (type) {
                1 -> { //点
                    when (i) {
                        0 -> reportTempViewPoint1
                        1 -> reportTempViewPoint2
                        2 -> reportTempViewPoint3
                        3 -> reportTempViewPoint4
                        else -> reportTempViewPoint5
                    }
                }
                2 -> { //线
                    when (i) {
                        0 -> reportTempViewLine1
                        1 -> reportTempViewLine2
                        2 -> reportTempViewLine3
                        3 -> reportTempViewLine4
                        else -> reportTempViewLine5
                    }
                }
                else -> { //面
                    when (i) {
                        0 -> reportTempViewRect1
                        1 -> reportTempViewRect2
                        else -> reportTempViewRect3
                    }
                }
            }
            val reportTempBean = if (type == 1) {//点的数据封装不太一样
                ReportTempBean(
                    if ((reportTempView as? ReportIRInputView)?.getMaxInput()?.isNotEmpty() == true) (reportTempView as ReportIRInputView).getMaxInput() + UnitTools.showUnit() else "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchMaxCheck() == true && (reportTempView as ReportIRInputView).getMaxInput().isNotEmpty()) 1 else 0,
                    (reportTempView as? ReportIRInputView)?.getExplainInput() ?: "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchExplainCheck() == true && (reportTempView as ReportIRInputView).getExplainInput().isNotEmpty()) 1 else 0
                )
            } else {
                ReportTempBean(
                    if ((reportTempView as? ReportIRInputView)?.getMaxInput()?.isNotEmpty() == true) (reportTempView as ReportIRInputView).getMaxInput() + UnitTools.showUnit() else "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchMaxCheck() == true && (reportTempView as ReportIRInputView).getMaxInput().isNotEmpty()) 1 else 0,
                    if ((reportTempView as? ReportIRInputView)?.getMinInput()?.isNotEmpty() == true) (reportTempView as ReportIRInputView).getMinInput() + UnitTools.showUnit() else "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchMinCheck() == true && (reportTempView as ReportIRInputView).getMinInput().isNotEmpty()) 1 else 0,
                    (reportTempView as? ReportIRInputView)?.getExplainInput() ?: "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchExplainCheck() == true && (reportTempView as ReportIRInputView).getExplainInput().isNotEmpty()) 1 else 0,
                    if ((reportTempView as? ReportIRInputView)?.getAverageInput()?.isNotEmpty() == true) (reportTempView as ReportIRInputView).getAverageInput() + UnitTools.showUnit() else "",
                    if ((reportTempView as? ReportIRInputView)?.isSwitchAverageCheck() == true && (reportTempView as ReportIRInputView).getAverageInput().isNotEmpty()) 1 else 0
                )
            }
            resultList.add(reportTempBean)
        }
        return resultList
    }

}