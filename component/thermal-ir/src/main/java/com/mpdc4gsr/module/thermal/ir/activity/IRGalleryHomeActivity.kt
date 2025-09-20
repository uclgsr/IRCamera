package com.mpdc4gsr.module.thermal.ir.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.repository.GalleryRepository.DirType
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.thermal.ir.viewmodel.IRGalleryTabViewModel


class IRGalleryHomeActivity : BaseActivity() {
    private var isTS004Remote = false

    private val viewModel: IRGalleryTabViewModel by viewModels()

    override fun initContentView(): Int = R.layout.activity_ir_gallery_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTS004Remote =
            intent.getIntExtra(ExtraKeyConfig.DIR_TYPE, 0) == DirType.TS004_REMOTE.ordinal

        if (savedInstanceState == null) {
            val bundle = Bundle()
            bundle.putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false)
            bundle.putBoolean(ExtraKeyConfig.HAS_BACK_ICON, true)
            bundle.putInt(ExtraKeyConfig.DIR_TYPE, intent.getIntExtra(ExtraKeyConfig.DIR_TYPE, 0))

            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, IRGalleryTabFragment::class.java, bundle)
                .commit()
        }

        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.isEditModeLD.value = false
                }
            }
        onBackPressedDispatcher.addCallback(this, callback)

        viewModel.isEditModeLD.observe(this) {
            callback.isEnabled = it
        }
    }

    override fun initView() {
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
