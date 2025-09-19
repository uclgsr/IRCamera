package com.mpdc4gsr.module.thermal.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ScreenUtils
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseViewModelFragment
import com.mpdc4gsr.module.thermal.R
import com.mpdc4gsr.module.thermal.adapter.GalleryAdapter
import com.mpdc4gsr.module.thermal.tools.GlideImageEngine
import com.mpdc4gsr.module.thermal.viewmodel.GalleryViewModel
import java.io.File

class GalleryPictureFragment : BaseViewModelFragment<GalleryViewModel>() {
    private val adapter by lazy { GalleryAdapter(requireContext()) }

    override fun providerVMClass() = GalleryViewModel::class.java

    override fun initContentView() = R.layout.fragment_gallery_picture

    override fun initView() {
        val span = if (ScreenUtils.isLandscape()) 6 else 3
        val galleryRecycler = requireView().findViewById<RecyclerView>(R.id.gallery_recycler)
        galleryRecycler.layoutManager = GridLayoutManager(requireContext(), span)
        galleryRecycler.adapter = adapter

        viewModel.galleryLiveData.observe(this) {
            adapter.datas = it
        }
        adapter.listener =
            object : GalleryAdapter.OnItemClickListener {
                override fun onClick(
                    index: Int,
                    path: String,
                ) {
                    previewPicture(path)
                }

                override fun onLongClick(
                    index: Int,
                    path: String,
                ) {
                    TipDialog.Builder(requireContext()).setMessage("导出图片")
                        .setPositiveListener("分享") {
                            share(path)
                        }
                        .create().show()
                }
            }
    }

    override fun initData() {
    }

    override fun onStart() {
        super.onStart()
        viewModel.getData()
    }

    fun share(path: String) {
        val file = File(path)
        var intent = Intent()
        intent.action = Intent.ACTION_SEND 
        intent.type = "image/*" 
        val uri: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val authority = "${requireContext().packageName}.fileprovider"
                FileProvider.getUriForFile(requireContext(), authority, file)
            } else {
                Uri.fromFile(file)
            }
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent = Intent.createChooser(intent, "分享图片")
        startActivity(intent)
    }

    fun previewPicture(path: String) {
        val imageEngine = GlideImageEngine()

        
    }
}
