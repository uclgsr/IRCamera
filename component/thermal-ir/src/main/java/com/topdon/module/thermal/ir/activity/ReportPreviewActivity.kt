package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.CollectionUtils
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.topdon.house.activity.SignInputActivity
import com.topdon.house.event.HouseReportAddEvent
import com.topdon.house.util.PDFUtil
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.house.viewmodel.ReportViewModel
import com.topdon.lib.core.bean.HouseRepPreviewAlbumItemBean
import com.topdon.lib.core.bean.HouseRepPreviewBean
import com.topdon.lib.core.bean.HouseRepPreviewItemBean
import com.topdon.lib.core.bean.HouseRepPreviewProjectItemBean
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseReport
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.adapter.ReportPreviewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

/**
 * 需要传递：
 * - [ExtraKeyConfig.IS_REPORT] - true-查看报告即查看 false-查看检测即生成
 * - [ExtraKeyConfig.LONG_ID] - 房屋检测Id(生成时)  房屋报告Id(查看时）
 */
// Legacy ARouter route annotation - now using NavigationManager
class ReportPreviewActivity : BaseActivity(), View.OnClickListener {

    private val detectViewModel: DetectViewModel by viewModels()
    private val reportViewModel: ReportViewModel by viewModels()

    // View declarations
    private lateinit var tvSave: android.widget.TextView
    private lateinit var rlyInspectorSignature: android.widget.RelativeLayout
    private lateinit var rlyHouseOwnerSignature: android.widget.RelativeLayout
    private lateinit var toolbarBackImg: android.widget.ImageView
    private lateinit var clSign: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var layAppbar: com.google.android.material.appbar.AppBarLayout
    private lateinit var layToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var llSave: android.widget.LinearLayout
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var ivHeaderBg: android.widget.ImageView
    private lateinit var tvAddress: android.widget.TextView
    private lateinit var tvHouseName: android.widget.TextView
    private lateinit var tvDetectTime: android.widget.TextView
    private lateinit var ivInspectorSignature: android.widget.ImageView
    private lateinit var ivHouseOwnerSignature: android.widget.ImageView
    private lateinit var tvInspector: android.widget.TextView
    private lateinit var tvBuildYear: android.widget.TextView
    private lateinit var tvArea: android.widget.TextView
    private lateinit var tvCost: android.widget.TextView
    private lateinit var rcyFloor: androidx.recyclerview.widget.RecyclerView

    /**
     * true-查看报告即查看 false-查看检测即生成
     */
    private var isReport = false
    private var houseReport = HouseReport()
    private var mPreviewBean: HouseRepPreviewBean? = null

    override fun initContentView() = R.layout.activity_report_preview

    override fun initView() {
        // Initialize views
        tvSave = findViewById(R.id.tv_save)
        rlyInspectorSignature = findViewById(R.id.rly_inspector_signature)
        rlyHouseOwnerSignature = findViewById(R.id.rly_house_owner_signature)
        toolbarBackImg = findViewById(R.id.toolbar_back_img)
        clSign = findViewById(R.id.cl_sign)
        layAppbar = findViewById(R.id.lay_appbar)
        layToolbar = findViewById(R.id.lay_toolbar)
        llSave = findViewById(R.id.ll_save)
        scrollView = findViewById(R.id.scroll_view)
        ivHeaderBg = findViewById(R.id.iv_header_bg)
        tvAddress = findViewById(R.id.tv_address)
        tvHouseName = findViewById(R.id.tv_house_name)
        tvDetectTime = findViewById(R.id.tv_detect_time)
        ivInspectorSignature = findViewById(R.id.iv_inspector_signature)
        ivHouseOwnerSignature = findViewById(R.id.iv_house_owner_signature)
        tvInspector = findViewById(R.id.tv_inspector)
        tvBuildYear = findViewById(R.id.tv_build_year)
        tvArea = findViewById(R.id.tv_area)
        tvCost = findViewById(R.id.tv_cost)
        rcyFloor = findViewById(R.id.rcy_floor)
        
        showLoadingDialog("")
        isReport = intent.getBooleanExtra(ExtraKeyConfig.IS_REPORT, false)
        tvSave.isEnabled = false
        rlyInspectorSignature.isEnabled = !isReport
        rlyHouseOwnerSignature.isEnabled = !isReport
        tvSave.text = if (isReport) getString(R.string.battery_share) else getString(R.string.finalize_and_save)
        toolbarBackImg.setOnClickListener(this)
        tvSave.setOnClickListener(this)
        rlyInspectorSignature.setOnClickListener(this)
        rlyHouseOwnerSignature.setOnClickListener(this)

        if(clSign.isShown){
            val mAppBarChildAt: View = layAppbar.getChildAt(0)
            val mAppBarParams = mAppBarChildAt.layoutParams as AppBarLayout.LayoutParams
            mAppBarParams.scrollFlags = 0
        }

        detectViewModel.detectLD.observe(this) {
            tvSave.isEnabled = it != null
            if (it != null) {
                houseReport = it.toHouseReport()
                mPreviewBean = convertDataModel(houseReport)
                setAdapter()
            }
            dismissLoadingDialog()
        }
        reportViewModel.reportLD.observe(this) {
            tvSave.isEnabled = it != null
            if (it != null) {
                houseReport = it
                mPreviewBean = convertDataModel(it)
                setAdapter()
            }
            dismissLoadingDialog()
        }

        if (isReport) {//查看报告
            reportViewModel.queryById(intent.getLongExtra(ExtraKeyConfig.LONG_ID, 0))
        } else {//生成报告
            detectViewModel.queryById(intent.getLongExtra(ExtraKeyConfig.LONG_ID, 0))
        }
    }

    override fun initData() {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setAvatorChange()
    }

    private fun setAvatorChange() {
        layAppbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            //verticalOffset始终为0以下的负数
            val percent = abs(verticalOffset * 1.0f) / appBarLayout.totalScrollRange
            layToolbar.setBackgroundColor(changeAlpha(getColor(R.color.color_23202E), percent))
        }
    }

    private fun changeAlpha(color: Int, fraction: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val alpha = (Color.alpha(color) * fraction).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    override fun onClick(v: View?) {
        when (v) {
            toolbarBackImg -> {
                finish()
            }

            rlyInspectorSignature -> {
                var intent = Intent(this, SignInputActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, true)
                startActivityForResult(intent, 1000)
            }

            rlyHouseOwnerSignature -> {
                var intent = Intent(this, SignInputActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, false)
                startActivityForResult(intent, 1001)
            }

            tvSave -> {
                if (isReport) {//分享
                    lifecycleScope.launch {
                        showLoadingDialog()
                        PDFUtil.delAllPDF(this@ReportPreviewActivity)
                        val pdfUri: Uri? = PDFUtil.savePDF(this@ReportPreviewActivity, houseReport)
                        dismissLoadingDialog()
                        if (pdfUri != null) {
                            val shareIntent = Intent()
                            shareIntent.action = Intent.ACTION_SEND
                            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
                            shareIntent.type = "application/pdf"
                            startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
                        }
                    }
                } else {//定稿并保存
                    if (houseReport.inspectorWhitePath.isEmpty() || houseReport.houseOwnerWhitePath.isEmpty()) {
                        if (clSign.bottom + layAppbar.height > llSave.top) {
                            layAppbar.setExpanded(false, true)
                            scrollView.smoothScrollTo(0, clSign.top)
                        }
                        TToast.shortToast(this, R.string.pdf_sign_tips)
                        return
                    }
                    showLoadingDialog("")
                    lifecycleScope.launch(Dispatchers.IO) {
                        val currentTime = System.currentTimeMillis()
                        houseReport.createTime = currentTime
                        houseReport.updateTime = currentTime
                        AppDatabase.getInstance().houseReportDao().insert(houseReport)
                        lifecycleScope.launch(Dispatchers.Main) {
                            dismissLoadingDialog()
                            TToast.shortToast(this@ReportPreviewActivity, R.string.pdf_saved_tips)
                            EventBus.getDefault().post(HouseReportAddEvent())
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val whitePath = data?.getStringExtra(ExtraKeyConfig.RESULT_PATH_WHITE) ?: return
            val blackPath = data.getStringExtra(ExtraKeyConfig.RESULT_PATH_BLACK) ?: return
            when (requestCode) {
                1000 -> {
                    //检测师签名
                    Glide.with(this).load(whitePath).into(ivInspectorSignature)
                    houseReport.inspectorWhitePath = whitePath
                    houseReport.inspectorBlackPath = blackPath
                }

                1001 -> {
                    //房主签名
                    Glide.with(this).load(whitePath).into(ivHouseOwnerSignature)
                    houseReport.houseOwnerWhitePath = whitePath
                    houseReport.houseOwnerBlackPath = blackPath
                }
            }
        }
    }

    private fun convertDataModel(houseReport: HouseReport): HouseRepPreviewBean {
        var houseRepPreviewBean = HouseRepPreviewBean()
        houseRepPreviewBean.housePhoto = houseReport.imagePath
        houseRepPreviewBean.houseAddress = houseReport.address
        houseRepPreviewBean.houseName = houseReport.name
        houseRepPreviewBean.detectTime =
            "${getString(R.string.detect_time)}${": "}${TimeTool.formatDetectTime(houseReport.detectTime)}"
        houseRepPreviewBean.inspectorName = houseReport.inspectorName
        houseRepPreviewBean.houseYear =
            if (houseReport.year == null) "--" else "${houseReport.year?.toString()}${getString(R.string.year)}"
        houseRepPreviewBean.houseArea =
            if (houseReport.houseSpace.isEmpty()) "--" else "${houseReport.houseSpace} ${houseReport.getSpaceUnitStr()}"
        houseRepPreviewBean.expenses =
            if (houseReport.cost.isEmpty()) "--" else "${resources.getStringArray(R.array.currency)[houseReport.costUnit]} ${houseReport.cost}"
        houseRepPreviewBean.itemBeans = ArrayList<HouseRepPreviewItemBean>()
        houseReport.dirList.forEachIndexed { _, dirReport ->
            var itemBean = HouseRepPreviewItemBean()
            itemBean.itemName = dirReport.dirName
            var count = dirReport.goodCount + dirReport.warnCount + dirReport.dangerCount
            itemBean.projectItemBeans = ArrayList<HouseRepPreviewProjectItemBean>()
            itemBean.albumItemBeans = ArrayList<HouseRepPreviewAlbumItemBean>()

            dirReport.itemList.forEachIndexed { _, itemReport ->
                var projectItemBean = HouseRepPreviewProjectItemBean()
                projectItemBean.projectName = itemReport.itemName
                projectItemBean.state = itemReport.state
                projectItemBean.remark = itemReport.inputText
                if (itemReport.state > 0 || itemReport.inputText.isNotEmpty()) {
                    itemBean.projectItemBeans.add(projectItemBean)
                }

                if (itemReport.getImageSize() > 0) {
                    var albumItemBean: HouseRepPreviewAlbumItemBean? = null
                    if (itemReport.image1.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image1
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image2.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image2
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image3.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image3
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                    if (itemReport.image4.isNotEmpty()) {
                        albumItemBean = HouseRepPreviewAlbumItemBean()
                        albumItemBean.photoPath = itemReport.image4
                        albumItemBean.title = itemReport.itemName
                        itemBean.albumItemBeans.add(albumItemBean)
                    }
                }
            }

            var isEmpty =
                CollectionUtils.isEmpty(itemBean.projectItemBeans) && CollectionUtils.isEmpty(
                    itemBean.albumItemBeans
                )
            if (CollectionUtils.isNotEmpty(itemBean.projectItemBeans)) {
                itemBean.projectItemBeans.add(0, HouseRepPreviewProjectItemBean())
            }
            if (!isEmpty) {
                houseRepPreviewBean.itemBeans.add(itemBean)
            }
        }
        houseRepPreviewBean.inspectorWhitePath = houseReport.inspectorWhitePath
        houseRepPreviewBean.houseOwnerWhitePath = houseReport.houseOwnerWhitePath
        return houseRepPreviewBean
    }

    private fun setAdapter() {
        mPreviewBean?.let {
            Glide.with(this).load(it.housePhoto).into(ivHeaderBg)
            tvAddress.text = it.houseAddress
            tvHouseName.text = it.houseName
            tvDetectTime.text = it.detectTime
            tvInspector.text = it.inspectorName
            tvBuildYear.text = it.houseYear
            tvArea.text = it.houseArea
            tvCost.text = it.expenses

            rcyFloor.layoutManager = LinearLayoutManager(this)
            val reportPreviewAdapter = ReportPreviewAdapter(this, it.itemBeans)
            rcyFloor.isNestedScrollingEnabled = false
            rcyFloor.adapter = reportPreviewAdapter

            Glide.with(this).load(it.inspectorWhitePath).into(ivInspectorSignature)
            Glide.with(this).load(it.houseOwnerWhitePath).into(ivHouseOwnerSignature)
        }
    }
}