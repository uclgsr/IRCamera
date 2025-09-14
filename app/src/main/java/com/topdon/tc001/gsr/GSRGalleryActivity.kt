package com.topdon.tc001.gsr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrGalleryBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.tools.PermissionTool


class GSRGalleryActivity : BaseBindingActivity<ActivityGsrGalleryBinding>() {
    companion object {
    private const val TAG = "GSRGalleryActivity"

    fun startActivity(context: Context) {
    context.startActivity(Intent(context, GSRGalleryActivity::class.java))
    }
    }

    override fun initContentLayoutId() = R.layout.activity_gsr_gallery

    private val permissionList by lazy {
        if (applicationContext.applicationInfo.targetSdkVersion >= 34) {
            listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (applicationContext.applicationInfo.targetSdkVersion >= 33) {
            mutableListOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initView()
    requestPermissions()
    }

    private fun initView() {
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.title = "GSR Recording Gallery"

    binding.gsrGalleryViewpager.adapter = ViewAdapter(this, supportFragmentManager)
    binding.gsrGalleryTab.setupWithViewPager(binding.gsrGalleryViewpager)
    }

    private fun requestPermissions() {
    PermissionTool.requestFile(this) {
    // Permission granted, gallery can now access media files
    }
    }

    override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
    }

    inner class ViewAdapter : FragmentStatePagerAdapter {
    private var titles: Array<String> = arrayOf()

    constructor(context: Context, fm: FragmentManager) : super(
    fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
    ) {
    titles = arrayOf("GSR Data", "Videos", "RAW Images", "Sessions")
    }

    override fun getCount(): Int {
    return titles.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
    return titles[position]
    }

    override fun getItem(position: Int): Fragment {
    return when (position) {
    0 -> GSRDataFragment()
    1 -> GSRVideoFragment()
    2 -> GSRRawImageFragment()
    else -> GSRSessionFragment()
    }
    }
    }
}
