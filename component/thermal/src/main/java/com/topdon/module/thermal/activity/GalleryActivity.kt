package com.topdon.module.thermal.activity

import android.Manifest
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.PermissionTool
import com.topdon.module.thermal.R
import com.topdon.module.thermal.fragment.GalleryPictureFragment
import com.topdon.module.thermal.fragment.GalleryVideoFragment


// Legacy ARouter route annotation - now using NavigationManager
class GalleryActivity : BaseActivity() {

//    override fun providerVMClass() = GalleryViewModel::class.java

    private val permissionList by lazy {
        if (this.applicationInfo.targetSdkVersion >= 34){
            listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (this.applicationInfo.targetSdkVersion >= 33){
            mutableListOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun initContentView() = R.layout.activity_gallery

    override fun initView() {
        // Use findViewById instead of synthetic views for Kotlin 2.1.0 compatibility
        val galleryViewPager = findViewById<ViewPager>(R.id.gallery_viewpager)
        val galleryTab = findViewById<TabLayout>(R.id.gallery_tab)
        
        galleryViewPager.adapter = ViewAdapter(this, supportFragmentManager)
        galleryTab.setupWithViewPager(galleryViewPager)

        // Request media permissions using modern PermissionTool
        PermissionTool.requestFile(this) {
            // Permission granted, gallery can now access media files
        }
    }

    override fun initData() {
    }

    inner class ViewAdapter : FragmentStatePagerAdapter {
        private var titles: Array<String> = arrayOf()

        constructor (context: Context, fm: FragmentManager) : super(
            fm,
            BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            titles = arrayOf("图片", "视频")
        }

        override fun getCount(): Int {
            return titles.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> GalleryPictureFragment()
                else -> GalleryVideoFragment()
            }
        }
    }

}