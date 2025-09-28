package com.mpdc4gsr.module.thermalunified.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.bean.event.GalleryDelEvent
import com.mpdc4gsr.libunified.app.comm.ExcelUtil
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.dialog.TipProgressDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.tools.FileTools
import com.mpdc4gsr.libunified.app.tools.TimeTool
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.libunified.app.utils.ByteUtils.bytesToInt
import com.mpdc4gsr.libunified.app.utils.Constants.IS_REPORT_FIRST
import com.mpdc4gsr.libunified.app.utils.UnifiedByteUtils.bytesToInt
import com.mpdc4gsr.libunified.app.view.TitleView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.event.ImageGalleryEvent
import com.mpdc4gsr.module.thermalunified.fragment.GalleryFragment
import com.mpdc4gsr.module.thermalunified.frame.FrameTool
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import com.mpdc4gsr.libunified.R as LibR


class IRGalleryDetail01Activity : BaseActivity(), View.OnClickListener {

    private var isTC007 = false

    private var position = 0

    private lateinit var dataList: ArrayList<GalleryBean>

    private var irPath: String? = null
    private val irViewModel: IRGalleryEditViewModel by viewModels()

    override fun initContentView() = R.layout.activity_ir_gallery_detail_01

    private val frameTool by lazy { FrameTool() }

    override fun initView() {
        position = intent.getIntExtra("position", 0)
        dataList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("list", GalleryBean::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<GalleryBean>("list")!!
        }
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        val titleView = findViewById<TitleView>(R.id.title_view)
        titleView.setTitleText("${position + 1}/${dataList.size}")
        titleView.setRightClickListener { actionInfo() }
        titleView.setRight2ClickListener { actionShare() }
        titleView.setRight3ClickListener { deleteImage() }

        initViewPager()

        findViewById<LinearLayout>(R.id.ll_ir_edit_2D)?.setOnClickListener(this)
        findViewById<LinearLayout>(R.id.ll_ir_edit_3D)?.setOnClickListener(this)
        findViewById<LinearLayout>(R.id.ll_ir_report)?.setOnClickListener(this)
        findViewById<LinearLayout>(R.id.ll_ir_ex)?.setOnClickListener(this)

        irViewModel.resultLiveData.observe(this) {
            lifecycleScope.launch {
                val filePath: String?
                withContext(Dispatchers.IO) {
                    frameTool.read(it.frame)
                    filePath =
                        ExcelUtil.exportExcel(
                            excelName,
                            192,
                            256,
                            frameTool.getRotate90Temp(frameTool.temperatureBytes),
                        ) { current, total ->
                            lifecycleScope.launch(Dispatchers.Main) {
                            }
                        }
                }
                progressDialog?.dismiss()
                if (filePath.isNullOrEmpty()) {
                    ToastTools.showShort(LibR.string.liveData_save_error)
                } else {
                    val uri = FileTools.getUri(File(filePath))
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "application/xlsx"
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            getString(LibR.string.battery_share)
                        )
                    )
                }
            }
        }
    }

    override fun initData() {
    }

    @SuppressLint("SetTextI18n")
    private fun initViewPager() {
        val irGalleryViewpager = findViewById<ViewPager2>(R.id.ir_gallery_viewpager)
        irGalleryViewpager.adapter = GalleryViewPagerAdapter(this)
        irGalleryViewpager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    this@IRGalleryDetail01Activity.position = position
                    findViewById<TitleView>(R.id.title_view).setTitleText("${position + 1}/${dataList.size}")

                    irPath = "${FileConfig.lineIrGalleryDir}/${
                        dataList[position].name.substringBeforeLast(".")
                    }.ir"
                    val hasIrData = File(irPath!!).exists()
                    findViewById<LinearLayout>(R.id.ll_ir_edit_3D)?.isVisible = hasIrData
                    findViewById<LinearLayout>(R.id.ll_ir_report)?.isVisible = hasIrData
                    findViewById<LinearLayout>(R.id.ll_ir_edit_2D)?.isVisible = hasIrData
                    findViewById<LinearLayout>(R.id.ll_ir_ex)?.isVisible = hasIrData
                }
            },
        )
        irGalleryViewpager?.setCurrentItem(position, false)
    }

    private fun actionInfo() {
        try {
            val data = dataList[position]
            val exif = ExifInterface(data.path)
            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val whStr = "${width}x$length"
            val sizeStr = FileTools.getFileSize(data.path)

            val str = StringBuilder()
            str.append(getString(LibR.string.detail_date)).append("\n")
            str.append(TimeTool.showDateType(data.timeMillis)).append("\n\n")
            str.append(getString(LibR.string.detail_info)).append("\n")
            str.append("${getString(LibR.string.detail_size)}: ").append(whStr).append("\n")
            str.append("${getString(LibR.string.detail_len)}: ").append(sizeStr).append("\n")
            str.append("${getString(LibR.string.detail_path)}: ").append(data.path).append("\n")
            TipDialog.Builder(this).setMessage(str.toString()).setCanceled(true).create().show()
        } catch (e: Exception) {
            ToastTools.showShort(LibR.string.status_error_load_fail)
        }
    }

    private fun actionShare() {
        val data = dataList[position]
        val uri = FileTools.getUri(File(data.path))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "image/jpeg"
        startActivity(Intent.createChooser(shareIntent, getString(LibR.string.battery_share)))
    }

    private fun deleteImage() {
        TipDialog.Builder(this)
            .setMessage(getString(LibR.string.tip_delete))
            .setPositiveListener(LibR.string.app_confirm) {
                val data = dataList[position]
                if (dataList.size == 1) {
                    File(data.path).delete()
                    finish()
                } else {
                    File(data.path).delete()
                    dataList.removeAt(position)
                    if (position >= dataList.size) {
                        position = dataList.size - 1
                    }
                    initViewPager()
                }
                EventBus.getDefault().post(GalleryDelEvent())
            }
            .setCancelListener(LibR.string.app_cancel)
            .create()
            .show()
    }

    private var progressDialog: TipProgressDialog? = null
    private var excelName: String = ""

    private fun actionExcel() {
        if (progressDialog == null) {
            progressDialog = TipProgressDialog.Builder(this).setCanceleable(false).create()
        }
        progressDialog?.show()

        excelName = dataList[position].name.substringBeforeLast(".")
        val irPath = "${FileConfig.lineIrGalleryDir}/$excelName.ir"
        if (!File(irPath).exists()) {
            ToastTools.showShort(getString(LibR.string.album_report_on_edit))
            progressDialog?.dismiss()
            return
        }
        irViewModel.initData(irPath)
    }

    override fun onClick(v: View?) {
        when (v) {
            findViewById<LinearLayout>(R.id.ll_ir_edit_2D) -> {

                actionEditOrReport(false)
            }

            findViewById<LinearLayout>(R.id.ll_ir_edit_3D) -> {

                val data = dataList[position]
                val fileName = data.name.substringBeforeLast(".")
                val irPath = "${FileConfig.lineIrGalleryDir}/$fileName.ir"
                if (!File(irPath).exists()) {
                    ToastTools.showShort(LibR.string.album_report_on_edit)
                    return
                }
                var tempHigh = 0f
                var tempLow = 0f
                lifecycleScope.launch {

                    withContext(Dispatchers.IO) {
                        val file = File(irPath)
                        if (!file.exists()) {
                            XLog.w("IR[ph][ph][ph][ph][ph]: ${file.absolutePath}")
                            return@withContext
                        }
                        XLog.w("IR[ph][ph]: ${file.absolutePath}")
                        val bytes = file.readBytes()
                        val headLenBytes = ByteArray(2)
                        System.arraycopy(bytes, 0, headLenBytes, 0, 2)
                        val headLen = headLenBytes.bytesToInt()
                        val headDataBytes = ByteArray(headLen)
                        val frameDataBytes = ByteArray(bytes.size - headLen)
                        System.arraycopy(bytes, 0, headDataBytes, 0, headDataBytes.size)
                        System.arraycopy(bytes, headLen, frameDataBytes, 0, frameDataBytes.size)
                        frameTool.read(frameDataBytes)
                        tempHigh = frameTool.getSrcTemp().maxTemperature
                        tempLow = frameTool.getSrcTemp().minTemperature
                    }

                    NavigationManager.getInstance().build(RouterConfig.IR_GALLERY_3D)
                        .withString(ExtraKeyConfig.IR_PATH, irPath)
                        .withFloat(ExtraKeyConfig.TEMP_HIGH, tempHigh)
                        .withFloat(ExtraKeyConfig.TEMP_LOW, tempLow)
                        .navigation(this@IRGalleryDetail01Activity)
                }
            }

            findViewById<LinearLayout>(R.id.ll_ir_report) -> {

                actionEditOrReport(true)
            }

            findViewById<LinearLayout>(R.id.ll_ir_ex) -> {
                TipDialog.Builder(this).setMessage(LibR.string.tip_album_temp_exportfile)
                    .setPositiveListener(LibR.string.app_confirm) {
                        actionExcel()
                    }.setCancelListener(LibR.string.app_cancel) {}.setCanceled(true).create().show()
            }
        }
    }

    private fun actionEditOrReport(isReport: Boolean) {
        val data = dataList[position]
        val fileName = data.name.substringBeforeLast(".")
        val irPath = "${FileConfig.lineIrGalleryDir}/$fileName.ir"
        if (!File(irPath).exists()) {
            ToastTools.showShort(LibR.string.album_report_on_edit)
            return
        }
        NavigationManager.getInstance().build(RouterConfig.IR_GALLERY_EDIT)
            .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
            .withBoolean(ExtraKeyConfig.IS_PICK_REPORT_IMG, isReport)
            .withBoolean(IS_REPORT_FIRST, true)
            .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, irPath)
            .navigation(this)
    }

    inner class GalleryViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = GalleryFragment()
            val bundle = Bundle()
            bundle.putString("path", dataList[position].path)
            fragment.arguments = bundle
            return fragment
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSaveFinishBean(imageGalleryEvent: ImageGalleryEvent) {
        finish()
    }
}
