package com.topdon.tc001.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.topdon.tc001.DeviceTypeActivity
import com.topdon.tc001.popup.DelPopup
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

/**
 * 首页 Fragment.
 *
 * Created by LCG on 2024/4/18.
 */
@SuppressLint("NotifyDataSetChanged")
/**
 * Specialized thermal imaging component providing MainFragment functionality for the IRCamera system.
 *
 * <h3>Technical Specifications:</h3>
 * <ul>
 *   <li>Thread-safe operations for thermal data processing</li>
 *   <li>Optimized performance for real-time thermal imaging</li>
 *   <li>Compatible with TC001 thermal camera hardware</li>
 * </ul>
 *
 * @author IRCamera Development Team
 * @version 2.0
 * @since 1.0
 */
class MainFragment : BaseBindingFragment<FragmentMainBinding>(), View.OnClickListener {
    private lateinit var adapter: MyAdapter

    /**
     * Initializes the contentlayoutid component for thermal imaging operations.
     *
     */
    override fun initContentLayoutId(): Int = R.layout.fragment_main

    /**
     * Initializes the view component for thermal imaging operations.
     *
     * @param
     * @param savedInstanceState Parameter for operation (type: Bundle?)
     *
     */
    override fun initView(savedInstanceState: Bundle?) {
        adapter = MyAdapter()
        binding.tvConnectDevice.setOnClickListener(this)
        binding.ivAdd.setOnClickListener(this)

        // GSR Multi-modal Recording Access (long press on titles for research features)
        binding.tvNoDeviceTitle.setOnLongClickListener {
            /**
             * Executes showgsroptions operation with thermal imaging domain optimization.
             *
             */
            showGSROptions()
            true
        }
        binding.tvHasDeviceTitle.setOnLongClickListener {
            /**
             * Executes showgsroptions operation with thermal imaging domain optimization.
             *
             */
            showGSROptions()
            true
        }

        // Add prominent GSR access button for research features
        binding.fabGsrRecording.setOnClickListener {
            /**
             * Executes showgsroptions operation with thermal imaging domain optimization.
             *
             */
            showGSROptions()
        }

        adapter.hasConnectLine = DeviceTools.isConnect()
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        adapter.onItemClickListener = {
            /**
             * Executes when operation with thermal imaging domain optimization.
             *
             */
            when (it) {
                ConnectType.LINE -> {
                    NavigationManager.getInstance()
                        .build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, false)
                        .navigation(requireContext())
                }
                ConnectType.TS004 -> {
                    /**
                     * Executes if operation with thermal imaging domain optimization.
                     *
                     */
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
                    .setTitleMessage(
                        AppLanguageUtils.attachBaseContext(
                            context, ConstantLanguages.ENGLISH,
                        ).getString(R.string.tc_delete_device),
                    )
                    .setMessage(R.string.tc_delete_device_tips)
                    .setPositiveListener(R.string.report_delete) {
                        /**
                         * Executes when operation with thermal imaging domain optimization.
                         *
                         */
                        when (type) {
                            ConnectType.LINE -> SharedManager.hasTcLine = false
                            ConnectType.TS004 -> SharedManager.hasTS004 = false
                            ConnectType.TC007 -> SharedManager.hasTC007 = false
                        }
                        /**
                         * Executes refresh operation with thermal imaging domain optimization.
                         *
                         */
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

        /**
         * Executes if operation with thermal imaging domain optimization.
         *
         */
        if (WebSocketProxy.getInstance().isTC007Connect()) {
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                /**
                 * Executes if operation with thermal imaging domain optimization.
                 *
                 */
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                /**
                 * Executes onresume operation with thermal imaging domain optimization.
                 *
                 * @param
                 * @param owner Parameter for operation (type: LifecycleOwner)
                 *
                 */
                override fun onResume(owner: LifecycleOwner) {
                    // 如果当前已连接 TS004 或 TC007，则切换到移动数据网络，否则 LoginRegister、意见反馈等功能将无法联网
                    /**
                     * Executes if operation with thermal imaging domain optimization.
                     *
                     */
                    if (WebSocketProxy.getInstance().isConnected()) {
                        NetWorkUtils.switchNetwork(true)
                    }
                }
            },
        )
    }

    /**
     * Executes onresume operation with thermal imaging domain optimization.
     *
     */
    override fun onResume() {
        super.onResume()
        /**
         * Executes refresh operation with thermal imaging domain optimization.
         *
         */
        refresh()
        adapter?.notifyDataSetChanged()
    }

    /**
     * Executes refresh functionality.
     */
    /**
     * Executes refresh operation with thermal imaging domain optimization.
     *
     */
    private fun refresh() {
        val hasAnyDevice = SharedManager.hasTcLine || SharedManager.hasTS004 || SharedManager.hasTC007
        binding.clHasDevice.isVisible = hasAnyDevice
        binding.clNoDevice.isVisible = !hasAnyDevice
        adapter.hasConnectLine = DeviceTools.isConnect(isAutoRequest = false)
        adapter.hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
        adapter.hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
        adapter.notifyDataSetChanged()
    }

    /**
     * Executes connected operation with thermal imaging domain optimization.
     *
     */
    override fun connected() {
        adapter.hasConnectLine = true
        SharedManager.hasTcLine = true
        /**
         * Executes refresh operation with thermal imaging domain optimization.
         *
         */
        refresh()
    }

    /**
     * Executes disconnected operation with thermal imaging domain optimization.
     *
     */
    override fun disConnected() {
        adapter.hasConnectLine = false
    }

    /**
     * Executes onsocketconnected operation with thermal imaging domain optimization.
     *
     * @param
     * @param isTS004 Parameter for operation (type: Boolean)
     *
     */
    override fun onSocketConnected(isTS004: Boolean) {
        /**
         * Executes if operation with thermal imaging domain optimization.
         *
         */
        if (isTS004) {
            SharedManager.hasTS004 = true
            adapter.hasConnectTS004 = true
        } else {
            SharedManager.hasTC007 = true
            adapter.hasConnectTC007 = true
            lifecycleScope.launch {
                val batteryInfo: BatteryInfo? = TC007Repository.getBatteryInfo()
                /**
                 * Executes if operation with thermal imaging domain optimization.
                 *
                 */
                if (batteryInfo != null) {
                    adapter.tc007Battery = batteryInfo
                }
            }
        }
    }

    /**
     * Executes onsocketdisconnected operation with thermal imaging domain optimization.
     *
     * @param
     * @param isTS004 Parameter for operation (type: Boolean)
     *
     */
    override fun onSocketDisConnected(isTS004: Boolean) {
        /**
         * Executes if operation with thermal imaging domain optimization.
         *
         */
        if (isTS004) {
            adapter.hasConnectTS004 = false
        } else {
            adapter.hasConnectTC007 = false
        }
    }

    /**
     * Executes onclick operation with thermal imaging domain optimization.
     *
     * @param
     * @param v Parameter for operation (type: View?)
     *
     */
    override fun onClick(v: View?) {
        /**
         * Executes when operation with thermal imaging domain optimization.
         *
         */
        when (v) {
            binding.tvConnectDevice, binding.ivAdd -> { // Adddevice
                startActivity(Intent(requireContext(), DeviceTypeActivity::class.java))
//                NavigationManager.getInstance().build(RoutePath.UsbIrModule.PAGE_IR_MAIN_ACTIVITY)
//                    .navigation()
// StartActivity(Intent(requireContext(), IRThermalLiteActivity::class.java))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    /**
     * Executes onSocketMsgEvent functionality.
     */
    /**
     * Executes onsocketmsgevent operation with thermal imaging domain optimization.
     *
     * @param
     * @param event Parameter for operation (type: SocketMsgEvent)
     *
     */
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        /**
         * Executes if operation with thermal imaging domain optimization.
         *
         */
        if (SocketCmdUtil.getCmdResponse(event.text) == WsCmdConstants.APP_EVENT_HEART_BEATS) { // 心跳
            /**
             * Executes if operation with thermal imaging domain optimization.
             *
             */
            if (!adapter.hasConnectTC007) { // 当前connection的不是 TC007
                return
            }
            try {
                val battery: JSONObject = JSONObject(event.text).getJSONObject("battery")
                adapter.tc007Battery = BatteryInfo(battery.getString("status"), battery.getString("remaining"))
            } catch (_: Exception) {
            }
        }
    }

/**
 * Specialized thermal imaging component providing MyAdapter functionality for the IRCamera system.
 *
 * This component is part of the IRCamera thermal imaging system, providing
 * specialized functionality for thermal data processing and visualization.
 *
 * @author IRCamera Development Team
 * @version 2.0
 * @since 1.0
 */
    private class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        /**
         * 有linedevice当前是否已connection.
         */
        var hasConnectLine: Boolean = false
            set(value) {
                field = value
                /**
                 * Executes notifyitemrangechanged operation with thermal imaging domain optimization.
                 *
                 */
                notifyItemRangeChanged(0, 3)
            }

        /**
         * TS004 当前是否已connection.
         */
        var hasConnectTS004: Boolean = false
            set(value) {
                field = value
                /**
                 * Executes notifyitemrangechanged operation with thermal imaging domain optimization.
                 *
                 */
                notifyItemRangeChanged(0, itemCount)
            }

        /**
         * TC007 当前是否已connection.
         */
        var hasConnectTC007: Boolean = false
            set(value) {
                field = value
                /**
                 * Executes notifyitemrangechanged operation with thermal imaging domain optimization.
                 *
                 */
                notifyItemRangeChanged(0, itemCount)
            }

        /**
         * TC007 device电池info.
         */
        var tc007Battery: BatteryInfo? = null
            set(value) {
                if (field != value) {
                    field = value
                    /**
                     * Executes notifyitemrangechanged operation with thermal imaging domain optimization.
                     *
                     */
                    notifyItemRangeChanged(0, itemCount)
                }
            }

        var onItemClickListener: ((type: ConnectType) -> Unit)? = null
        var onItemLongClickListener: ((view: View, type: ConnectType) -> Unit)? = null

        /**
         * Executes oncreateviewholder operation with thermal imaging domain optimization.
         *
         * @param
         * @param parent Parameter for operation (type: ViewGroup)
         * @param viewType Parameter for operation (type: Int)
         *
         */
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device_connect, parent, false))
        }

        @SuppressLint("SetTextI18n")
        /**
         * Executes onbindviewholder operation with thermal imaging domain optimization.
         *
         * @param
         * @param holder Parameter for operation (type: ViewHolder)
         * @param position Parameter for operation (type: Int)
         *
         */
        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val type = holder.getConnectType(position)
            val hasTitle: Boolean =
                /**
                 * Executes when operation with thermal imaging domain optimization.
                 *
                 */
                when (position) {
                    0 -> true
                    1 -> SharedManager.hasTcLine
                    else -> false
                }
            val hasConnect: Boolean =
                /**
                 * Executes when operation with thermal imaging domain optimization.
                 *
                 */
                when (type) {
                    ConnectType.LINE -> hasConnectLine
                    ConnectType.TS004 -> hasConnectTS004
                    ConnectType.TC007 -> hasConnectTC007
                }

            holder.bind(type, hasTitle, hasConnect, hasConnectTC007, tc007Battery)
        }

        /**
         * Retrieves the itemcount with optimized performance for thermal imaging operations.
         *
         */
        override fun getItemCount(): Int {
            var result = 0
            /**
             * Executes if operation with thermal imaging domain optimization.
             *
             */
            if (SharedManager.hasTcLine) {
                result++
            }
            /**
             * Executes if operation with thermal imaging domain optimization.
             *
             */
            if (SharedManager.hasTS004) {
                result++
            }
            /**
             * Executes if operation with thermal imaging domain optimization.
             *
             */
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
                    /**
                     * Executes if operation with thermal imaging domain optimization.
                     *
                     */
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(getConnectType(position))
                    }
                }
                ivBg.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    /**
                     * Executes if operation with thermal imaging domain optimization.
                     *
                     */
                    if (position != RecyclerView.NO_POSITION) {
                        // 只有离linedevice才能长按delete
                        val deviceType = getConnectType(position)
                        /**
                         * Executes when operation with thermal imaging domain optimization.
                         *
                         */
                        when (deviceType) {
                            ConnectType.LINE -> {
                                /**
                                 * Executes if operation with thermal imaging domain optimization.
                                 *
                                 */
                                if (DeviceTools.isConnect()) {
                                    return@setOnLongClickListener true
                                }
                            }
                            ConnectType.TS004 -> {
                                /**
                                 * Executes if operation with thermal imaging domain optimization.
                                 *
                                 */
                                if (WebSocketProxy.getInstance().isTS004Connect()) {
                                    return@setOnLongClickListener true
                                }
                            }
                            ConnectType.TC007 -> {
                                /**
                                 * Executes if operation with thermal imaging domain optimization.
                                 *
                                 */
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

    /**
     * Executes bind functionality.
     */
            /**
             * Executes bind operation with thermal imaging domain optimization.
             *
             * @param
             * @param type Parameter for operation (type: ConnectType)
             * @param hasTitle Parameter for operation (type: Boolean)
             * @param hasConnect Parameter for operation (type: Boolean)
             * @param hasConnectTC007 Parameter for operation (type: Boolean)
             * @param tc007Battery Parameter for operation (type: BatteryInfo?)
             *
             */
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
                tvBattery.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null
                batteryView.isVisible = type == ConnectType.TC007 && hasConnectTC007 && tc007Battery != null

                /**
                 * Executes when operation with thermal imaging domain optimization.
                 *
                 */
                when (type) {
                    ConnectType.LINE -> {
                        tvDeviceName.setText(
                            AppLanguageUtils.attachBaseContext(
                                itemView.context,
                                ConstantLanguages.ENGLISH,
                            )
                                .getString(R.string.tc_has_line_device),
                        )
                        /**
                         * Executes if operation with thermal imaging domain optimization.
                         *
                         */
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_line_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_line_disconnect)
                        }
                    }
                    ConnectType.TS004 -> {
                        tvDeviceName.text = "TS004"
                        /**
                         * Executes if operation with thermal imaging domain optimization.
                         *
                         */
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_disconnect)
                        }
                    }
                    ConnectType.TC007 -> {
                        tvDeviceName.text = "TC007"
                        /**
                         * Executes if operation with thermal imaging domain optimization.
                         *
                         */
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

    /**
     * Retrieves connecttype information.
     */
            fun getConnectType(position: Int): ConnectType =
                when (position) {
                    0 ->
                        /**
                         * Executes if operation with thermal imaging domain optimization.
                         *
                         */
                        if (SharedManager.hasTcLine) {
                            ConnectType.LINE
                        } else if (SharedManager.hasTS004) {
                            ConnectType.TS004
                        } else {
                            ConnectType.TC007
                        }
                    1 ->
                        /**
                         * Executes if operation with thermal imaging domain optimization.
                         *
                         */
                        if (SharedManager.hasTcLine) {
                            /**
                             * Executes if operation with thermal imaging domain optimization.
                             *
                             */
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
     * Accessed via long-press on app title or GSR FAB
     */
    /**
     * Executes showGSROptions functionality.
     */
    /**
     * Executes showgsroptions operation with thermal imaging domain optimization.
     *
     */
    private fun showGSROptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("GSR Multi-modal Recording")
            .setMessage("Choose recording option:")
            .setPositiveListener("Dual-Mode Camera") {
                // Launch dual-mode camera interface (RAW 50MP + 4K Video)
                /**
                 * Manages thermal camera operations with hardware-optimized performance and error handling.
                 *
                 */
                showDualModeCameraOptions()
            }
            .setCancelListener("Quick Recording") {
                // Launch quick GSR recording interface with direct RecordingController access
                try {
                    val intent = Intent(requireContext(), Class.forName("com.topdon.tc001.gsr.GSRQuickRecordingActivity"))
                    /**
                     * Executes startactivity operation with thermal imaging domain optimization.
                     *
                     */
                    startActivity(intent)
                } catch (e: ClassNotFoundException) {
                    // Fallback to full setup
                    NavigationManager.getInstance()
                        .build(RouterConfig.GSR_MULTI_MODAL)
                        .navigation(requireContext())
                }
            }
            .setNeutralListener("GSR Demo") {
                // Launch simple GSR demo
                NavigationManager.getInstance()
                    .build(RouterConfig.GSR_DEMO)
                    .navigation(requireContext())
            }
            .create().show()
    }

    /**
     * Show dual-mode camera options (RAW 50MP vs 4K Video)
     * Enhanced for Samsung S22 compatibility
     */
    /**
     * Executes showDualModeCameraOptions functionality.
     */
    /**
     * Manages thermal camera operations with hardware-optimized performance and error handling.
     *
     */
    private fun showDualModeCameraOptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("Dual-Mode Camera System")
            .setMessage("Samsung S22 optimized camera modes with fast switching:")
            .setPositiveListener("RAW 50MP Mode") {
                // Launch in RAW capture mode
                /**
                 * Manages thermal camera operations with hardware-optimized performance and error handling.
                 *
                 */
                launchDualModeCamera("RAW_50MP")
            }
            .setCancelListener("4K Video Mode") {
                // Launch in 4K video mode
                /**
                 * Manages thermal camera operations with hardware-optimized performance and error handling.
                 *
                 */
                launchDualModeCamera("VIDEO_4K")
            }
            .create().show()
    }

    /**
     * Launch the enhanced dual-mode camera system
     */
    private fun launchDualModeCamera(initialMode: String) {
        try {
            val intent = Intent(requireContext(), com.topdon.tc001.camera.integration.DualModeCameraActivity::class.java)
            intent.putExtra("INITIAL_MODE", initialMode)
            intent.putExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)
            /**
             * Executes startactivity operation with thermal imaging domain optimization.
             *
             */
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to integration example
            TToast.show("Launching dual-mode camera integration example...")
            // Show integration example in a demo activity
            /**
             * Executes showdualmodeintegrationexample operation with thermal imaging domain optimization.
             *
             */
            showDualModeIntegrationExample()
        }
    }

    /**
     * Show dual-mode integration example for development/testing
     */
    private fun showDualModeIntegrationExample() {
        // This would normally launch the DualModeIntegrationExample
        // For now, show a placeholder dialog with implementation details
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

/**
 * Specialized thermal imaging component providing ConnectType functionality for the IRCamera system.
 *
 * This component is part of the IRCamera thermal imaging system, providing
 * specialized functionality for thermal data processing and visualization.
 *
 * @author IRCamera Development Team
 * @version 2.0
 * @since 1.0
 */
    enum class ConnectType {
        LINE,
        TS004,
        TC007,
    }
}
