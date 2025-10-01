package com.mpdc4gsr.module.thermalunified.fragment


import android.content.Intent
import android.view.View
import android.widget.ImageView
import com.mpdc4gsr.libunified.app.bean.event.WinterClickEvent
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.ThermalIrNightComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.MonitoryHomeComposeActivity
import org.greenrobot.eventbus.EventBus

class AbilityFragment : BaseFragment(), View.OnClickListener {
    private var mIsTC007 = false

    private lateinit var ivWinter: ImageView
    private lateinit var viewMonitory: View
    private lateinit var viewHouse: View
    private lateinit var viewCar: View

    override fun initContentView() = R.layout.fragment_ability

    override fun initView() {
        mIsTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false

        ivWinter = requireView().findViewById(R.id.iv_winter)
        viewMonitory = requireView().findViewById(R.id.view_monitory)
        viewHouse = requireView().findViewById(R.id.view_house)
        viewCar = requireView().findViewById(R.id.view_car)

        ivWinter.setOnClickListener(this)
        viewMonitory.setOnClickListener(this)
        viewHouse.setOnClickListener(this)
        viewCar.setOnClickListener(this)
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            ivWinter -> {
                SharedManager.hasClickWinter = true
                EventBus.getDefault().post(WinterClickEvent())
                val url =
                    if (UrlConstants.BASE_URL == "https://api.topdon.com/") {
                        "https://app.topdon.com/h5/share/#/detectionGuidanceIndex?showHeader=1&" +
                                "languageId=1"
                    } else {
                        "http://172.16.66.77:8081/#/detectionGuidanceIndex?languageId=1&showHeader=1"
                    }
                NavigationManager.getInstance().build(RouterConfig.WEB_VIEW)
                    .withString(ExtraKeyConfig.URL, url)
                    .navigation(requireContext())
            }

            viewMonitory -> {
                val intent = Intent(requireContext(), MonitoryHomeComposeActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_TC007, mIsTC007)
                startActivity(intent)
            }

            viewHouse -> {


            }

            viewCar -> {
                if (mIsTC007) {
                    if (WebSocketProxy.getInstance().isConnected()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_07)
                            .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            .navigation(requireContext())
                    }
                } else {
                    if (DeviceTools.isTC001PlusConnect()) {
                        var intent = Intent(requireContext(), IRThermalPlusComposeActivity::class.java)
                        intent.putExtra(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                        startActivity(intent)
                    } else if (DeviceTools.isTC001LiteConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_TCLITE)
                            .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            .navigation(requireActivity())
                    } else if (DeviceTools.isHikConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_HIK_MAIN)
                            .withBoolean(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                            .navigation(requireActivity())
                    } else if (DeviceTools.isConnect(isSendConnectEvent = false, true)) {
                        var intent = Intent(requireContext(), ThermalIrNightComposeActivity::class.java)
                        intent.putExtra(ExtraKeyConfig.IS_CAR_DETECT_ENTER, true)
                        startActivity(intent)
                    } else {
                        TipDialog.Builder(requireContext())
                            .setMessage(R.string.device_connect_tip)
                            .setPositiveListener(R.string.app_confirm)
                            .create().show()
                    }
                }
            }
        }
    }
}
