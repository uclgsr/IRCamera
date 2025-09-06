package com.topdon.house.activity

import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.house.event.DetectDirListEvent
import com.topdon.house.event.DetectItemListEvent
import com.topdon.house.event.HouseReportAddEvent
import com.topdon.house.popup.ThreePickPopup
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.entity.HouseDetect
import com.topdon.lib.core.db.entity.ItemDetect
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.tools.PermissionTool
import com.topdon.house.view.HouseDetectView
import com.topdon.lms.sdk.weiget.TToast
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * 报告生成（即检测）.
 *
 * 需要传递：
 * - [ExtraKeyConfig.DETECT_ID] - 执行检测的房屋检测 Id
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007
 *
 * Created by LCG on 2024/8/23.
 */
class ReportAddActivity : BaseActivity(), View.OnClickListener {
    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false

    /**
     * 所有目录是否都已展开.
     * true-都已展开 false-至少有1个未展开
     */
    private var isAllExpand = false

    private val viewModel: DetectViewModel by viewModels()
    
    // View references
    private lateinit var ivExpand: View
    private lateinit var ivBack: View  
    private lateinit var ivEdit: View
    private lateinit var tvAdd: View
    private lateinit var tvExportReport: View
    private lateinit var clEmpty: View
    private lateinit var viewHouseDetect: HouseDetectView

    override fun initContentView(): Int = R.layout.activity_report_add

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        // Initialize view references
        ivExpand = findViewById(R.id.iv_expand)
        ivBack = findViewById(R.id.iv_back)
        ivEdit = findViewById(R.id.iv_edit) 
        tvAdd = findViewById(R.id.tv_add)
        tvExportReport = findViewById(R.id.tv_export_report)
        clEmpty = findViewById(R.id.cl_empty)
        viewHouseDetect = findViewById(R.id.view_house_detect)

        ivExpand.isEnabled = false
        ivBack.setOnClickListener(this)
        ivEdit.setOnClickListener(this)
        ivExpand.setOnClickListener(this)
        tvAdd.setOnClickListener(this)
        tvExportReport.setOnClickListener(this)

        initDetectViewListener()

        viewModel.detectLD.observe(this) {
            //查询当前检测结果
            if (it != null) {
                isAllExpand = false

                clEmpty.isVisible = it.dirList.isEmpty()
                viewHouseDetect.isVisible = it.dirList.isNotEmpty()
                tvExportReport.isVisible = it.dirList.isNotEmpty()

                viewHouseDetect.refresh(it.dirList)

                ivEdit.isEnabled = it.dirList.isNotEmpty()
                ivExpand.isEnabled = it.dirList.isNotEmpty()
                ivExpand.isSelected = isAllExpand
            }
        }
        viewModel.copyDirLD.observe(this) {
            //复制目录结果
            viewHouseDetect.notifyDirInsert(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, LibR.string.ts004_copy_success)
        }
        viewModel.copyItemLD.observe(this) {
            //复制项目结果
            viewHouseDetect.notifyItemInsert(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, LibR.string.ts004_copy_success)
        }
        viewModel.delItemLD.observe(this) {
            //删除项目结果
            viewHouseDetect.notifyItemRemove(it.first, it.second)
            TToast.shortToast(this@ReportAddActivity, LibR.string.test_results_delete_success)
        }


        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            ivBack -> finish()
            ivEdit -> {//目录编辑
                val newIntent = Intent(this, DirEditActivity::class.java)
                newIntent.putExtra(ExtraKeyConfig.DETECT_ID, intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
                startActivity(newIntent)
            }
            ivExpand -> {//展开收起
                isAllExpand = !isAllExpand
                if (isAllExpand) {
                    viewHouseDetect.expandAllDir()
                } else {
                    viewHouseDetect.retractAllDir()
                }
                ivExpand.isSelected = isAllExpand
            }
            tvExportReport -> {//导出报告
                NavigationManager.getInstance().build(RouterConfig.REPORT_PREVIEW)
                    .withBoolean(ExtraKeyConfig.IS_REPORT, false)
                    .withLong(ExtraKeyConfig.LONG_ID, intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
                    .navigation(this)
            }
            tvAdd -> {//新增默认目录
                val detect: HouseDetect? = viewModel.detectLD.value
                if (detect != null) {
                    viewModel.insertDefaultDirs(detect)
                }
            }
        }
    }

    /**
     * 当前正在编辑的项目，在 viewHouseDetect 中的 index.
     */
    private var editLayoutIndex = 0
    /**
     * 当前正在编辑的项目
     */
    private var editItemDetect = ItemDetect()

    /**
     * 初始化 viewHouseDetect 的相关事件监听.
     */
    private fun initDetectViewListener() {
        viewHouseDetect.onDirCopyListener = {//目录复制
            viewModel.copyDir(it.first, it.second)
        }
        viewHouseDetect.onItemCopyListener = {//项目复制
            viewModel.copyItem(it.first, it.second)
        }
        viewHouseDetect.onItemDelListener = {
            viewModel.delItem(it.first, it.second)
        }

        viewHouseDetect.onImageAddListener = { layoutIndex, v, item ->
            //项目添加图片
            editLayoutIndex = layoutIndex
            editItemDetect = item
            ThreePickPopup(this, arrayListOf(LibR.string.person_headshot_phone, LibR.string.light_camera_take_photo, LibR.string.ir_camera_take_photo)) {
                when (it) {
                    0 -> {//从相册获取
                        PermissionTool.requestImageRead(this) {
                            galleryPickResult.launch("image/*")
                        }
                    }
                    1 -> {//相机拍照
                        PermissionTool.requestCamera(this) {
                            val fileName = "Item${System.currentTimeMillis()}.png"
                            val file = FileConfig.getDetectImageDir(this, fileName)
                            lightPhotoResult.launch(file)
                        }
                    }
                    2 -> {//红外线拍照
                        if ((isTC007 && !WebSocketProxy.getInstance().isTC007Connect()) || (!isTC007 && !DeviceTools.isConnect())) {
                            TToast.shortToast(this@ReportAddActivity, LibR.string.device_disconnect)
                        } else {
                            val fileName = "Item${System.currentTimeMillis()}.png"
                            val file = FileConfig.getDetectImageDir(this, fileName)
                            NavigationManager.jumpImagePick(this@ReportAddActivity, isTC007, file.absolutePath)
                        }
                    }
                }
            }.show(v, true)
        }
        viewHouseDetect.onTextInputListener = {
            //项目文字输入
            editLayoutIndex = it.first
            editItemDetect = it.second
            val intent = Intent(this, TextInputActivity::class.java)
            intent.putExtra(ExtraKeyConfig.ITEM_NAME, it.second.itemName)
            intent.putExtra(ExtraKeyConfig.RESULT_INPUT_TEXT, it.second.inputText)
            textInputResult.launch(intent)
        }

        viewHouseDetect.onDirChangeListener = {
            //目录数据变更（3种状态数量）
            viewModel.updateDir(it)
        }
        viewHouseDetect.onDirExpandListener = {
            //一个目录展开收起状态变化
            if (it) {
                if (!isAllExpand) {
                    val detect: HouseDetect? = viewModel.detectLD.value
                    if (detect != null) {
                        isAllExpand = true
                        for (dir in detect.dirList) {
                            if (!dir.isExpand) {
                                isAllExpand = false
                                break
                            }
                        }
                    }
                }
            } else {
                isAllExpand = false
            }
            ivExpand.isSelected = isAllExpand
        }
        viewHouseDetect.onItemChangeListener = {
            //项目数据变更（3种状态、图片删除）
            viewModel.updateItem(it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: HouseReportAddEvent) {
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: DetectDirListEvent) {
        //目录列表编辑成功，刷新数据
        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: DetectItemListEvent) {
        //项目列表编辑成功，刷新数据
        viewModel.queryById(intent.getLongExtra(ExtraKeyConfig.DETECT_ID, 0))
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 101) {
            val absolutePath: String = data?.getStringExtra(ExtraKeyConfig.RESULT_IMAGE_PATH) ?: return
            editItemDetect.addOneImage(absolutePath)
            viewModel.updateItem(editItemDetect)
            viewHouseDetect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 从系统相册拾取图片结果
     */
    private val galleryPickResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val srcFile: File? = UriUtils.uri2File(it)
        if (srcFile != null) {
            val copyFile = FileConfig.getDetectImageDir(this, "Item${System.currentTimeMillis()}.png")
            FileUtils.copy(srcFile, copyFile)
            editItemDetect.addOneImage(copyFile.absolutePath)
            viewModel.updateItem(editItemDetect)
            viewHouseDetect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 从系统相机拍照结果
     */
    private val lightPhotoResult = registerForActivityResult(TakePhotoResult()) {
        if (it != null) {
            editItemDetect.addOneImage(it.absolutePath)
            viewModel.updateItem(editItemDetect)
            viewHouseDetect.notifyItemChange(editLayoutIndex)
        }
    }

    /**
     * 项目输入文字结果
     */
    private val textInputResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val inputText: String = it.data?.getStringExtra(ExtraKeyConfig.RESULT_INPUT_TEXT) ?: ""
            if (editItemDetect.inputText != inputText) {//有变化，刷新
                editItemDetect.inputText = inputText
                viewModel.updateItem(editItemDetect)
                viewHouseDetect.notifyItemChange(editLayoutIndex)
            }
        }
    }
}