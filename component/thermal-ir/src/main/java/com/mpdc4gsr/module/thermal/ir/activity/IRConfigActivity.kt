package com.mpdc4gsr.module.thermal.ir.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.repository.TC007Repository
import com.mpdc4gsr.lib.core.socket.WebSocketProxy
import com.mpdc4gsr.lib.core.tools.NumberTools
import com.mpdc4gsr.lib.core.tools.UnitTools
import com.mpdc4gsr.lib.ui.widget.MyItemDecoration
import com.mpdc4gsr.lms.sdk.weiget.TToast
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.adapter.ConfigEmAdapter
import com.mpdc4gsr.module.thermal.ir.bean.DataBean
import com.mpdc4gsr.module.thermal.ir.bean.ModelBean
import com.mpdc4gsr.module.thermal.ir.dialog.ConfigGuideDialog
import com.mpdc4gsr.module.thermal.ir.dialog.IRConfigInputDialog
import com.mpdc4gsr.module.thermal.ir.repository.ConfigRepository
import com.mpdc4gsr.module.thermal.ir.viewmodel.IRConfigViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mpdc4gsr.lib.core.R as LibR




class IRConfigActivity : BaseActivity(), View.OnClickListener {
    
    private var isTC007 = false

    private val viewModel: IRConfigViewModel by viewModels()

    private lateinit var adapter: ConfigAdapter

    override fun initContentView(): Int = R.layout.activity_ir_config

    @SuppressLint("SetTextI18n")
    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        val tvDefaultTempTitle = findViewById<android.widget.TextView>(R.id.tv_default_temp_title)
        val tvDefaultDisTitle = findViewById<android.widget.TextView>(R.id.tv_default_dis_title)
        val tvDefaultEmTitle = findViewById<android.widget.TextView>(R.id.tv_default_em_title)
        val tvDefaultTempUnit = findViewById<android.widget.TextView>(R.id.tv_default_temp_unit)
        val ivDefaultSelector = findViewById<android.widget.ImageView>(R.id.iv_default_selector)
        val viewDefaultTempBg = findViewById<android.view.View>(R.id.view_default_temp_bg)
        val viewDefaultDisBg = findViewById<android.view.View>(R.id.view_default_dis_bg)
        val tvDefaultEmValue = findViewById<android.widget.TextView>(R.id.tv_default_em_value)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val tvDefaultTempValue = findViewById<android.widget.TextView>(R.id.tv_default_temp_value)
        val tvDefaultDisValue = findViewById<android.widget.TextView>(R.id.tv_default_dis_value)

        tvDefaultTempTitle.text = "${getString(LibR.string.thermal_config_environment)} ${
            UnitTools.showConfigC(
                -10,
                if (isTC007) 50 else 55
            )
        }"
        tvDefaultDisTitle.text =
            "${getString(LibR.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
        tvDefaultEmTitle.text =
            "${getString(LibR.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
        tvDefaultTempUnit.text = UnitTools.showUnit()

        ivDefaultSelector.setOnClickListener(this)
        viewDefaultTempBg.setOnClickListener(this)
        viewDefaultDisBg.setOnClickListener(this)
        tvDefaultEmValue.setOnClickListener(this)

        adapter = ConfigAdapter(this, isTC007)
        adapter.onSelectListener = {
            viewModel.checkConfig(isTC007, it)
        }
        adapter.onDeleteListener = {
            TipDialog.Builder(this)
                .setMessage(
                    getString(
                        LibR.string.tip_config_delete,
                        "${getString(LibR.string.thermal_custom_mode)}${it.name}"
                    )
                )
                .setPositiveListener(LibR.string.app_confirm) {
                    viewModel.deleteConfig(isTC007, it.id)
                }
                .setCancelListener(LibR.string.app_cancel)
                .create().show()
        }
        adapter.onUpdateListener = {
            viewModel.updateCustom(isTC007, it)
        }
        adapter.onAddListener =
            View.OnClickListener {
                TipDialog.Builder(this)
                    .setMessage(LibR.string.tip_myself_model)
                    .setPositiveListener(LibR.string.app_confirm) {
                        viewModel.addConfig(isTC007)
                    }
                    .setCancelListener(LibR.string.app_cancel)
                    .create().show()
            }

        val itemDecoration = MyItemDecoration(this)
        itemDecoration.wholeBottom = 20f

        recyclerView.addItemDecoration(itemDecoration)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ConcatAdapter(adapter, ConfigEmAdapter(this))

        viewModel.configLiveData.observe(this) {

            tvDefaultTempValue.text =
                NumberTools.to02(UnitTools.showUnitValue(it.defaultModel.environment))
            tvDefaultDisValue.text = NumberTools.to02(it.defaultModel.distance)
            tvDefaultEmValue.text = NumberTools.to02(it.defaultModel.radiation)
            ivDefaultSelector.isSelected = true

            showGuideDialog(it)

            if (isTC007 && WebSocketProxy.getInstance().isTC007Connect()) {
                lifecycleScope.launch {
                    val config = ConfigRepository.readConfig(true)
                    TC007Repository.setIRConfig(
                        config.environment,
                        config.distance,
                        config.radiation
                    )
                }
            }
        }
        viewModel.getConfig(isTC007)
    }

    override fun initData() {
    }

    private fun showGuideDialog(modelBean: ModelBean) {
        val ivDefaultSelector = findViewById<android.widget.ImageView>(R.id.iv_default_selector)
        val llRoot = findViewById<android.widget.LinearLayout>(R.id.ll_root)

        if (SharedManager.configGuideStep == 0) { 
            ivDefaultSelector.isSelected = modelBean.defaultModel.use
            adapter.refresh(modelBean.myselfModel)
            return
        }
        val guideDialog = ConfigGuideDialog(this, isTC007, modelBean.defaultModel)
        guideDialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(null)
            }
            ivDefaultSelector.isSelected = modelBean.defaultModel.use
            adapter.refresh(modelBean.myselfModel)
        }
        guideDialog.show()

        if (Build.VERSION.SDK_INT >= 31) {
            window?.decorView?.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20f,
                    20f,
                    Shader.TileMode.MIRROR
                )
            )
        } else {
            lifecycleScope.launch {

                delay(100)
                guideDialog.blurBg(llRoot)
            }
        }
    }

    override fun onClick(v: View?) {
        val ivDefaultSelector = findViewById<android.widget.ImageView>(R.id.iv_default_selector)
        val viewDefaultTempBg = findViewById<android.view.View>(R.id.view_default_temp_bg)
        val viewDefaultDisBg = findViewById<android.view.View>(R.id.view_default_dis_bg)
        val tvDefaultEmValue = findViewById<android.widget.TextView>(R.id.tv_default_em_value)

        when (v) {
            ivDefaultSelector -> { 
                viewModel.checkConfig(isTC007, 0)
            }

            viewDefaultTempBg -> { 
                IRConfigInputDialog(this, IRConfigInputDialog.Type.TEMP, isTC007)
                    .setInput(UnitTools.showUnitValue(viewModel.configLiveData.value?.defaultModel?.environment!!))
                    .setConfirmListener {
                        viewModel.updateDefaultEnvironment(isTC007, UnitTools.showToCValue(it))
                    }
                    .show()
            }

            viewDefaultDisBg -> { 
                IRConfigInputDialog(this, IRConfigInputDialog.Type.DIS, isTC007)
                    .setInput(viewModel.configLiveData.value?.defaultModel?.distance)
                    .setConfirmListener {
                        viewModel.updateDefaultDistance(isTC007, it)
                    }
                    .show()
            }

            tvDefaultEmValue -> { 
                IRConfigInputDialog(this, IRConfigInputDialog.Type.EM, isTC007)
                    .setInput(viewModel.configLiveData.value?.defaultModel?.radiation)
                    .setConfirmListener {
                        viewModel.updateDefaultRadiation(isTC007, it)
                    }
                    .show()
            }
        }
    }

    private class ConfigAdapter(val context: Context, val isTC007: Boolean) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val dataList: ArrayList<DataBean> = ArrayList()

        var onSelectListener: ((id: Int) -> Unit)? = null

        var onDeleteListener: ((bean: DataBean) -> Unit)? = null

        var onUpdateListener: ((bean: DataBean) -> Unit)? = null

        var onAddListener: View.OnClickListener? = null

        @SuppressLint("NotifyDataSetChanged")
        fun refresh(newList: List<DataBean>) {
            dataList.clear()
            dataList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < dataList.size) 0 else 1
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                ItemViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_ir_config_config, parent, false)
                )
            } else {
                FootViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_ir_config_foot, parent, false)
                )
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (holder is ItemViewHolder) {
                val dataBean = dataList[position]
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_name).text =
                    "${context.getString(LibR.string.thermal_custom_mode)}${dataBean.name}"
                holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_selector).isSelected =
                    dataBean.use

                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_temp_title).text =
                    "${context.getString(LibR.string.thermal_config_environment)} ${
                        UnitTools.showConfigC(
                            -10,
                            if (isTC007) 50 else 55
                        )
                    }"
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_dis_title).text =
                    "${context.getString(LibR.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_em_title).text =
                    "${context.getString(LibR.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_temp_unit).text =
                    UnitTools.showUnit()

                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_temp_value).text =
                    NumberTools.to02(UnitTools.showUnitValue(dataBean.environment))
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_dis_value).text =
                    NumberTools.to02(dataBean.distance)
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_em_value).text =
                    NumberTools.to02(dataBean.radiation)
            } else if (holder is FootViewHolder) {
                holder.itemView.findViewById<android.widget.TextView>(
                    R.id.tv_add,
                ).setTextColor(if (dataList.size >= 10) 0x80ffffff.toInt() else 0xccffffff.toInt())
            }
        }

        override fun getItemCount(): Int = dataList.size + 1

        inner class ItemViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            init {
                rootView.findViewById<android.widget.ImageView>(R.id.iv_selector)
                    .setOnClickListener {
                        val position: Int = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onSelectListener?.invoke(dataList[position].id)
                        }
                    }
                rootView.findViewById<android.widget.ImageView>(R.id.iv_del).setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteListener?.invoke(dataList[position])
                    }
                }
                rootView.findViewById<android.view.View>(R.id.view_temp_bg).setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        IRConfigInputDialog(context, IRConfigInputDialog.Type.TEMP, isTC007)
                            .setInput(UnitTools.showUnitValue(dataList[position].environment))
                            .setConfirmListener {
                                itemView.findViewById<android.widget.TextView>(R.id.tv_temp_value).text =
                                    NumberTools.to02(UnitTools.showToCValue(it))
                                dataList[position].environment = UnitTools.showToCValue(it)
                                onUpdateListener?.invoke(dataList[position])
                            }
                            .show()
                    }
                }
                rootView.findViewById<android.view.View>(R.id.view_dis_bg).setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        IRConfigInputDialog(context, IRConfigInputDialog.Type.DIS, isTC007)
                            .setInput(dataList[position].distance)
                            .setConfirmListener {
                                itemView.findViewById<android.widget.TextView>(R.id.tv_dis_value).text =
                                    it.toString()
                                dataList[position].distance = it
                                onUpdateListener?.invoke(dataList[position])
                            }
                            .show()
                    }
                }
                rootView.findViewById<android.widget.TextView>(R.id.tv_em_value)
                    .setOnClickListener {
                        val position: Int = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            IRConfigInputDialog(context, IRConfigInputDialog.Type.EM, isTC007)
                                .setInput(dataList[position].radiation)
                                .setConfirmListener {
                                    itemView.findViewById<android.widget.TextView>(R.id.tv_em_value).text =
                                        it.toString()
                                    dataList[position].radiation = it
                                    onUpdateListener?.invoke(dataList[position])
                                }
                                .show()
                        }
                    }
            }
        }

        inner class FootViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            init {
                rootView.findViewById<android.view.View>(R.id.view_add).setOnClickListener {
                    if (dataList.size < 10) {
                        val position: Int = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onAddListener?.onClick(it)
                        }
                    } else {
                        TToast.shortToast(context, com.mpdc4gsr.lib.core.R.string.config_add_tip)
                    }
                }
                rootView.findViewById<TextView>(R.id.tv_all_emissivity).setOnClickListener {
                    context.startActivity(Intent(context, IREmissivityActivity::class.java))
                }
            }
        }
    }
}
