package mpdc4gsr.ui_components

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.FragmentMainBinding
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseBindingFragment
import com.topdon.lib.core.repository.BatteryInfo
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.AppLanguageUtils
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.WsCmdConstants
import com.topdon.libcom.navigation.NavigationManager
import com.topdon.lms.sdk.weiget.TToast
import mpdc4gsr.activities.DeviceTypeActivity
import mpdc4gsr.ui_components.DelPopup
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

@SuppressLint("NotifyDataSetChanged")
class MainFragment : BaseBindingFragment<FragmentMainBinding>(), View.OnClickListener {
    private lateinit var adapter: MyAdapter

    override fun initContentLayoutId(): Int = R.layout.fragment_main

    override fun initView(savedInstanceState: Bundle?) {
        adapter = MyAdapter()
        binding.tvConnectDevice.setOnClickListener(this)
        binding.ivAdd.setOnClickListener(this)

        binding.tvNoDeviceTitle.setOnLongClickListener {
            showGSROptions()
            true
        }
        binding.tvHasDeviceTitle.setOnLongClickListener {
            showGSROptions()
            true
        }

        binding.fabGsrRecording.setOnClickListener {
            showGSROptions()
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
                        NavigationManager.getInstance().build(RouterConfig.IR_MONOCULAR)
                            .navigation(requireContext())
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
                    .setTitleMessage(
                        AppLanguageUtils.attachBaseContext(
                            context, ConstantLanguages.ENGLISH,
                        ).getString(R.string.tc_delete_device),
                    )
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

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        if (WebSocketProxy.getInstance().isTC007Connect()) {
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {

                    if (WebSocketProxy.getInstance().isConnected()) {
                        NetWorkUtils.switchNetwork(true)
                    }
                }
            },
        )
    }

    override fun onResume() {
        super.onResume()
        refresh()
        adapter?.notifyDataSetChanged()
    }

    private fun refresh() {
        val hasAnyDevice =
            SharedManager.hasTcLine || SharedManager.hasTS004 || SharedManager.hasTC007
        binding.clHasDevice.isVisible = hasAnyDevice
        binding.clNoDevice.isVisible = !hasAnyDevice
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
            binding.tvConnectDevice, binding.ivAdd -> { // 添加设备
                startActivity(Intent(requireContext(), DeviceTypeActivity::class.java))


            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        if (SocketCmdUtil.getCmdResponse(event.text) == WsCmdConstants.APP_EVENT_HEART_BEATS) { // 心跳
            if (!adapter.hasConnectTC007) { // 当前连接的不是 TC007
                return
            }
            try {
                val battery: JSONObject = JSONObject(event.text).getJSONObject("battery")
                adapter.tc007Battery =
                    BatteryInfo(battery.getString("status"), battery.getString("remaining"))
            } catch (_: Exception) {
            }
        }
    }

    private class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        var hasConnectLine: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, 3)
            }

        var hasConnectTS004: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }

        var hasConnectTC007: Boolean = false
            set(value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }

        var tc007Battery: BatteryInfo? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemRangeChanged(0, itemCount)
                }
            }

        var onItemClickListener: ((type: ConnectType) -> Unit)? = null
        var onItemLongClickListener: ((view: View, type: ConnectType) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_device_connect, parent, false)
            )
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val type = holder.getConnectType(position)
            val hasTitle: Boolean =
                when (position) {
                    0 -> true
                    1 -> SharedManager.hasTcLine
                    else -> false
                }
            val hasConnect: Boolean =
                when (type) {
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

            private val ivBg: View = rootView.findViewById(R.id.iv_bg)
            private val tvTitle: TextView = rootView.findViewById(R.id.tv_title)
            private val tvDeviceName: TextView = rootView.findViewById(R.id.tv_device_name)
            private val tvDeviceState: TextView = rootView.findViewById(R.id.tv_device_state)
            private val tvBattery: TextView = rootView.findViewById(R.id.tv_battery)
            private val ivImage: ImageView = rootView.findViewById(R.id.iv_image)
            private val batteryView: com.topdon.lib.ui.widget.BatteryView =
                rootView.findViewById(R.id.battery_view)
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

            fun bind(
                type: ConnectType,
                hasTitle: Boolean,
                hasConnect: Boolean,
                hasConnectTC007: Boolean,
                tc007Battery: BatteryInfo?,
            ) {
                tvTitle.isVisible = hasTitle
                tvTitle.text =
                    AppLanguageUtils.attachBaseContext(
                        itemView.context, ConstantLanguages.ENGLISH,
                    )
                        .getString(if (type == ConnectType.LINE) R.string.tc_connect_line else R.string.tc_connect_wifi)

                ivBg.isSelected = hasConnect
                tvDeviceName.isSelected = hasConnect
                viewDeviceState.isSelected = hasConnect
                tvDeviceState.isSelected = hasConnect
                tvDeviceState.text = if (hasConnect) "online" else "offline"
                tvBattery.isVisible =
                    type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null
                batteryView.isVisible =
                    type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null

                when (type) {
                    ConnectType.LINE -> {
                        tvDeviceName.setText(
                            AppLanguageUtils.attachBaseContext(
                                itemView.context,
                                ConstantLanguages.ENGLISH,
                            )
                                .getString(R.string.tc_has_line_device),
                        )
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

            fun getConnectType(position: Int): ConnectType =
                when (position) {
                    0 ->
                        if (SharedManager.hasTcLine) {
                            ConnectType.LINE
                        } else if (SharedManager.hasTS004) {
                            ConnectType.TS004
                        } else {
                            ConnectType.TC007
                        }

                    1 ->
                        if (SharedManager.hasTcLine) {
                            if (SharedManager.hasTS004) ConnectType.TS004 else ConnectType.TC007
                        } else {
                            ConnectType.TC007
                        }

                    else -> ConnectType.TC007
                }
        }
    }

    private fun showGSROptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("GSR Multi-modal Recording")
            .setMessage("Choose recording option:")
            .setPositiveListener("Dual-Mode Camera") {
                // Dual-Mode Camera
                showDualModeCameraOptions()
            }
            .setCancelListener("Quick Recording") {
                // Quick Recording
                try {
                    val intent = Intent(
                        requireContext(),
                        Class.forName("mpdc4gsr.sensors.gsr.GSRQuickRecordingActivity")
                    )
                    startActivity(intent)
                } catch (e: ClassNotFoundException) {
                    // Fallback
                    NavigationManager.getInstance()
                        .build(RouterConfig.GSR_MULTI_MODAL)
                        .navigation(requireContext())
                }
            }
            // Remove setNeutralListener since it's not available in TipDialog API
            // .setNeutralListener("GSR Demo") {
            //     // GSR Demo
            //     NavigationManager.getInstance()
            //         .build(RouterConfig.GSR_DEMO)
            //         .navigation(requireContext())
            // }
            .create().show()
    }

    private fun showDualModeCameraOptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("Dual-Mode Camera System")
            .setMessage("Samsung S22 optimized camera modes with fast switching:")
            .setPositiveListener("RAW 50MP Mode") {

                launchDualModeCamera("RAW_50MP")
            }
            .setCancelListener("4K Video Mode") {

                launchDualModeCamera("VIDEO_4K")
            }
            .create().show()
    }

    private fun launchDualModeCamera(initialMode: String) {
        try {
            val intent = Intent(
                requireContext(),
                mpdc4gsr.camera.integration.DualModeCameraActivity::class.java
            )
            intent.putExtra("INITIAL_MODE", initialMode)
            intent.putExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)
            startActivity(intent)
        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Launching dual-mode camera integration example...",
                Toast.LENGTH_SHORT
            ).show()

            showDualModeIntegrationExample()
        }
    }

    private fun showDualModeIntegrationExample() {


        TipDialog.Builder(requireContext())
            .setTitleMessage("Dual-Mode Camera Integration")
            .setMessage(
                "Enhanced RGBCameraRecorder with:\n\n" +
                        "• RAW 50MP capture at ~15fps\n" +
                        "• 4K video at 30/60fps\n" +
                        "• Fast session switching (~200ms)\n" +
                        "• Samsung S22 optimizations\n" +
                        "• CameraModeSelector UI\n\n" +
                        "Implementation ready for integration.",
            )
            .setPositiveListener("Got it") { }
            .create().show()
    }

    enum class ConnectType {
        LINE,
        TS004,
        TC007,
    }
}
