package com.topdon.module.thermal.ir.fragment

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.bean.GalleryBean
import com.topdon.lib.core.bean.GalleryTitle
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.tools.FileTools.getUri
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.repository.GalleryRepository.DirType
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.R as LibR
import com.topdon.module.thermal.ir.adapter.GalleryAdapter
import com.topdon.lib.core.dialog.ConfirmSelectDialog
import com.topdon.module.thermal.ir.event.GalleryAddEvent
import com.topdon.lib.core.bean.event.GalleryDelEvent
import com.topdon.lib.core.config.FileConfig.getGalleryDirByType
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.event.GalleryDirChangeEvent
import com.topdon.module.thermal.ir.event.GalleryDownloadEvent
import com.topdon.module.thermal.ir.viewmodel.IRGalleryTabViewModel
import com.topdon.module.thermal.ir.viewmodel.IRGalleryViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * 图库
 */
class IRGalleryFragment : BaseFragment() {

    /**
     * 从上一界面传递过来的，进入图库时初始的目录类型
     */
    private var currentDirType = DirType.LINE

    private val viewModel: IRGalleryViewModel by viewModels()

    private val tabViewModel: IRGalleryTabViewModel by activityViewModels()

    private val adapter = GalleryAdapter()
    
    // View references
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var clDownload: View
    private lateinit var clShare: View  
    private lateinit var clDelete: View
    private lateinit var clBottom: View
    private lateinit var irGalleryRecycler: RecyclerView

    /**
     * 从上一界面传递过来的，当前是查看照片还是查看视频.
     */
    private var isVideo = false

    override fun initContentView() = R.layout.fragment_ir_gallery

    override fun initView() {
        // Initialize views with findViewById
        refreshLayout = requireView().findViewById(R.id.refresh_layout)
        clDownload = requireView().findViewById(R.id.cl_download)
        clShare = requireView().findViewById(R.id.cl_share)
        clDelete = requireView().findViewById(R.id.cl_delete)
        clBottom = requireView().findViewById(R.id.cl_bottom)
        irGalleryRecycler = requireView().findViewById(R.id.ir_gallery_recycler)
        
        currentDirType = when (arguments?.getInt(ExtraKeyConfig.DIR_TYPE, 0) ?: 0) {
            DirType.TS004_LOCALE.ordinal -> DirType.TS004_LOCALE
            DirType.TS004_REMOTE.ordinal -> DirType.TS004_REMOTE
            DirType.TC007.ordinal -> DirType.TC007
            else -> DirType.LINE
        }

        clDownload.isVisible = currentDirType == DirType.TS004_REMOTE

        initRecycler()

        clShare.setOnClickListener {
            val selectList = adapter.buildSelectList()
            if (selectList.size == 0) {
                ToastTools.showShort(getString(R.string.tip_least_select))
                return@setOnClickListener
            }
            if (selectList.size > 9) {
                ToastTools.showShort(getString(R.string.Limite_di_9carte))
                return@setOnClickListener
            }
            downloadList(selectList, true)
        }
        clDelete.setOnClickListener {
            showDeleteDialog()
        }
        clDownload.setOnClickListener {
            val selectList = adapter.buildSelectList()
            if (selectList.size == 0) {
                ToastTools.showShort(getString(R.string.tip_least_select))
                return@setOnClickListener
            }
            downloadList(selectList, false)
        }

        viewModel.pageListLD.observe(this) {
            if (it == null) {
                TToast.shortToast(requireContext(), LibR.string.operation_failed_tips)
            }
            refreshLayout.finishRefresh(it != null)
            refreshLayout.finishLoadMore(it != null)
            refreshLayout.setNoMoreData(it != null && it.size < IRGalleryViewModel.PAGE_COUNT)
        }
        viewModel.showListLD.observe(this) {
            adapter.refreshList(it)
        }
        viewModel.deleteResultLD.observe(this) {
            dismissLoadingDialog()
            if (it) {
                TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                tabViewModel.isEditModeLD.value = false
                MediaScannerConnection.scanFile(requireContext(), arrayOf(FileConfig.lineGalleryDir, FileConfig.ts004GalleryDir), null, null)
                EventBus.getDefault().post(GalleryDelEvent())
            } else {
                TToast.shortToast(requireContext(), LibR.string.test_results_delete_failed)
            }
        }
        tabViewModel.isEditModeLD.observe(this) {
            adapter.isEditMode = it
            clBottom.isVisible = it
        }
        tabViewModel.selectAllIndex.observe(this) {
            if ((isVideo && it == 1) || (!isVideo && it == 0)) {
                adapter.selectAll()
            }
        }

        isVideo = arguments?.getBoolean(ExtraKeyConfig.IS_VIDEO) ?: false
    }

    override fun initData() {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun galleryDirChange(event: GalleryDirChangeEvent) {
        currentDirType = event.dirType
        refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun galleryDownload(event: GalleryDownloadEvent) {
        for (i in adapter.dataList.indices) {
            val data = adapter.dataList[i]
            if (data.name == event.filename) {
                data.hasDownload = true
                adapter.notifyItemChanged(i)
                return
            }
        }
        refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun galleryAdd(event: GalleryAddEvent) {
        refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun galleryDel(event: GalleryDelEvent) {
        refresh()
    }

    private fun initRecycler() {
        val spanCount = 3
        val gridLayoutManager = GridLayoutManager(requireActivity(), spanCount)
        //动态设置span
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.dataList[position] is GalleryTitle) spanCount else 1
            }
        }
        irGalleryRecycler.adapter = adapter
        irGalleryRecycler.layoutManager = gridLayoutManager

        adapter.isTS004Remote = currentDirType == DirType.TS004_REMOTE
        adapter.onLongEditListener = {
            tabViewModel.isEditModeLD.value = true
            clBottom.isVisible = true
        }
        adapter.selectCallback = {
            tabViewModel.selectSizeLD.value = it.size
        }
        adapter.itemClickCallback = {
            val galleryBean: GalleryBean = adapter.dataList[it]
            if (galleryBean.name.uppercase().endsWith(".MP4")) {
                NavigationManager.getInstance().build(RouterConfig.IR_VIDEO_GSY)
                    .withBoolean("isRemote", currentDirType == DirType.TS004_REMOTE)
                    .withParcelable("data", adapter.dataList[it])
                    .navigation(requireActivity())
            } else {
                val sourceList: ArrayList<GalleryBean> = viewModel.sourceListLD.value ?: ArrayList()
                var position = if (it >= sourceList.size) sourceList.size - 1 else it
                for (i in position downTo 0) {
                    if (sourceList[i].path == galleryBean.path) {
                        position = i
                        break
                    }
                }


                if (currentDirType == DirType.LINE || currentDirType == DirType.TC007) {
                    NavigationManager.getInstance().build(RouterConfig.IR_GALLERY_DETAIL_01)
                        .withBoolean(ExtraKeyConfig.IS_TC007, currentDirType == DirType.TC007)
                        .withInt("position", position)
                        .withParcelableArrayList("list", sourceList)
                        .navigation(requireActivity())
                } else {
                    NavigationManager.getInstance().build(RouterConfig.IR_GALLERY_DETAIL_04)
                        .withBoolean("isRemote", currentDirType == DirType.TS004_REMOTE)
                        .withInt("position", position)
                        .withParcelableArrayList("list", sourceList)
                        .navigation(requireActivity())
                }
            }
        }


        refreshLayout.setOnRefreshListener {
            refresh()
        }
        refreshLayout.setOnLoadMoreListener {
            viewModel.queryGalleryByPage(isVideo, currentDirType)
        }
        refreshLayout.setEnableScrollContentWhenLoaded(false)

        refreshLayout.autoRefresh()
    }

    private fun refresh() {
        refreshLayout.setEnableLoadMore(true)
        viewModel.hasLoadPage = 0
        viewModel.queryGalleryByPage(isVideo, currentDirType)
    }

    private fun showDeleteDialog() {
        val deleteList = adapter.buildSelectList()

        var hasOneDownload = false
        if (currentDirType == DirType.TS004_REMOTE) {
            for (data in deleteList) {
                if (data.hasDownload) {
                    hasOneDownload = true
                    break
                }
            }
        }

        if (deleteList.size > 0) {
            ConfirmSelectDialog(requireContext()).run {
                setTitleStr(getString(
                    R.string.tip_delete_chosen,
                    deleteList.size
                ))
                setMessageRes(R.string.also_del_from_phone_album)
                setShowMessage(currentDirType == DirType.TS004_REMOTE && hasOneDownload)
                onConfirmClickListener = {
                    showLoadingDialog()
                    viewModel.delete(deleteList, currentDirType, it)
                }
                show()
            }
        } else {
            ToastTools.showShort(getString(R.string.tip_least_select))
        }
    }

    private fun downloadList(downloadList: List<GalleryBean>, isShare: Boolean) {
        val downloadMap = HashMap<String, File>()
        downloadList.forEach {
            if (!it.hasDownload) {
                downloadMap[it.path] = File(FileConfig.ts004GalleryDir, it.name)
            }
        }

        if (downloadMap.isEmpty()) {
            if (isShare) {
                shareImage(downloadList)
            } else {
                ToastTools.showShort(R.string.ts004_download_complete)
            }
            tabViewModel.isEditModeLD.value = false
        } else {
            lifecycleScope.launch {
                (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                showLoadingDialog()
                val successCount = TS004Repository.downloadList(downloadMap) { path, isSuccess ->
                    if (isSuccess) {
                        for (galleryBean in downloadList) {
                            if (galleryBean.path == path) {
                                galleryBean.hasDownload = true
                                adapter.notifyDataSetChanged()
                                break
                            }
                        }
                    }
                }
                if (successCount == downloadMap.size) {//全都下载成功
                    dismissLoadingDialog()
                    if (isShare) {
                        shareImage(downloadList)
                    } else {
                        ToastTools.showShort(R.string.ts004_download_complete)
                    }
                    tabViewModel.isEditModeLD.value = false
                } else {
                    dismissLoadingDialog()
                    ToastTools.showShort(LibR.string.liveData_save_error)
                }
                MediaScannerConnection.scanFile(requireContext(), arrayOf(FileConfig.lineGalleryDir, FileConfig.ts004GalleryDir), null, null)
                (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun shareImage(shareList: List<GalleryBean>) {
        val shareIntent = Intent()
        if (shareList.size == 1) {
            if (shareList[0].name.uppercase().endsWith(".MP4")) {
                shareIntent.type = "video/*"
            } else {
                shareIntent.type = "image/*"
            }
            shareIntent.action = Intent.ACTION_SEND
            val uri = getUri(File(getGalleryDirByType(currentDirType), shareList[0].name))
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        } else {
            val imageUris = ArrayList<Uri>()
            shareIntent.type = "video/*"
            for (bean in shareList) {
                imageUris.add(getUri(File(getGalleryDirByType(currentDirType), bean.name)))
            }
            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUris)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.battery_share)))
    }
}

