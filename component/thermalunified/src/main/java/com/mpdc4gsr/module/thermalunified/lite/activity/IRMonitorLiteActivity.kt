package com.mpdc4gsr.module.thermalunified.lite.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.energy.ac020library.bean.IrcmdError
import com.energy.irutilslibrary.LibIRTempAC020
import com.energy.irutilslibrary.bean.GainStatus
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.utils.Line
import com.mpdc4gsr.module.thermalunified.lite.R
import com.mpdc4gsr.module.thermalunified.lite.camera.DeviceIrcmdControlManager
import com.mpdc4gsr.module.thermalunified.lite.databinding.ActivityIrMonitorLiteBinding
import com.mpdc4gsr.module.thermalunified.lite.fragment.IRMonitorLiteFragment
import com.google.gson.Gson
import com.infisense.usbir.view.ITsTempListener
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.bean.tools.ThermalBean
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.db.AppDatabase
import com.mpdc4gsr.lib.core.db.entity.ThermalEntity
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.tools.NumberTools
import com.mpdc4gsr.lib.core.tools.TimeTool
import com.mpdc4gsr.lib.ui.dialog.MonitorSelectDialog
import com.mpdc4gsr.lib.ui.listener.SingleClickListener
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import com.mpdc4gsr.module.thermalunified.bean.SelectPositionBean
import com.mpdc4gsr.module.thermalunified.event.MonitorSaveEvent
import com.mpdc4gsr.module.thermalunified.event.ThermalActionEvent
import com.mpdc4gsr.module.thermalunified.repository.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.math.RoundingMode

open class IRMonitorLiteActivity : BaseActivity(), View.OnClickListener, ITsTempListener {
    private lateinit var binding: ActivityIrMonitorLiteBinding
    private var selectIndex: SelectPositionBean? = null
    val irMonitorLiteFragment = IRMonitorLiteFragment()
    private val bean = ThermalBean()
    private var selectBean: SelectPositionBean = SelectPositionBean()

    override fun initContentView() = R.layout.activity_ir_monitor_lite

    override fun initView() {
        binding = ActivityIrMonitorLiteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.motionBtn.setOnClickListener(
            object : SingleClickListener() {
                override fun onSingleClick() {
                    MonitorSelectDialog.Builder(this@IRMonitorLiteActivity)
                        .setPositiveListener {
                            updateUI()
                            when (it) {
                                1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                                2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                                else -> EventBus.getDefault()
                                    .post(ThermalActionEvent(action = 2003))
                            }
                        }
                        .create().show()
                }
            },
        )
        binding.motionStartBtn.setOnClickListener(this)
    }

    private fun startChart() {
        if (selectIndex == null) {
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        selectBean = selectIndex!!
        if (showTask != null && showTask!!.isActive) {
            showTask!!.cancel()
            showTask = null
        }
        binding.titleView.setRightText(R.string.monitor_finish)
        binding.titleView.setRightClickListener {
            recordJob?.cancel()
            lifecycleScope.launch {
                delay(500)
                finish()
            }
        }
        showTask =
            lifecycleScope.launch {
                var isFirstRead = true
                var errorReadCount = 0
                while (true) {
                    delay(1000)
                    @Suppress("SENSELESS_COMPARISON")
                    if (irMonitorLiteFragment != null) {
                        val result: LibIRTemp.TemperatureSampleResult =
                            when (selectBean.type) {
                                1 -> irMonitorLiteFragment!!.temperatureView.getPointTemp(selectBean.startPosition)
                                2 -> irMonitorLiteFragment!!.temperatureView.getLineTemp(
                                    Line(
                                        selectBean.startPosition,
                                        selectBean.endPosition
                                    )
                                )

                                else -> irMonitorLiteFragment!!.temperatureView.getRectTemp(
                                    selectBean.getRect()
                                )
                            } ?: continue
                        if (isFirstRead) {
                            if (result.maxTemperature > 200f || result.minTemperature < -200f) {
                                errorReadCount++
                                XLog.w("[ph] $errorReadCount [ph][ph][ph][ph][ph][ph][ph][ph]，max = ${result.maxTemperature} min = ${result.minTemperature}")
                                if (errorReadCount > 10) {
                                    XLog.i("[ph][ph]10[ph][ph][ph][ph][ph][ph][ph][ph]，[ph][ph][ph][ph][ph][ph][ph][ph]")
                                    isFirstRead = false
                                }
                                continue
                            } else {
                                isFirstRead = false
                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding.llTime.isVisible = true
                                }
                            }
                        }
                        if (result.maxTemperature >= -270f) {
                            val maxBigDecimal =
                                BigDecimal.valueOf(tempCorrectByTs(result.maxTemperature).toDouble())
                            val minBigDecimal =
                                BigDecimal.valueOf(tempCorrectByTs(result.minTemperature).toDouble())
                            bean.centerTemp =
                                maxBigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                            bean.maxTemp = maxBigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                            bean.minTemp = minBigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                            bean.createTime = System.currentTimeMillis()
                            canUpdate = true
                        }
                    }
                }
            }

        binding.monitorCurrentVol.text =
            getString(if (selectIndex!!.type == 1) R.string.chart_temperature else R.string.chart_temperature_high)
        binding.monitorRealVol.visibility = if (selectIndex!!.type == 1) View.GONE else View.VISIBLE
        binding.monitorRealImg.visibility = if (selectIndex!!.type == 1) View.GONE else View.VISIBLE
        recordThermal()
    }

    private var showTask: Job? = null

    private var isRecord = false
    private var timeMillis = 1000L
    private var canUpdate = false

    private var recordJob: Job? = null

    private fun recordThermal() {
        recordJob =
            lifecycleScope.launch(Dispatchers.IO) {
                isRecord = true
                val thermalId = TimeTool.showDateSecond()
                val startTime = System.currentTimeMillis()
                val typeStr =
                    when (selectBean.type) {
                        1 -> "point"
                        2 -> "line"
                        else -> "fence"
                    }
                var time = 0L
                while (isRecord) {
                    if (canUpdate) {
                        val entity = ThermalEntity()
                        entity.userId = SharedManager.getUserId()
                        entity.thermalId = thermalId
                        entity.thermal = NumberTools.to02f(bean.centerTemp)
                        entity.thermalMax = NumberTools.to02f(bean.maxTemp)
                        entity.thermalMin = NumberTools.to02f(bean.minTemp)
                        entity.type = typeStr
                        entity.startTime = startTime
                        entity.createTime = System.currentTimeMillis()
                        AppDatabase.getInstance().thermalDao().insert(entity)
                        time++
                        launch(Dispatchers.Main) {
                            binding.mpChartView.addPointToChart(
                                bean = entity,
                                selectType = selectBean.type
                            )
                        }
                        delay(timeMillis)
                    } else {
                        delay(100)
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.tvTime.text =
                            TimeTool.showVideoLongTime(System.currentTimeMillis() - startTime)
                    }
                }
                XLog.w("[ph][ph][ph][ph], [ph][ph][ph]:$time")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().add(R.id.thermal_fragment, irMonitorLiteFragment)
            .commit()
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.motion_start_btn -> {
                if (selectIndex == null) {
                    MonitorSelectDialog.Builder(this)
                        .setPositiveListener {
                            updateUI()
                            when (it) {
                                1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                                2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                                else -> EventBus.getDefault()
                                    .post(ThermalActionEvent(action = 2003))
                            }
                        }
                        .create().show()
                    return
                }
                lifecycleScope.launch {
                    if (irMonitorLiteFragment.frameReady) {
                        lifecycleScope.launch {
                            if (selectIndex == null) {
                                return@launch
                            }
                            irMonitorLiteFragment?.stopTask()
                            binding.thermalFragment.getViewTreeObserver().addOnGlobalLayoutListener(
                                object :
                                    ViewTreeObserver.OnGlobalLayoutListener {
                                    override fun onGlobalLayout() {

                                        binding.thermalFragment.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this)
                                        irMonitorLiteFragment?.restTempView()
                                        irMonitorLiteFragment?.addTempLine(selectIndex!!)

                                    }
                                },
                            )
                            binding.motionActionLay.isVisible = false
                            binding.chartLay.isVisible = true
                            showCameraLoading()
                            delay(500)
                            dismissCameraLoading()
                            startChart()
                        }
                    }
                }
            }
        }
    }

    fun select(selectIndex: SelectPositionBean?) {
        this.selectIndex = selectIndex
        XLog.i("[ph][ph][ph][ph][ph][ph]：${Gson().toJson(selectIndex)}")
    }

    private fun updateUI() {
        binding.motionStartBtn.visibility = View.VISIBLE
        binding.motionBtn.visibility = View.GONE
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }

    var config: DataBean? = null
    val basicGainGetValue = IntArray(1)
    var basicGainGetTime = 0L

    override fun tempCorrectByTs(temp: Float?): Float {
        var tempNew = temp
        try {
            if (config == null) {
                config = ConfigRepository.readConfig(false)
            }
            val defModel = DataBean()
            if (config!!.radiation == defModel.radiation &&
                defModel.environment == config!!.environment &&
                defModel.distance == config!!.distance
            ) {
                return temp!!
            }

            if (System.currentTimeMillis() - basicGainGetTime > 5000L) {
                try {
                    val basicGainGet: IrcmdError? =
                        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                            ?.basicGainGet(basicGainGetValue)
                } catch (e: Exception) {
                    XLog.e("[ph][ph][ph][ph][ph][ph]")
                }
                basicGainGetTime = System.currentTimeMillis()
            }
            val params_array =
                floatArrayOf(
                    temp!!,
                    config!!.radiation,
                    config!!.environment,
                    config!!.environment,
                    config!!.distance,
                    0.8f,
                )
            if (BaseApplication.instance.tau_data_H == null || BaseApplication.instance.tau_data_L == null) return temp
            tempNew =
                LibIRTempAC020.temperatureCorrection(
                    params_array[0],
                    BaseApplication.instance.tau_data_H,
                    BaseApplication.instance.tau_data_L,
                    params_array[1],
                    params_array[2],
                    params_array[3],
                    params_array[4],
                    params_array[5],
                    if (basicGainGetValue[0] == 0) GainStatus.LOW_GAIN else GainStatus.HIGH_GAIN,
                )
            Log.i(
                TAG,
                "temp correct,${basicGainGetValue[0]} oldTemp = " + params_array[0] + "newtemp = " + tempNew +
                        " ems = " + params_array[1] + " ta = " + params_array[2] + " " +
                        "distance = " + params_array[4] + " hum = " + params_array[5],
            )
        } catch (e: Exception) {
            XLog.e("$TAG--[ph][ph][ph][ph][ph][ph]：${e.message}")
        } finally {
            return tempNew ?: 0f
        }
    }

    override fun finish() {
        super.finish()
        if (isRecord) {
            EventBus.getDefault().post(MonitorSaveEvent())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        showTask?.cancel()
        recordJob?.cancel()
    }
}
