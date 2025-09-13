package com.topdon.module.thermal.ir.utils

import android.util.Log
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap

/**
 * Calibration tools for thermal imaging processing.
 * Contains specialized algorithms and processing functions.
 */
object CalibrationTools {
    /**
     * Single point calibration
     * Aim at blackbody - set temperature
     */
    fun sign(
        irCmd: IRCMD,
        singlePointTemp: Int,
    ): Boolean {
        var success = false
calibration前需要resettemperature measurement parameters,否则temperaturecalibration inaccuracy
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD)
            val result = irCmd.setTPDKtBtRecalPoint(CommonParams.TPDKtBtRecalPointType.RECAL_1_POINT, singlePointTemp)
            if (result == 0) {
                success = true
            } else {
                XLog.w("Single point calibration failed")
            }
        } else {
            XLog.w("Single point calibration failed")
        }
        return success
    }

    /**
     * Temperature calibration
     * Low temperature (100 ~ 400)
     */
    fun pointFirst(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
calibration前需要resettemperature measurement parameters,否则temperaturecalibration inaccuracy
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_FIRST, pointTemp + 273)
            if (result == 0) {
                success = true
            } else {
                XLog.w("低温calibrationfailed")
            }
        } else {
            XLog.w("低温calibrationfailed")
        }
        return success
    }

    /**
temperaturecalibration
high temperature(20 ~ 100)
     *
提交完low temperature之后才能提交high temperature
     */
    fun pointEnd(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
calibration前需要resettemperature measurement parameters,否则temperaturecalibration inaccuracy
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_END, pointTemp + 273)
            if (result == 0) {
                success = true
            } else {
                Log.w("123", "failed")
            }
        } else {
            Log.w("123", "failed")
        }
        return success
    }

    /**
锅盖calibration - 步骤一准备
     *
     */
    fun potReady(irCmd: IRCMD): Boolean {
        return irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_DIS) == 0 // Close锅盖校正
    }

    /**
锅盖calibration - 步骤二start
     *
@param gainType 默认GAIN_1
     * CommonParams.RMCoverAutoCalcType.GAIN_1
     * CommonParams.RMCoverAutoCalcType.GAIN_2
     * CommonParams.RMCoverAutoCalcType.GAIN_4
     */
    fun potStart(
        irCmd: IRCMD,
        type: Int,
    ) {
        val gainType =
            when (type) {
                1 -> CommonParams.RMCoverAutoCalcType.GAIN_1
                2 -> CommonParams.RMCoverAutoCalcType.GAIN_2
                4 -> CommonParams.RMCoverAutoCalcType.GAIN_4
                else -> CommonParams.RMCoverAutoCalcType.GAIN_1
            }
        irCmd.rmCoverAutoCalc(gainType) // Send锅盖calibration
        irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_EN) // Open锅盖校正
    }

    /**
Cancelcalibration
     */
    fun cancelCalibration(irCmd: IRCMD) {
        irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD)
    }

    /**
Restore出厂calibration
     */
    fun reset(irCmd: IRCMD) {
        irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_ALL)
    }

    /**
查询gainmode
@return true: 高gain    false: 低gain
     */
    fun queryGain(irCmd: IRCMD): Boolean {
        val value = IntArray(1)
        irCmd.getPropTPDParams(CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL, value)
        return value[0] == 1
    }

    /**
setgainmode
@param type 1: Open    0: disabled
     *
     */
    fun setGain(
        irCmd: IRCMD,
        type: Int,
    ) {
        if (type == 1) {
            irCmd.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
                CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_HIGH,
            )
        } else {
            irCmd.setPropTPDParams(CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL, CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_LOW)
        }
    }

    /**
查询Tpd
     */
    fun queryTpd(
        irCmd: IRCMD,
        params: CommonParams.PropTPDParams,
    ): Int {
        val value = IntArray(1)
        irCmd.getPropTPDParams(params, value)
        return value[0]
    }

    /**
打快门
     */
    fun shutter(
        irCmd: IRCMD?,
        syncImage: SynchronizedBitmap,
    ) {
        if (syncImage.type == 1) {
            irCmd?.tc1bShutterManual()
        } else {
执行这段
            irCmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
        }
    }

    /**
控制锅盖calibration开关
     */
    fun stsSwitch(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        if (flag) {
            irCmd?.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_EN)
        } else {
            irCmd?.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_DIS)
        }
    }

    /**
锅盖calibration - 步骤二start
     *
@param gainType 默认GAIN_1
     * CommonParams.RMCoverAutoCalcType.GAIN_1
     * CommonParams.RMCoverAutoCalcType.GAIN_2
     * CommonParams.RMCoverAutoCalcType.GAIN_4
     */
    fun pot(
        irCmd: IRCMD,
        type: Int,
    ) {
        val gainType =
            when (type) {
                1 -> CommonParams.RMCoverAutoCalcType.GAIN_1
                2 -> CommonParams.RMCoverAutoCalcType.GAIN_2
                4 -> CommonParams.RMCoverAutoCalcType.GAIN_4
                else -> CommonParams.RMCoverAutoCalcType.GAIN_1
            }
        irCmd.rmCoverAutoCalc(gainType) // Send锅盖calibration
    }

    /**
自动快门
     */
    fun autoShutter(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        val data = if (flag) CommonParams.PropAutoShutterParameterValue.StatusSwith.ON else CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
        irCmd?.setPropAutoShutterParameter(CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH, data)
    }

    /**
TPD_PROP_DISTANCE不给set
set距离 unit:cnt(128cnt=1m)
     * @param value 0 ~ 25600
     */
    fun setTpdDis(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_DISTANCE, value = data)
    }

    /**
setemissivity unit:cnt(128cnt=1)
     * @param value 1 ~ 128
     */
    fun setTpdEms(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_EMS, value = data)
    }

    /**
setTpd
     */
    private fun setTpdParams(
        irCmd: IRCMD?,
        params: CommonParams.PropTPDParams,
        value: CommonParams.PropTPDParamsValue,
    ): Int {
        return try {
            irCmd?.setPropTPDParams(params, value) ?: 0
        } catch (e: Exception) {
            XLog.w("settingsparameterexception[${params.name}]: ${e.message}")
            0
        }
    }
}
