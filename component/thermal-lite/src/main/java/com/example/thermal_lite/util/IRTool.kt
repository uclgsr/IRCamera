package com.example.thermal_lite.util

import android.util.Log
import com.elvishew.xlog.XLog
import com.energy.ac020library.bean.CommonParams
import com.energy.ac020library.bean.IrcmdError
import com.energy.irutilslibrary.LibIRTempAC020
import com.energy.irutilslibrary.bean.GainStatus
import com.example.thermal_lite.camera.CameraPreviewManager
import com.example.thermal_lite.camera.DeviceIrcmdControlManager
import com.topdon.lib.core.bean.CameraItemBean
import kotlinx.coroutines.delay


object IRTool {
    const val TAG: String = "IRTool"


    fun setAutoShutter(isAutoShutter: Boolean)  {
        val basicAutoFFCStatusSet: IrcmdError? =
            DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                ?.basicAutoFFCStatusSet(
                    if (isAutoShutter) {
                        CommonParams.AutoFFCStatus.AUTO_FFC_ENABLE
                    } else {
                        CommonParams.AutoFFCStatus.AUTO_FFC_DISABLED
                    },
                )
        Log.d(
            TAG,
            "basicAutoFFCStatusSet=$basicAutoFFCStatusSet",
        )
    }


    fun setOneShutter()  {
        val basicFFCUpdate = DeviceIrcmdControlManager.getInstance().ircmdEngine?.basicFFCUpdate()
        Log.d(
            TAG,
            "basicFFCUpdate=$basicFFCUpdate",
        )
    }


    fun basicGainSet(gainType: Int)  {
        if (gainType == CameraItemBean.TYPE_TMP_ZD)
            {
                CameraPreviewManager.getInstance().setAutoSwitchGainEnable(true)
            } else if (gainType == CameraItemBean.TYPE_TMP_C)
            {
                CameraPreviewManager.getInstance().setAutoSwitchGainEnable(false)
                val basicGainSet =
                    DeviceIrcmdControlManager.getInstance().ircmdEngine
                        ?.basicGainSet(CommonParams.GainStatus.HIGH_GAIN)
                Log.d(TAG, "basicGainSet=$basicGainSet--$gainType")
            } else if (gainType == CameraItemBean.TYPE_TMP_H)
            {
                CameraPreviewManager.getInstance().setAutoSwitchGainEnable(false)
                val basicGainSet =
                    DeviceIrcmdControlManager.getInstance().ircmdEngine
                        ?.basicGainSet(CommonParams.GainStatus.LOW_GAIN)
                Log.d(TAG, "basicGainSet=$basicGainSet--$gainType")
            }
    }


    fun basicGlobalContrastLevelSet(levelValue: Int)  {
        val basicGlobalContrastLevelSetResult =
            DeviceIrcmdControlManager.getInstance().ircmdEngine
                ?.basicGlobalContrastLevelSet(levelValue)
        Log.d(
            TAG,
            "basicGlobalContrastLevelSet=$basicGlobalContrastLevelSetResult",
        )
    }


    fun basicImageDetailEnhanceLevelSet(levelValue: Int)  {
//        val professionModeSetResult = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .advProfessionModeSet(CommonParams.ProfessionMode.valueOf(0))
//        val basicImageDetailEnhanceLevelSetResult = DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
//            ?.basicImageDetailEnhanceLevelSet(levelValue);
//        Log.d(TAG, "basicImageDetailEnhanceLevelSet=" + basicImageDetailEnhanceLevelSetResult)
    }


    fun basicMirrorAndFlipStatusSet(openMirror: Boolean)  {
//setimage镜像或翻转 PASS
        val basicMirrorAndFlipStatusSet =
            DeviceIrcmdControlManager.getInstance().ircmdEngine
                ?.basicMirrorAndFlipStatusSet(
                    if (openMirror) {
                        CommonParams.MirrorFlipType.ONLY_FLIP
                    } else {
                        CommonParams.MirrorFlipType.NO_MIRROR_OR_FLIP
                    },
                )
        Log.d(TAG, "basicGlobalContrastLevelSet=$basicMirrorAndFlipStatusSet")
    }


    fun onceAuto(): Boolean  {
        // Setp2
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.basicRestoreDefaultData(CommonParams.DeviceRestoreTypeType.BASIC_RESTROE_RMCOVER_DATA)
        // Setp3
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.basicAutoFFCStatusSet(CommonParams.AutoFFCStatus.AUTO_FFC_DISABLED)
        // Setp4
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()?.basicFFCUpdate()
        // Setp5
        val result = DeviceIrcmdControlManager.getInstance().getIrcmdEngine()?.advAutoRmcoverCali()
        Log.d(TAG, "advAutoRmcoverCali=$result")
        // Setp6
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.basicAutoFFCStatusSet(CommonParams.AutoFFCStatus.AUTO_FFC_ENABLE)
        // Setp7
        val ircmdError =
            DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                ?.basicSaveData(CommonParams.DeviceDataSaveType.BASIC_SAVE_RMCOVER_DATA)
        return ircmdError == IrcmdError.IRCMD_SUCCESS
    }


    suspend fun autoStart(): Boolean  {
        basicGainSet(CameraItemBean.TYPE_TMP_C)
        delay(2000)
        XLog.d(TAG, "onceAuto=start")
        if (!onceAuto())
            {
                return false
            }
        XLog.d(TAG, "basicGainSet=start")
        basicGainSet(CameraItemBean.TYPE_TMP_H)
        delay(2000)
        XLog.d(TAG, "onceAuto=start")
        return onceAuto()
    }


    fun advEnvCorrectSwitchSet(open: Boolean)  {
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.advEnvCorrectSwitchSet(
                if (open) {
                    CommonParams.BasicEnableStatus.BASIC_ENABLE
                } else {
                    CommonParams.BasicEnableStatus.BASIC_DISABLE
                },
            )
    }


    fun advEnvCorrectEMSSet(value: Int)  {
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            .advEnvCorrectEMSSet(value)
    }


    fun advEnvCorrectTUSet(value: Int)  {
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.advEnvCorrectTUSet(value)
    }


    fun temperatureCorrection(
        temp: Float,
        params_array: FloatArray,
        tau_data_H: ByteArray,
        tau_data_L: ByteArray,
        basicGainGetValue: Int,
    ): Float {
        var newTemp = temp
//getgain状态 PASS
        try {
            if (tau_data_H == null || tau_data_L == null) return temp
            newTemp =
                LibIRTempAC020.temperatureCorrection(
                    params_array[0],
                    tau_data_H,
                    tau_data_L,
                    params_array[1],
                    params_array[2],
                    params_array[3],
                    params_array[4],
                    params_array[5],
                    if (basicGainGetValue == 0) GainStatus.LOW_GAIN else GainStatus.HIGH_GAIN,
                )
        } catch (e: Exception) {
            XLog.e("$TAG:temperatureCorrection-${e.message}")
        } finally {
            return newTemp
        }
    }


    fun setMode()  {
//        val professionModeSetResult = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .advProfessionModeSet(CommonParams.ProfessionMode.valueOf(0))
//        val ircmdError = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .basicImageSceneModeSet(3)
//        Log.d(TAG, "setModel=${ircmdError}")
    }
}
