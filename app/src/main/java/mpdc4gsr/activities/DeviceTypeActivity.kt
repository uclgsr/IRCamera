package mpdc4gsr

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityDeviceTypeBinding
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.tools.DeviceTools

class DeviceTypeActivity : BaseBindingActivity<ActivityDeviceTypeBinding>() {

    private var clientType: IRDeviceType? = null

    override fun initContentLayoutId() = R.layout.activity_device_type

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter =
            MyAdapter(this).apply {
                onItemClickListener = {
                    clientType = it
                    when (it) {
                        IRDeviceType.TS004 -> {
                            NavigationManager.getInstance()
                                .build(RouterConfig.IR_DEVICE_ADD)
                                .withBoolean("isTS004", true)
                                .navigation(this@DeviceTypeActivity)
                        }

                        IRDeviceType.TC007 -> {
                            NavigationManager.getInstance()
                                .build(RouterConfig.IR_DEVICE_ADD)
                                .withBoolean("isTS004", false)
                                .navigation(this@DeviceTypeActivity)
                        }

                        IRDeviceType.SHIMMER3_GSR -> {
                            NavigationManager.getInstance()
                                .build(RouterConfig.GSR_MULTI_MODAL)
                                .navigation(this@DeviceTypeActivity)
                            finish()
                        }

                        IRDeviceType.PC_CONTROLLER -> {

                            mpdc4gsr.network.DevicePairingActivity.start(this@DeviceTypeActivity)
                        }

                        else -> {
                            NavigationManager.getInstance()
                                .build(RouterConfig.IR_MAIN)
                                .withBoolean(ExtraKeyConfig.IS_TC007, false)
                                .navigation(this@DeviceTypeActivity)
                            if (DeviceTools.isConnect()) {
                                finish()
                            }
                        }
                    }
                }
            }
    }

    private fun initData() {
    }

    override fun connected() {
        if (clientType?.isLine() == true) {
            finish()
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTS004) {
            if (clientType == IRDeviceType.TS004) {
                finish()
            }
        } else {
            if (clientType == IRDeviceType.TC007) {
                finish()
            }
        }
    }

    private class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        var onItemClickListener: ((type: IRDeviceType) -> Unit)? = null

        private data class ItemInfo(
            val isTitle: Boolean,
            val firstType: IRDeviceType,
            val secondType: IRDeviceType?
        )

        private val dataList: ArrayList<ItemInfo> =
            arrayListOf(
                ItemInfo(true, IRDeviceType.TS001, IRDeviceType.TC001),
                ItemInfo(false, IRDeviceType.TC001_PLUS, IRDeviceType.TC002C_DUO),


                ItemInfo(true, IRDeviceType.TS004, null),
                ItemInfo(true, IRDeviceType.SHIMMER3_GSR, null),
                ItemInfo(true, IRDeviceType.PC_CONTROLLER, null),
            )

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_device_type, parent, false)
            )
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val firstType: IRDeviceType = dataList[position].firstType
            val secondType: IRDeviceType? = dataList[position].secondType
            val tvTitle = holder.itemView.findViewById<TextView>(R.id.tv_title)
            tvTitle.isVisible = dataList[position].isTitle
            tvTitle.text =
                context.getString(
                    when (firstType) {
                        IRDeviceType.SHIMMER3_GSR -> R.string.tc_connect_bluetooth
                        IRDeviceType.PC_CONTROLLER -> R.string.tc_connect_wifi
                        else -> if (firstType.isLine()) R.string.tc_connect_line else R.string.tc_connect_wifi
                    },
                )

            val tvItem1 = holder.itemView.findViewById<TextView>(R.id.tv_item1)
            tvItem1.text = firstType.getDeviceName()
            when (firstType) {

                IRDeviceType.TC001 ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_tc001)

                IRDeviceType.TC001_PLUS ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_tc001_plus)

                IRDeviceType.TC002C_DUO ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_tc001_plus)

                IRDeviceType.TC007 ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_tc007)

                IRDeviceType.TS001 ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_ts001)

                IRDeviceType.TS004 ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_ts004)

                IRDeviceType.SHIMMER3_GSR ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_shimmer_gsr)

                IRDeviceType.PC_CONTROLLER ->
                    holder.itemView.findViewById<android.widget.ImageView>(
                        R.id.iv_item1,
                    ).setImageResource(R.drawable.ic_device_type_pc)
            }

            holder.itemView.findViewById<ViewGroup>(R.id.group_item2).isVisible = secondType != null
            if (secondType != null) {
                val tvItem2 = holder.itemView.findViewById<TextView>(R.id.tv_item2)
                tvItem2.text = secondType.getDeviceName()
                when (secondType) {

                    IRDeviceType.TC001 ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_tc001)

                    IRDeviceType.TC001_PLUS ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_tc001_plus)

                    IRDeviceType.TC002C_DUO ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_tc001_plus)

                    IRDeviceType.TC007 ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_tc007)

                    IRDeviceType.TS001 ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_ts001)

                    IRDeviceType.TS004 ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_ts004)

                    IRDeviceType.SHIMMER3_GSR ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_shimmer_gsr)

                    IRDeviceType.PC_CONTROLLER ->
                        holder.itemView.findViewById<android.widget.ImageView>(
                            R.id.iv_item2,
                        ).setImageResource(R.drawable.ic_device_type_pc)
                }
            }
        }

        override fun getItemCount(): Int = dataList.size

        inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            init {
                rootView.findViewById<View>(R.id.view_bg_item1).setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(dataList[position].firstType)
                    }
                }
                rootView.findViewById<View>(R.id.view_bg_item2).setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val irDeviceType: IRDeviceType =
                            dataList[position].secondType ?: return@setOnClickListener
                        onItemClickListener?.invoke(irDeviceType)
                    }
                }
            }
        }
    }

    enum class IRDeviceType {
        TC001 {
            override fun isLine(): Boolean = true

            override fun getDeviceName(): String = "TC001"
        },
        TC001_PLUS {
            override fun isLine(): Boolean = true

            override fun getDeviceName(): String = "TC001 Plus"
        },
        TC002C_DUO {
            override fun isLine(): Boolean = true

            override fun getDeviceName(): String = "TC002C Duo"
        },
        TC007 {
            override fun isLine(): Boolean = false

            override fun getDeviceName(): String = "TC007"
        },
        TS001 {
            override fun isLine(): Boolean = true

            override fun getDeviceName(): String = "TS001"
        },
        TS004 {
            override fun isLine(): Boolean = false

            override fun getDeviceName(): String = "TS004"
        },
        SHIMMER3_GSR {
            override fun isLine(): Boolean = false 

            override fun getDeviceName(): String = "Shimmer3 GSR"
        },
        PC_CONTROLLER {
            override fun isLine(): Boolean = false 

            override fun getDeviceName(): String = "PC Controller"
        }, ;

        abstract fun isLine(): Boolean

        abstract fun getDeviceName(): String
    }
}
