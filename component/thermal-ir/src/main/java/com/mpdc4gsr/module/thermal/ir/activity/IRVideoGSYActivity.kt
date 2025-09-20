package com.mpdc4gsr.module.thermal.ir.activity


import android.content.Intent
import android.media.MediaScannerConnection
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.BarUtils
import com.mpdc4gsr.lib.core.bean.GalleryBean
import com.mpdc4gsr.lib.core.bean.event.GalleryDelEvent
import com.mpdc4gsr.lib.core.config.FileConfig
import com.mpdc4gsr.lib.core.dialog.ConfirmSelectDialog
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.repository.TS004Repository
import com.mpdc4gsr.lib.core.tools.FileTools
import com.mpdc4gsr.lib.core.tools.TimeTool
import com.mpdc4gsr.lib.core.tools.ToastTools
import com.mpdc4gsr.lib.core.lms.weiget.TToast
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.event.GalleryDownloadEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import com.mpdc4gsr.lib.core.R as LibR
import com.mpdc4gsr.lib.ui.R as UiR


class IRVideoGSYActivity : BaseActivity() {
    private var isRemote = false
    private lateinit var data: GalleryBean

    private lateinit var titleView: com.mpdc4gsr.lib.core.view.TitleView
    private lateinit var clBottom: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var clDownload: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var clShare: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var clDelete: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var ivDownload: android.widget.ImageView


    override fun initContentView() = R.layout.activity_ir_video_gsy

    override fun initView() {

        titleView = findViewById(R.id.title_view)
        clBottom = findViewById(R.id.cl_bottom)
        clDownload = findViewById(R.id.cl_download)
        clShare = findViewById(R.id.cl_share)
        clDelete = findViewById(R.id.cl_delete)
        ivDownload = findViewById(R.id.iv_download)


        BarUtils.setNavBarColor(this, ContextCompat.getColor(this, UiR.color.black))

        isRemote = intent.getBooleanExtra("isRemote", false)
        data = intent.getParcelableExtra("data") ?: throw NullPointerException("传递 data")

        clBottom.isVisible = isRemote

        if (!isRemote) {
            titleView.setRightDrawable(UiR.drawable.ic_toolbar_info_svg)
            titleView.setRight2Drawable(UiR.drawable.ic_toolbar_share_svg)
            titleView.setRight3Drawable(UiR.drawable.ic_toolbar_delete_svg)
            titleView.setRightClickListener { actionInfo() }
            titleView.setRight2ClickListener { actionShare() }
            titleView.setRight3ClickListener { showDeleteDialog() }
        }

        clDownload.setOnClickListener {
            actionDownload(false)
        }
        clShare.setOnClickListener {
            if (data.hasDownload) {
                actionShare()
            } else {
                actionDownload(true)
            }
        }
        clDelete.setOnClickListener {
            showDeleteDialog()
        }

        ivDownload.isSelected = data.hasDownload
        ivDownload.setImageResource(if (isRemote) R.drawable.selector_download else UiR.drawable.ic_toolbar_info_svg)

        previewVideo(isRemote, data.path)
    }

    override fun initData() {
    }

    private fun previewVideo(
        isRemote: Boolean,
        path: String,
    ) {


    }

    private fun actionDownload(isToShare: Boolean) {
        if (data.hasDownload) {
            if (isToShare) {
                actionShare()
            }
            return
        }
        lifecycleScope.launch {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            showCameraLoading()
            val isSuccess =
                TS004Repository.download(data.path, File(FileConfig.ts004GalleryDir, data.name))
            MediaScannerConnection.scanFile(
                this@IRVideoGSYActivity,
                arrayOf(FileConfig.ts004GalleryDir),
                null,
                null
            )
            dismissCameraLoading()
            if (isSuccess) {
                ToastTools.showShort(R.string.tip_save_success)
                EventBus.getDefault().post(GalleryDownloadEvent(data.name))
                data.hasDownload = true
                ivDownload.isSelected = true
                if (isToShare) {
                    actionShare()
                }
            } else {
                ToastTools.showShort(LibR.string.liveData_save_error)
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun actionInfo() {
        val sizeStr = FileTools.getFileSize(data.path)
        val str = StringBuilder()
        str.append(getString(R.string.detail_date)).append("\n")
        str.append(TimeTool.showDateType(data.timeMillis)).append("\n\n")
        str.append(getString(R.string.detail_info)).append("\n")

        str.append("${getString(R.string.detail_len)}: ").append(sizeStr).append("\n")
        str.append("${getString(R.string.detail_path)}: ").append(data.path).append("\n")
        TipDialog.Builder(this)
            .setMessage(str.toString())
            .setCanceled(true)
            .create().show()
    }

    private fun actionShare() {
        val uri = FileTools.getUri(File(data.path))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "video/*"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }

    private fun showDeleteDialog() {
        ConfirmSelectDialog(this).run {
            setTitleRes(R.string.tip_delete)
            setMessageRes(R.string.also_del_from_phone_album)
            setShowMessage(isRemote && data.hasDownload)
            onConfirmClickListener = {
                deleteFile(it)
            }
            show()
        }
    }

    private fun deleteFile(isDelLocal: Boolean) {
        if (isRemote) {
            lifecycleScope.launch {
                showCameraLoading()
                val isSuccess = TS004Repository.deleteFiles(arrayOf(data.id))
                if (isSuccess) {
                    if (isDelLocal) {
                        File(FileConfig.ts004GalleryDir, data.name).delete()
                        MediaScannerConnection.scanFile(
                            this@IRVideoGSYActivity,
                            arrayOf(FileConfig.ts004GalleryDir),
                            null,
                            null
                        )
                    }
                    dismissCameraLoading()
                    ToastTools.showShort(R.string.test_results_delete_success)
                    EventBus.getDefault().post(GalleryDelEvent())
                    finish()
                } else {
                    dismissCameraLoading()
                    TToast.shortToast(
                        this@IRVideoGSYActivity,
                        LibR.string.test_results_delete_failed
                    )
                }
            }
        } else {
            EventBus.getDefault().post(GalleryDelEvent())
            File(data.path).delete()
            MediaScannerConnection.scanFile(this, arrayOf(FileConfig.ts004GalleryDir), null, null)
            finish()
        }
    }

    override fun onResume() {

        super.onResume()
    }

    override fun onPause() {

        super.onPause()
    }


}
