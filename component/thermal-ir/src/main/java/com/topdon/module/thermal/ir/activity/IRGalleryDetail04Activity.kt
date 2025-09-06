package com.topdon.module.thermal.ir.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.R as LibR
import com.topdon.lib.ui.R as UiR
import com.topdon.lib.core.dialog.ConfirmSelectDialog
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.event.GalleryDownloadEvent
import com.topdon.module.thermal.ir.fragment.GalleryFragment
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * TS004 图片详情
 */
// Legacy ARouter route annotation - now using NavigationManager
class IRGalleryDetail04Activity : BaseActivity() {

    /**
     * 是否查看远端数据.
     * true-远端数据 false-手机本地数据
     */
    private var isRemote = false
    /**
     * 当前展示图片在列表中的 position
     */
    private var position = 0
    /**
     * 从上一界面传递过来的，当前展示的图片列表.
     */
    private lateinit var dataList: ArrayList<GalleryBean>

    override fun initContentView() = R.layout.activity_ir_gallery_detail_04

    @SuppressLint("SetTextI18n")
    override fun initView() {
        isRemote = intent.getBooleanExtra("isRemote", false)
        position = intent.getIntExtra("position", 0)
        dataList = intent.getParcelableArrayListExtra("list")!!

        val titleView = findViewById<com.topdon.lib.core.view.TitleView>(R.id.title_view)
        titleView.setTitleText("${position + 1}/${dataList.size}")

        findViewById<ConstraintLayout>(R.id.cl_bottom).isVisible = isRemote //查看远端时底部才有3个按钮

        if (!isRemote) {
            titleView.setRightDrawable(UiR.drawable.ic_toolbar_info_svg)
            titleView.setRight2Drawable(UiR.drawable.ic_toolbar_share_svg)
            titleView.setRight3Drawable(UiR.drawable.ic_toolbar_delete_svg)
            titleView.setRightClickListener { actionInfo() }
            titleView.setRight2ClickListener { actionShare() }
            titleView.setRight3ClickListener { actionDelete() }
        }

        initViewPager()

        findViewById<ConstraintLayout>(R.id.cl_download).setOnClickListener {
            actionDownload(false)
        }
        findViewById<ConstraintLayout>(R.id.cl_share).setOnClickListener {
            if (dataList[position].hasDownload) {
                actionShare()
            } else {
                actionDownload(true)
            }
        }
        findViewById<ConstraintLayout>(R.id.cl_delete).setOnClickListener {
            actionDelete()
        }
    }

    override fun initData() {

    }

    @SuppressLint("SetTextI18n")
    private fun initViewPager() {
        val irGalleryViewpager = findViewById<ViewPager2>(R.id.ir_gallery_viewpager)
        irGalleryViewpager.adapter = GalleryViewPagerAdapter(this)
        irGalleryViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                this@IRGalleryDetail04Activity.position = position
                findViewById<com.topdon.lib.core.view.TitleView>(R.id.title_view).setTitleText("${position + 1}/${dataList.size}")
                findViewById<ImageView>(R.id.iv_download).isSelected = dataList[position].hasDownload
            }
        })
        irGalleryViewpager?.setCurrentItem(position, false)
    }

    private fun actionInfo() {
        try {
            val data = dataList[position]
            val exif = ExifInterface(data.path)
            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val whStr = "${width}x${length}"
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
        val uri = FileTools.getUri(File(FileConfig.ts004GalleryDir, data.name))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "image/jpeg"
        startActivity(Intent.createChooser(shareIntent, getString(LibR.string.battery_share)))
    }

    private fun actionDelete() {
        ConfirmSelectDialog(this).run {
            setTitleRes(LibR.string.tip_delete)
            setMessageRes(LibR.string.also_del_from_phone_album)
            setShowMessage(isRemote && dataList[position].hasDownload)
            onConfirmClickListener = {
                deleteFile(it)
            }
            show()
        }
    }

    private fun deleteFile(isDelLocal: Boolean) {
        val data = dataList[position]
        if (isRemote) {
            lifecycleScope.launch {
                showCameraLoading()

                val isSuccess = TS004Repository.deleteFiles(arrayOf(data.id))
                if (isSuccess) {
                    if (isDelLocal) {
                        File(FileConfig.ts004GalleryDir, data.name).delete()
                        MediaScannerConnection.scanFile(this@IRGalleryDetail04Activity, arrayOf(FileConfig.ts004GalleryDir), null, null)
                    }

                    dismissCameraLoading()
                    ToastTools.showShort(LibR.string.test_results_delete_success)
                    EventBus.getDefault().post(GalleryDelEvent())
                    if (dataList.size == 1) {
                        finish()
                    } else {
                        dataList.removeAt(position)
                        if (position >= dataList.size) {
                            position = dataList.size - 1
                        }
                        initViewPager()
                    }
                } else {
                    dismissCameraLoading()
                    TToast.shortToast(this@IRGalleryDetail04Activity, LibR.string.test_results_delete_failed)
                }
            }
        } else {
            File(data.path).delete()
            MediaScannerConnection.scanFile(this, arrayOf(FileConfig.ts004GalleryDir), null, null)
            EventBus.getDefault().post(GalleryDelEvent())
            if (dataList.size == 1) {
                finish()
            } else {
                dataList.removeAt(position)
                if (position >= dataList.size) {
                    position = dataList.size - 1
                }
                initViewPager()
            }
        }
    }


    private fun actionDownload(isToShare: Boolean) {
        val data = dataList[position]
        if (data.hasDownload) {
            if (isToShare) {
                actionShare()
            }
            return
        }
        showCameraLoading()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Glide.with(this).downloadOnly().load(data.path).addListener(object : RequestListener<File> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
                    dismissCameraLoading()
                    ToastTools.showShort(LibR.string.liveData_save_error)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    return false
                }

                override fun onResourceReady(
                    resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    EventBus.getDefault().post(GalleryDownloadEvent(data.name))
                    dismissCameraLoading()
                    FileUtils.copy(resource, File(FileConfig.ts004GalleryDir, data.name))
                    MediaScannerConnection.scanFile(this@IRGalleryDetail04Activity, arrayOf(FileConfig.ts004GalleryDir), null, null)
                    ToastTools.showShort(LibR.string.tip_save_success)
                    data.hasDownload = true
                    findViewById<ImageView>(R.id.iv_download).isSelected = dataList[position].hasDownload
                    if (isToShare) {
                        actionShare()
                    }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    return false
                }
            }).preload()
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
}

