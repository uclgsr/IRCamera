package com.topdon.module.thermal.activity

import android.widget.TextView
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.module.thermal.R

connectiondevice
// Legacy ARouter route annotation - now using NavigationManager
/**
 * Connect activity for thermal imaging interface.
 * Manages UI interactions and thermal data display.
 */
class ConnectActivity : BaseActivity() {
    override fun initContentView() = R.layout.activity_connect

    override fun initView() {
        // Set toolbar title
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(com.topdon.lib.core.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.app_name)

        val bluetoothBtn = findViewById<TextView>(R.id.bluetooth_btn)
        val isDeviceConnected = DeviceTools.isConnect()
        if (!isDeviceConnected) {
未connection
            bluetoothBtn.text = getString(R.string.app_no_connect)
        } else {
已connection
            bluetoothBtn.text = getString(R.string.app_connect)
        }
    }

    override fun initData() {
    }
}
