package com.topdon.module.user.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.topdon.lib.core.common.SharedManager
import com.topdon.module.user.databinding.ActivityUnitBinding

/**
temperature单位switch
 */
class UnitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUnitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnitBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}
