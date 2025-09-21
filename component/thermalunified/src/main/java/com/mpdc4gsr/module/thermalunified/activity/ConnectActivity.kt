package com.mpdc4gsr.module.thermal.activity

import android.widget.TextView
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.module.thermal.R


class ConnectActivity : BaseActivity() {
    override fun initContentView() = R.layout.activity_connect

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.libunified.R.id.toolbar_lay)
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
