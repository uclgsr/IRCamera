package com.topdon.module.user.activity

import android.os.Bundle
import androidx.core.view.isVisible
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.module.user.databinding.ActivityUnitBinding
import com.topdon.lib.core.view.TitleView
import com.topdon.module.user.R

/**
 * 温度单位切换
 */
// Legacy ARouter route annotation - now using NavigationManager
class UnitActivity : BaseBindingActivity<ActivityUnitBinding>() {

    override fun initContentLayoutId() = R.layout.activity_unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.titleView.setRightClickListener {
            SharedManager.setTemperature(if (binding.ivDegreesCelsius.isVisible) 1 else 0)
            finish()
        }

        binding.ivDegreesCelsius.isVisible = SharedManager.getTemperature() == 1
        binding.ivFahrenheit.isVisible = SharedManager.getTemperature() == 0

        binding.constraintDegreesCelsius.setOnClickListener {
            binding.ivDegreesCelsius.isVisible = true
            binding.ivFahrenheit.isVisible = false
        }
        binding.constraintFahrenheit.setOnClickListener {
            binding.ivDegreesCelsius.isVisible = false
            binding.ivFahrenheit.isVisible = true
        }
    }

    override fun initData() {

    }

}

