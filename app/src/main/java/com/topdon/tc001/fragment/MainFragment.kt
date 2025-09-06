package com.topdon.tc001.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.libcom.navigation.NavigationManager
import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.repository.BatteryInfo
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.AppLanguageUtils
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.tools.LocaleContextWrapper
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.WsCmdConstants
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.tc001.DeviceTypeActivity
import com.csl.irCamera.R
import com.topdon.tc001.popup.DelPopup
import kotlinx.coroutines.launch
import org.bytedeco.librealsense.context
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject


/**
 * 首页 Fragment.
 *
 * Created by LCG on 2024/4/18.
 */
@SuppressLint("NotifyDataSetChanged")
class MainFragment : BaseFragment(), View.OnClickListener {

    private lateinit var adapter : MyAdapter
    
    // View references
    private lateinit var tvConnectDevice: TextView
    private lateinit var ivAdd: ImageView
    private lateinit var tvNoDeviceTitle: TextView
    private lateinit var tvHasDeviceTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var clHasDevice: View
    private lateinit var clNoDevice: View

    override fun initContentView(): Int = R.layout.fragment_main

    override fun initView() {
        // Initialize views using view?.findViewById since this is a fragment
        tvConnectDevice = view?.findViewById(R.id.tv_connect_device)!!
        ivAdd = view?.findViewById(R.id.iv_add)!!
        tvNoDeviceTitle = view?.findViewById(R.id.tv_no_device_title)!!
        tvHasDeviceTitle = view?.findViewById(R.id.tv_has_device_title)!!
        recyclerView = view?.findViewById(R.id.recycler_view)!!
        clHasDevice = view?.findViewById(R.id.cl_has_device)!!
        clNoDevice = view?.findViewById(R.id.cl_no_device)!!
        
        adapter = MyAdapter()
        tvConnectDevice.setOnClickListener(this)
        ivAdd.setOnClickListener(this)
        
        // GSR Multi-modal Recording Access (long press on titles for research features)
        tvNoDeviceTitle.setOnLongClickListener {
            showGSROptions()
            true
        }
        tvHasDeviceTitle.setOnLongClickListener {
            showGSROptions()
            true
        }
        
        adapter.hasConnectLine = DeviceTools.isConnect()
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        adapter.onItemClickListener = {
            when (it) {
                ConnectType.LINE -> {
                    NavigationManager.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, false)
                        .navigation(requireContext())
                }
                ConnectType.TS004 -> {
                    if (WebSocketProxy.getInstance().isTS004Connect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_MONOCULAR).navigation(requireContext())
                    } else {
                        NavigationManager.getInstance()
                            .build(RouterConfig.IR_DEVICE_ADD)
                            .withBoolean("isTS004", true)
                            .navigation(requireContext())
                    }
                }
                ConnectType.TC007 -> {
                    NavigationManager.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, true)
                        .navigation(requireContext())
                }
            }
        }
        adapter.onItemLongClickListener = { view, type ->
            val popup = DelPopup(requireContext())
            popup.onDelListener = {
                TipDialog.Builder(requireContext())
                    .setTitleMessage(AppLanguageUtils.attachBaseContext(
                        context, ConstantLanguages.ENGLISH).getString(R.string.tc_delete_device))
                    .setMessage(R.string.tc_delete_device_tips)
                    .setPositiveListener(R.string.report_delete) {
                        when (type) {
                            ConnectType.LINE -> SharedManager.hasTcLine = false
                            ConnectType.TS004 -> SharedManager.hasTS004 = false
                            ConnectType.TC007 -> SharedManager.hasTC007 = false
                        }
                        refresh()
                        TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                    }
                    .setCancelListener(R.string.app_cancel)
                    .create().show()
            }
            popup.show(view)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        if (WebSocketProxy.getInstance().isTC007Connect()) {
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // 要是当前已连接 TS004、TC007，切到流量上，不然登录注册意见反馈那些没网
                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.switchNetwork(true)
                }
            }
        })
    }

    override fun initData() {
    }

    override fun onResume() {
        super.onResume()
        refresh()
        adapter?.notifyDataSetChanged()
    }

    private fun refresh() {
        val hasAnyDevice = SharedManager.hasTcLine || SharedManager.hasTS004 || SharedManager.hasTC007
        clHasDevice.isVisible = hasAnyDevice
        clNoDevice.isVisible = !hasAnyDevice
        adapter.hasConnectLine = DeviceTools.isConnect(isAutoRequest = false)
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        adapter.notifyDataSetChanged()
    }

    override fun connected() {
        adapter.hasConnectLine = true
        SharedManager.hasTcLine = true
        refresh()
    }

    override fun disConnected() {
        adapter.hasConnectLine = false
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTS004) {
            SharedManager.hasTS004 = true
            adapter.hasConnectTS004 = true
        } else {
            SharedManager.hasTC007 = true
            adapter.hasConnectTC007 = true
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTS004) {
            adapter.hasConnectTS004 = false
        } else {
            adapter.hasConnectTC007 = false
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            tvConnectDevice, ivAdd -> {//添加设备
                startActivity(Intent(requireContext(), DeviceTypeActivity::class.java))
//                NavigationManager.getInstance().build(RoutePath.UsbIrModule.PAGE_IR_MAIN_ACTIVITY)
//                    .navigation()
//                startActivity(Intent(requireContext(), IRThermalLiteActivity::class.java))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        if (SocketCmdUtil.getCmdResponse(event.text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {//心跳
            if (!adapter.hasConnectTC007) {//当前连接的不是 TC007
                return
            }
            try {
                val battery: JSONObject = JSONObject(event.text).getJSONObject("battery")
                adapter.tc007Battery = BatteryInfo(battery.getString("status"), battery.getString("remaining"))
            } catch (_: Exception) {

            }
        }
    }

    private class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        /**
         * 有线设备当前是否已连接.
         */
        var hasConnectLine: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, 3)
            }
        /**
         * TS004 当前是否已连接.
         */
        var hasConnectTS004: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        /**
         * TC007 当前是否已连接.
         */
        var hasConnectTC007: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        /**
         * TC007 设备电池信息.
         */
        var tc007Battery: BatteryInfo? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemRangeChanged(0, itemCount)
                }
            }


        var onItemClickListener: ((type: ConnectType) -> Unit)? = null
        var onItemLongClickListener: ((view: View, type: ConnectType) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device_connect, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val type = holder.getConnectType(position)
            val hasTitle: Boolean = when (position) {
                0 -> true
                1 -> SharedManager.hasTcLine
                else -> false
            }
            val hasConnect: Boolean = when (type) {
                ConnectType.LINE -> hasConnectLine
                ConnectType.TS004 -> hasConnectTS004
                ConnectType.TC007 -> hasConnectTC007
            }
            
            holder.bind(type, hasTitle, hasConnect, hasConnectTC007, tc007Battery)
        }

        override fun getItemCount(): Int {
            var result = 0
            if (SharedManager.hasTcLine) {
                result++
            }
            if (SharedManager.hasTS004) {
                result++
            }
            if (SharedManager.hasTC007) {
                result++
            }
            return result
        }

        inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            // View references
            private val ivBg: View = rootView.findViewById(R.id.iv_bg)
            private val tvTitle: TextView = rootView.findViewById(R.id.tv_title)
            private val tvDeviceName: TextView = rootView.findViewById(R.id.tv_device_name)
            private val tvDeviceState: TextView = rootView.findViewById(R.id.tv_device_state)
            private val tvBattery: TextView = rootView.findViewById(R.id.tv_battery)
            private val ivImage: ImageView = rootView.findViewById(R.id.iv_image)
            private val batteryView: com.topdon.lib.ui.widget.BatteryView = rootView.findViewById(R.id.battery_view)
            private val viewDeviceState: View = rootView.findViewById(R.id.view_device_state)
            
            init {
                ivBg.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(getConnectType(position))
                    }
                }
                ivBg.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        //只有离线设备才能长按删除
                        val deviceType = getConnectType(position)
                        when (deviceType) {
                            ConnectType.LINE -> {
                                if (DeviceTools.isConnect()) {
                                    return@setOnLongClickListener true
                                }
                            }
                            ConnectType.TS004 -> {
                                if (WebSocketProxy.getInstance().isTS004Connect()) {
                                    return@setOnLongClickListener true
                                }
                            }
                            ConnectType.TC007 -> {
                                if (WebSocketProxy.getInstance().isTC007Connect()) {
                                    return@setOnLongClickListener true
                                }
                            }
                        }
                        onItemLongClickListener?.invoke(ivBg, deviceType)
                    }
                    true
                }
            }
            
            fun bind(type: ConnectType, hasTitle: Boolean, hasConnect: Boolean, hasConnectTC007: Boolean, tc007Battery: BatteryInfo?) {
                tvTitle.isVisible = hasTitle
                tvTitle.text = AppLanguageUtils.attachBaseContext(
                    itemView.context, ConstantLanguages.ENGLISH)
                    .getString(if (type == ConnectType.LINE) R.string.tc_connect_line else R.string.tc_connect_wifi)

                ivBg.isSelected = hasConnect
                tvDeviceName.isSelected = hasConnect
                viewDeviceState.isSelected = hasConnect
                tvDeviceState.isSelected = hasConnect
                tvDeviceState.text = if (hasConnect) "online" else "offline"
                tvBattery.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null
                batteryView.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null

                when (type) {
                    ConnectType.LINE -> {
                        tvDeviceName.setText(AppLanguageUtils.attachBaseContext(
                            itemView.context, ConstantLanguages.ENGLISH)
                            .getString(R.string.tc_has_line_device))
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_line_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_line_disconnect)
                        }
                    }
                    ConnectType.TS004 -> {
                        tvDeviceName.text = "TS004"
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_disconnect)
                        }
                    }
                    ConnectType.TC007 -> {
                        tvDeviceName.text = "TC007"
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_tc007_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_tc007_disconnect)
                        }
                        tvBattery.text = "${tc007Battery?.getBattery()}%"
                        batteryView.battery = tc007Battery?.getBattery() ?: 0
                        batteryView.isCharging = tc007Battery?.isCharging() ?: false
                    }
                }
            }

            fun getConnectType(position: Int): ConnectType = when (position) {
                0 -> if (SharedManager.hasTcLine) {
                    ConnectType.LINE
                } else if (SharedManager.hasTS004) {
                    ConnectType.TS004
                } else {
                    ConnectType.TC007
                }
                1 -> if (SharedManager.hasTcLine) {
                    if (SharedManager.hasTS004) ConnectType.TS004 else ConnectType.TC007
                } else {
                    ConnectType.TC007
                }
                else -> ConnectType.TC007
            }
        }
    }
    
    /**
     * Show GSR Multi-modal Recording options for research purposes
     * Accessed via long-press on app title
     */
    private fun showGSROptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("GSR Multi-modal Recording")
            .setMessage("Choose GSR recording option:")
            .setPositiveListener("Full Recording") {
                // Launch full multi-modal recording interface
                NavigationManager.getInstance()
                    .build(RouterConfig.GSR_MULTI_MODAL)
                    .navigation(requireContext())
            }
            .setCancelListener("GSR Demo") {
                // Launch simple GSR demo
                NavigationManager.getInstance()
                    .build(RouterConfig.GSR_DEMO)
                    .navigation(requireContext())
            }
            .create().show()
    }

    enum class ConnectType {
        LINE,
        TS004,
        TC007,
    }
}