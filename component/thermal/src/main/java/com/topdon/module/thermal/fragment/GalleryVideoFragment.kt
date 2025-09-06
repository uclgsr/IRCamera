package com.topdon.module.thermal.fragment

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.ScreenUtils
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelFragment
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.module.thermal.R
import com.topdon.module.thermal.adapter.GalleryAdapter
import com.topdon.module.thermal.viewmodel.GalleryViewModel

/**
 * 图片
 */
class GalleryVideoFragment : BaseViewModelFragment<GalleryViewModel>() {
    private val adapter by lazy { GalleryAdapter(requireContext()) }

    override fun providerVMClass() = GalleryViewModel::class.java

    override fun initContentView() = R.layout.fragment_gallery_video

    override fun initView() {
        val span = if (ScreenUtils.isLandscape()) 6 else 3
        val galleryVideoRecycler = requireView().findViewById<RecyclerView>(R.id.gallery_video_recycler)
        galleryVideoRecycler.layoutManager = GridLayoutManager(requireContext(), span)
        galleryVideoRecycler.adapter = adapter

        viewModel.galleryLiveData.observe(this) {
            adapter.datas = it
        }
        adapter.listener = object : GalleryAdapter.OnItemClickListener {
            override fun onClick(index: Int, path: String) {
                openVideo(path)
            }

            override fun onLongClick(index: Int, path: String) {
                TipDialog.Builder(requireContext()).setMessage("导出图片")
                    .setPositiveListener("分享") {
//                            share(path)
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


//    fun previewVideo(path: String) {
//        val imageEngine = GlideImageEngine()
//        MNImageBrowser.with(context)
//            .setCurrentPosition(0)
//            .setImageEngine(imageEngine)
//            .setImageUrl(path)
//            .show()
//    }


    fun openVideo(path: String) {
        NavigationManager.getInstance().build(RouterConfig.VIDEO).withString("video_path", path)
            .navigation(requireContext())
    }

}