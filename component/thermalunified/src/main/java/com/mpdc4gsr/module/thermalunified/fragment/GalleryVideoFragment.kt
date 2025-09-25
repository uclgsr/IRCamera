package com.mpdc4gsr.module.thermalunified.fragment

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ScreenUtils
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelFragment
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.adapter.GalleryAdapter
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryViewModel

class GalleryVideoFragment : BaseViewModelFragment<GalleryViewModel>() {
    private val adapter by lazy { GalleryAdapter(requireContext()) }

    override fun providerVMClass() = GalleryViewModel::class.java

    override fun initContentView() = R.layout.fragment_gallery_video

    override fun initView() {
        val span = if (ScreenUtils.isLandscape()) 6 else 3
        val galleryVideoRecycler =
            requireView().findViewById<RecyclerView>(R.id.gallery_video_recycler)
        galleryVideoRecycler.layoutManager = GridLayoutManager(requireContext(), span)
        galleryVideoRecycler.adapter = adapter

        viewModel.galleryLiveData.observe(this) {
            adapter.datas = it
        }
        adapter.listener =
            object : GalleryAdapter.OnItemClickListener {
                override fun onClick(
                    index: Int,
                    path: String,
                ) {
                    openVideo(path)
                }

                override fun onLongClick(
                    index: Int,
                    path: String,
                ) {
                    TipDialog.Builder(requireContext()).setMessage("导出图片")
                        .setPositiveListener("分享") {

                        }
                        .create().show()
                }
            }
    }

    override fun initData() {
    }

    override fun onStart() {
        super.onStart()
        viewModel.getVideoData()
    }


    fun openVideo(path: String) {
        NavigationManager.getInstance().build(RouterConfig.VIDEO).withString("video_path", path)
            .navigation(requireContext())
    }
}
