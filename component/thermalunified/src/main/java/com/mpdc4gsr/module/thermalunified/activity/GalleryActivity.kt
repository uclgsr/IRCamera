@file:Suppress("DEPRECATION")

package com.mpdc4gsr.module.thermalunified.activity

import android.Manifest
import android.content.Context
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import com.mpdc4gsr.libunified.app.tools.PermissionTool
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.fragment.GalleryPictureFragment
import com.mpdc4gsr.module.thermalunified.fragment.GalleryVideoFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryActivityViewModel


class GalleryActivity : BaseViewModelActivity<GalleryActivityViewModel>() {

    override fun providerVMClass(): Class<GalleryActivityViewModel> = GalleryActivityViewModel::class.java

    override fun initContentView() = R.layout.activity_gallery

    override fun initView() {
        setupObservers()
        viewModel.initializePermissions(applicationInfo.targetSdkVersion)
    }

    private fun setupObservers() {
        viewModel.permissionState.observe(this) { permissionState ->
            if (!permissionState.hasAllPermissions) {
                requestPermissions(permissionState.missingPermissions)
            } else {
                setupViewPager()
            }
        }

        viewModel.viewPagerState.observe(this) { state ->
            when (state) {
                is GalleryActivityViewModel.ViewPagerState.Ready -> {
                    setupViewPager()
                }
                is GalleryActivityViewModel.ViewPagerState.TabSelected -> {
                    selectTab(state.position)
                }
            }
        }
    }

    private fun requestPermissions(permissions: List<String>) {
        PermissionTool.with(this)
            .setPermissions(permissions)
            .setCallBack { isSuccess, _, _ ->
                viewModel.onPermissionsResult(isSuccess)
            }
            .start()
    }

    private fun setupViewPager() {
        val galleryViewPager = findViewById<ViewPager>(R.id.gallery_viewpager)
        val galleryTab = findViewById<TabLayout>(R.id.gallery_tab)

        galleryViewPager.adapter = ViewAdapter(this, supportFragmentManager)
        galleryTab.setupWithViewPager(galleryViewPager)
    }

    private fun selectTab(position: Int) {
        val galleryTab = findViewById<TabLayout>(R.id.gallery_tab)
        galleryTab.getTabAt(position)?.select()
    }

    override fun initData() {
        // Data initialization handled by ViewModel
    }

    class ViewAdapter(private val context: Context, fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val titles = arrayOf(
            context.getString(R.string.tab_picture),
            context.getString(R.string.tab_video)
        )

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> GalleryPictureFragment()
                1 -> GalleryVideoFragment()
                else -> GalleryPictureFragment()
            }
        }

        override fun getCount(): Int = titles.size

        override fun getPageTitle(position: Int): CharSequence? = titles[position]
    }
}
