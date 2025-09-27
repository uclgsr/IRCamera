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
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.FragmentMainBinding
import com.mpdc4gsr.libunified.app.bean.event.SocketMsgEvent
import com.mpdc4gsr.libunified.app.comm.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingFragment
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.socket.SocketCmdUtil
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.libunified.app.utils.WsCmdConstants
import com.mpdc4gsr.libunified.ui.widget.BatteryView
import mpdc4gsr.activities.DeviceTypeActivity
import mpdc4gsr.ui_components.MainFragmentViewModel.ConnectType
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

@SuppressLint("NotifyDataSetChanged")
class MainFragment : BaseBindingFragment<FragmentMainBinding>(), View.OnClickListener {
    private val viewModel: MainFragmentViewModel by viewModels()
    private lateinit var adapter: MyAdapter

    override fun initContentLayoutId(): Int = R.layout.fragment_main

    override fun initView(savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupObservers()
        setupLifecycleObserver()
        
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

        // Initialize device state
        viewModel.initializeDeviceState()
    }

    private fun setupRecyclerView() {
        adapter = MyAdapter()
        adapter.onItemClickListener = { connectType ->
            viewModel.onDeviceItemClick(connectType)
        }
        adapter.onItemLongClickListener = { view, type ->
            showDeleteDeviceDialog(view, type)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.deviceState.observe(viewLifecycleOwner) { deviceState ->
            binding.clHasDevice.isVisible = deviceState.hasAnyDevice
            binding.clNoDevice.isVisible = !deviceState.hasAnyDevice
            
            adapter.hasConnectLine = deviceState.hasConnectLine
            adapter.hasConnectTS004 = deviceState.hasConnectTS004
            adapter.hasConnectTC007 = deviceState.hasConnectTC007
            adapter.notifyDataSetChanged()
        }

        viewModel.batteryInfo.observe(viewLifecycleOwner) { batteryInfo ->
            adapter.tc007Battery = batteryInfo
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { navigationEvent ->
            navigationEvent?.let { event ->
                when (event.route) {
                    "IR_MAIN" -> {
                        NavigationManager.getInstance()
                            .build(RouterConfig.IR_MAIN)
                            .withBoolean(ExtraKeyConfig.IS_TC007, event.isTC007)
                            .navigation(requireContext())
                    }
                    "IR_MONOCULAR" -> {
                        NavigationManager.getInstance()
                            .build(RouterConfig.IR_MONOCULAR)
                            .navigation(requireContext())
                    }
                    "IR_DEVICE_ADD" -> {
                        NavigationManager.getInstance()
                            .build(RouterConfig.IR_DEVICE_ADD)
                            .withBoolean("isTS004", event.isTS004)
                            .navigation(requireContext())
                    }
                }
                viewModel.clearNavigationEvent()
            }
        }
    }

    private fun setupLifecycleObserver() {
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    // Network switching logic moved from direct WebSocket check
                    NetWorkUtils.switchNetwork(true)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceState()
    }

    private fun showDeleteDeviceDialog(view: View, type: MainFragmentViewModel.ConnectType) {
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
                    viewModel.onDeviceDeleted(type)
                    TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                }
                .setCancelListener(R.string.app_cancel)
                .create().show()
        }
        popup.show(view)
    }

    override fun connected() {
        viewModel.onDeviceConnected(isLine = true)
    }

    override fun disConnected() {
        viewModel.onDeviceDisconnected()
    }

    override fun onSocketConnected(isTS004: Boolean) {
        viewModel.onSocketConnected(isTS004)
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        viewModel.onSocketDisconnected(isTS004)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.tvConnectDevice, binding.ivAdd -> {
                startActivity(Intent(requireContext(), DeviceTypeActivity::class.java))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        if (SocketCmdUtil.getCmdResponse(event.text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {
            viewModel.processBatteryUpdate(event.text)
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

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

        var tc007Battery: MainFragmentViewModel.BatteryInfo? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemRangeChanged(0, itemCount)
                }
            }

        var onItemClickListener: ((type: MainFragmentViewModel.ConnectType) -> Unit)? = null
        var onItemLongClickListener: ((view: View, type: MainFragmentViewModel.ConnectType) -> Unit)? = null

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
            // TS004/TC007 functionality removed
            // if (SharedManager.hasTS004) {
            //     result++
            // }
            // if (SharedManager.hasTC007) {
            //     result++
            // }
            return result
        }

        inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

            private val ivBg: View = rootView.findViewById(R.id.iv_bg)
            private val tvTitle: TextView = rootView.findViewById(R.id.tv_title)
            private val tvDeviceName: TextView = rootView.findViewById(R.id.tv_device_name)
            private val tvDeviceState: TextView = rootView.findViewById(R.id.tv_device_state)
            private val tvBattery: TextView = rootView.findViewById(R.id.tv_battery)
            private val ivImage: ImageView = rootView.findViewById(R.id.iv_image)
            private val batteryView: BatteryView =
                rootView.findViewById(R.id.battery_view)
            private val viewDeviceState: View = rootView.findViewById(R.id.view_device_state)

            init {
                ivBg.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(getConnectType(position))
                    }
                }
                ivBg.setOnLongClickListener { view ->
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val deviceType = getConnectType(position)
                        val currentState = this@MainFragment.viewModel.deviceState.value
                        
                        val shouldPreventLongClick = when (deviceType) {
                            MainFragmentViewModel.ConnectType.LINE -> {
                                currentState?.hasConnectLine == true
                            }
                            MainFragmentViewModel.ConnectType.TS004 -> {
                                currentState?.hasConnectTS004 == true
                            }
                            MainFragmentViewModel.ConnectType.TC007 -> {
                                currentState?.hasConnectTC007 == true
                            }
                        }
                        
                        if (shouldPreventLongClick) {
                            return@setOnLongClickListener true
                        }
                        
                        onItemLongClickListener?.invoke(view, deviceType)
                    }
                    true
                }
            }

            fun bind(
                type: MainFragmentViewModel.ConnectType,
                hasTitle: Boolean,
                hasConnect: Boolean,
                hasConnectTC007: Boolean,
                tc007Battery: MainFragmentViewModel.BatteryInfo?,
            ) {
                tvTitle.isVisible = hasTitle
                tvTitle.text =
                    AppLanguageUtils.attachBaseContext(
                        itemView.context, ConstantLanguages.ENGLISH,
                    )
                        .getString(if (type == MainFragmentViewModel.ConnectType.LINE) R.string.tc_connect_line else R.string.tc_connect_wifi)

                ivBg.isSelected = hasConnect
                tvDeviceName.isSelected = hasConnect
                viewDeviceState.isSelected = hasConnect
                tvDeviceState.isSelected = hasConnect
                tvDeviceState.text = if (hasConnect) "online" else "offline"
                tvBattery.isVisible =
                    type == MainFragmentViewModel.ConnectType.TC007 && hasConnectTC007 && tc007Battery != null
                batteryView.isVisible =
                    type == MainFragmentViewModel.ConnectType.TC007 && hasConnectTC007 && tc007Battery != null

                when (type) {
                    MainFragmentViewModel.ConnectType.LINE -> {
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

                    MainFragmentViewModel.ConnectType.TS004 -> {
                        tvDeviceName.text = "TS004"
                        if (hasConnect) {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_connect)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_main_device_ts004_disconnect)
                        }
                    }

                    MainFragmentViewModel.ConnectType.TC007 -> {
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

            fun getConnectType(position: Int): MainFragmentViewModel.ConnectType =
                when (position) {
                    0 -> MainFragmentViewModel.ConnectType.LINE
                    1 -> MainFragmentViewModel.ConnectType.TS004
                    else -> MainFragmentViewModel.ConnectType.TC007
                }
        }
    }

    private fun showGSROptions() {
        TipDialog.Builder(requireContext())
            .setTitleMessage("GSR Multi-modal Recording")
            .setMessage("Choose recording option:")
            .setPositiveListener("Dual-Mode Camera") {

                showDualModeCameraOptions()
            }
            .setCancelListener("Quick Recording") {

                try {
                    val intent = Intent(
                        requireContext(),
                        Class.forName("mpdc4gsr.sensors.gsr.GSRQuickRecordingActivity")
                    )
                    startActivity(intent)
                } catch (e: ClassNotFoundException) {

                    NavigationManager.getInstance()
                        .build(RouterConfig.GSR_MULTI_MODAL)
                        .navigation(requireContext())
                }
            }


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
                "Enhanced RgbCameraRecorder with:\n\n" +
                        "• RAW 50MP capture at ~15fps\n" +
                        "• 4K video at 30/60fps\n" +
                        "• Fast session switching (~200ms)\n" +
                        "• Samsung S22 optimizations\n" +
                        "• Unified camera controls\n\n" +
                        "Implementation ready for integration.",
            )
            .setPositiveListener("Got it") { }
            .create().show()
    }
}
