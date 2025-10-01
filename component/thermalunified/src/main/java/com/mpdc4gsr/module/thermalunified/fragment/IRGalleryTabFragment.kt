package com.mpdc4gsr.module.thermalunified.fragment

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.libunified.app.view.MyTextView
import com.mpdc4gsr.libunified.app.view.TitleView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.event.GalleryDirChangeEvent
import com.mpdc4gsr.module.thermalunified.popup.GalleryChangePopup
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryTabViewModel
import org.greenrobot.eventbus.EventBus
import com.mpdc4gsr.libunified.R as LibCoreR
import com.mpdc4gsr.libunified.R as UiR


class IRGalleryTabFragment : BaseFragment() {

    private var hasBackIcon = false

    private var canSwitchDir = true

    private var currentDirType = DirType.LINE

    private val viewModel: IRGalleryTabViewModel by activityViewModels()

    private var viewPagerAdapter: ViewPagerAdapter? = null

    private lateinit var titleView: TitleView
    private lateinit var tvTitleDir: MyTextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2

    override fun initContentView(): Int = R.layout.fragment_gallery_tab

    override fun initView() {

        titleView = requireView().findViewById(R.id.title_view)
        tvTitleDir = requireView().findViewById(R.id.tv_title_dir)
        tabLayout = requireView().findViewById(R.id.tab_layout)
        viewPager2 = requireView().findViewById(R.id.view_pager2)

        hasBackIcon = arguments?.getBoolean(ExtraKeyConfig.HAS_BACK_ICON, false) ?: false
        canSwitchDir = arguments?.getBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false) ?: false
        currentDirType =
            when (arguments?.getInt(ExtraKeyConfig.DIR_TYPE, 0) ?: 0) {
                DirType.TS004_LOCALE.ordinal -> DirType.TS004_LOCALE
                DirType.TS004_REMOTE.ordinal -> DirType.TS004_REMOTE
                DirType.TC007.ordinal -> DirType.TC007
                else -> DirType.LINE
            }

        tvTitleDir.text =
            when (currentDirType) {
                DirType.LINE -> getString(R.string.tc_has_line_device)
                DirType.TC007 -> "TC007"
                else -> "TS004"
            }
        tvTitleDir.isVisible = canSwitchDir
        tvTitleDir.setOnClickListener {
            val popup = GalleryChangePopup(requireContext())
            popup.onPickListener = { position, str ->
                currentDirType =
                    when (position) {
                        0 -> DirType.LINE
                        1 -> DirType.TS004_LOCALE
                        else -> DirType.TC007
                    }
                tvTitleDir.text = str
                EventBus.getDefault().post(GalleryDirChangeEvent(currentDirType))
            }
            popup.show(tvTitleDir)
        }

        titleView.setTitleText(if (canSwitchDir) "" else getString(LibCoreR.string.app_gallery))
        titleView.setLeftDrawable(if (hasBackIcon) R.drawable.ic_back_white_svg else 0)
        titleView.setLeftClickListener {
            if (viewModel.isEditModeLD.value == true) {
                viewModel.isEditModeLD.value = false
            } else {
                if (hasBackIcon) {
                    requireActivity().finish()
                }
            }
        }
        titleView.setRightDrawable(UiR.drawable.ic_toolbar_check_svg)
        titleView.setRightClickListener {
            if (viewModel.isEditModeLD.value == true) {
                viewModel.selectAllIndex.value = viewPager2.currentItem
            } else {
                viewModel.isEditModeLD.value = true
            }
        }

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager2.adapter = viewPagerAdapter
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.setText(if (position == 0) LibCoreR.string.album_menu_Photos else LibCoreR.string.app_video)
        }.attach()

        viewModel.isEditModeLD.observe(viewLifecycleOwner) { isEditMode ->
            if (isEditMode) {
                titleView.setLeftDrawable(LibCoreR.drawable.svg_x_cc)
            } else {
                titleView.setLeftDrawable(if (hasBackIcon) R.drawable.ic_back_white_svg else 0)
            }
            titleView.setRightDrawable(if (isEditMode) 0 else UiR.drawable.ic_toolbar_check_svg)
            titleView.setRightText(if (isEditMode) getString(LibCoreR.string.report_select_all) else "")
            tabLayout.isVisible = !isEditMode
            viewPager2.isUserInputEnabled = !isEditMode
            if (isEditMode) {
                titleView.setTitleText(
                    getString(
                        LibCoreR.string.chosen_item,
                        viewModel.selectSizeLD.value
                    )
                )
                tvTitleDir.isVisible = false
            } else {
                titleView.setTitleText(if (canSwitchDir) "" else getString(LibCoreR.string.app_gallery))
                tvTitleDir.isVisible = canSwitchDir
            }
        }
        viewModel.selectSizeLD.observe(viewLifecycleOwner) {
            if (viewModel.isEditModeLD.value == true) {
                titleView.setTitleText(getString(LibCoreR.string.chosen_item, it))
                tvTitleDir.isVisible = false
            } else {
                titleView.setTitleText(if (canSwitchDir) "" else getString(LibCoreR.string.app_gallery))
                tvTitleDir.isVisible = canSwitchDir
            }
        }
    }

    override fun initData() {
    }

    private inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle()
            bundle.putBoolean(ExtraKeyConfig.IS_VIDEO, position == 1)
            bundle.putInt(ExtraKeyConfig.DIR_TYPE, currentDirType.ordinal)
            val fragment = IRGalleryComposeFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
