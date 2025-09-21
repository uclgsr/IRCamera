package com.mpdc4gsr.module.thermalunified.activity

import android.widget.TextView
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.tools.DeviceTools
import com.mpdc4gsr.module.thermalunified.R


class ConnectActivity : BaseActivity() {
    override fun initContentView() = R.layout.activity_connect

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.lib.core.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.app_name)

        val bluetoothBtn = findViewById<TextView>(R.id.bluetooth_btn)
        val isDeviceConnected = DeviceTools.isConnect()
        if (!isDeviceConnected) {

            bluetoothBtn.text = getString(R.string.app_no_connect)
        } else {

            bluetoothBtn.text = getString(R.string.app_connect)
        }
    }

    override fun initData() {
    }
}
