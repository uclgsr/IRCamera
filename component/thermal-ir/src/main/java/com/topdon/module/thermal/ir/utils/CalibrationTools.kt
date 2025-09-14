package com.topdon.module.thermal.ir.utils

import android.util.Log
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap


object CalibrationTools {

    fun sign(
        irCmd: IRCMD,
        singlePointTemp: Int,
    ): Boolean {
        var success = false
//calibration前需要重置temperature measurement parameters,否则temperaturecalibration inaccuracy
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


    fun pointFirst(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
//calibration前需要重置temperature measurement parameters,否则temperaturecalibration inaccuracy
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_FIRST, pointTemp + 273)
            if (result == 0) {
                success = true
            } else {
                XLog.w("低温标定失败")
            }
        } else {
            XLog.w("低温标定失败")
        }
        return success
    }


    fun pointEnd(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
//calibration前需要重置temperature measurement parameters,否则temperaturecalibration inaccuracy
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_END, pointTemp + 273)
            if (result == 0) {
                success = true
            } else {
                Log.w("123", "失败")
            }
        } else {
            Log.w("123", "失败")
        }
        return success
    }


    fun potReady(irCmd: IRCMD): Boolean {
        return irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_DIS) == 0 // 关闭锅盖校正
    }


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
        irCmd.rmCoverAutoCalc(gainType) // 发送锅盖标定
        irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_EN) // 打开锅盖校正
    }


    fun cancelCalibration(irCmd: IRCMD) {
    irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD)
    }


    fun reset(irCmd: IRCMD) {
    irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_ALL)
    }


    fun queryGain(irCmd: IRCMD): Boolean {
    val value = IntArray(1)
    irCmd.getPropTPDParams(CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL, value)
    return value[0] == 1
    }


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


    fun queryTpd(
        irCmd: IRCMD,
        params: CommonParams.PropTPDParams,
    ): Int {
        val value = IntArray(1)
        irCmd.getPropTPDParams(params, value)
        return value[0]
    }


    fun shutter(
        irCmd: IRCMD?,
        syncImage: SynchronizedBitmap,
    ) {
        if (syncImage.type == 1) {
            irCmd?.tc1bShutterManual()
        } else {
//执行这段
            irCmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
        }
    }


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
        irCmd.rmCoverAutoCalc(gainType) // 发送锅盖标定
    }


    fun autoShutter(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        val data = if (flag) CommonParams.PropAutoShutterParameterValue.StatusSwith.ON else CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
        irCmd?.setPropAutoShutterParameter(CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH, data)
    }


    fun setTpdDis(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_DISTANCE, value = data)
    }


    fun setTpdEms(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_EMS, value = data)
    }


    private fun setTpdParams(
        irCmd: IRCMD?,
        params: CommonParams.PropTPDParams,
        value: CommonParams.PropTPDParamsValue,
    ): Int {
        return try {
            irCmd?.setPropTPDParams(params, value) ?: 0
        } catch (e: Exception) {
            XLog.w("设置参数异常[${params.name}]: ${e.message}")
            0
        }
    }
}
