@file:Suppress("DEPRECATION")

package com.topdon.lib.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.topdon.lib.core.R
import com.topdon.lib.ui.widget.IndicateView
import kotlin.collections.ArrayList
import com.topdon.lib.ui.R as UiR

class TipGuideDialog : DialogFragment() {
    private lateinit var titleList: ArrayList<String>
    private lateinit var imgList: ArrayList<Int>
    var closeEvent: ((check: Boolean) -> Unit)? = null

    private lateinit var tvContent1: TextView
    private lateinit var tvContent2: TextView
    private lateinit var tvContent3: TextView
    private lateinit var viewPager: ViewPager
    private lateinit var ivTarget: AppCompatImageView
    private lateinit var indicateView: IndicateView
    private var index: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(UiR.layout.dialog_tip_guide, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        titleList =
            arrayListOf(
                getString(R.string.target_tips_step_1),
                getString(R.string.target_tips_step_2),
                getString(R.string.target_tips_step_3),
                getString(R.string.target_tips_step_4),
            )
        imgList =
            arrayListOf(
                UiR.drawable.target_guide_pic_1,
                UiR.drawable.target_guide_pic_2,
                UiR.drawable.target_guide_pic_3,
                UiR.drawable.target_guide_pic_4,
            )
        viewPager = view.findViewById(UiR.id.view_pager)
        tvContent1 = view.findViewById(UiR.id.tv_content_1)
        tvContent2 = view.findViewById(UiR.id.tv_content_2)
        tvContent3 = view.findViewById(UiR.id.tv_content_3)
        indicateView = view.findViewById(UiR.id.indicate_view)
        ivTarget = view.findViewById(UiR.id.iv_target)
        val adapter = PageAdapter(childFragmentManager, imgList)
        indicateView.itemCount = adapter.count
        viewPager.adapter = adapter
        view.findViewById<TextView>(UiR.id.tv_i_know).setOnClickListener {
            closeEvent?.invoke(true)
            dismiss()
        }
        updateIndex(0)
        viewPager.addOnPageChangeListener(
            object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                }

                override fun onPageSelected(position: Int) {
                    updateIndex(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                }
            },
        )
    }

    fun updateIndex(position: Int) {
        if (index == position) {
            return
        }
        indicateView.currentIndex = position
        viewPager.setCurrentItem(position, true)
        when (position) {
            0 -> {
                tvContent1.visibility = View.VISIBLE
                tvContent3.visibility = View.VISIBLE
                ivTarget.visibility = View.GONE
            }

            2 -> {
                tvContent1.visibility = View.GONE
                tvContent3.visibility = View.GONE
                ivTarget.visibility = View.VISIBLE
            }

            else -> {
                tvContent1.visibility = View.GONE
                tvContent3.visibility = View.GONE
                ivTarget.visibility = View.GONE
            }
        }
        tvContent2.text = titleList[position]
        index = position
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = -1
        params.height = -1
        dialog?.window?.attributes = params as WindowManager.LayoutParams
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun show(
        manager: FragmentManager,
        tag: String?,
    ) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(): TipGuideDialog {
            return TipGuideDialog()
        }
    }

    @Suppress("DEPRECATION")
    inner class PageAdapter(
        fragmentManager: FragmentManager,
        private val imgResList: ArrayList<Int>,
    ) :
        FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return imgResList.size
        }

        override fun getItem(position: Int): Fragment {
            return PageFragment.newInstance(imgResList[position])
        }
    }
}
