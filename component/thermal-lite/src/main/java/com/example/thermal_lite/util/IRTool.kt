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

/**
 * des:
 * author: CaiSongL
 * date: 2024/8/2 16:43
 **/
object IRTool {
    const val TAG: String = "IRTool"

    /**
自动快门开关
     */
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

    /**
手动打快门
     */
    fun setOneShutter()  {
        val basicFFCUpdate = DeviceIrcmdControlManager.getInstance().ircmdEngine?.basicFFCUpdate()
        Log.d(
            TAG,
            "basicFFCUpdate=$basicFFCUpdate",
        )
    }

    /**
     *
     *
常温 ([CameraItemBean.TYPE_TMP_C] = 1）也就是高gain
     *
high temperature ([CameraItemBean.TYPE_TMP_H] = 0) 也就是低gain
     *
自动 ([CameraItemBean.TYPE_TMP_ZD] = -1)
     */
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

    /**
contrast：parameter是0-100
     */
    fun basicGlobalContrastLevelSet(levelValue: Int)  {
        val basicGlobalContrastLevelSetResult =
            DeviceIrcmdControlManager.getInstance().ircmdEngine
                ?.basicGlobalContrastLevelSet(levelValue)
        Log.d(
            TAG,
            "basicGlobalContrastLevelSet=$basicGlobalContrastLevelSetResult",
        )
    }

    /**
锐度：parameter是0-100，也就是细节
     */
    fun basicImageDetailEnhanceLevelSet(levelValue: Int)  {
//        val professionModeSetResult = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .advProfessionModeSet(CommonParams.ProfessionMode.valueOf(0))
//        val basicImageDetailEnhanceLevelSetResult = DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
//            ?.basicImageDetailEnhanceLevelSet(levelValue);
//        Log.d(TAG, "basicImageDetailEnhanceLevelSet=" + basicImageDetailEnhanceLevelSetResult)
    }

    /**
set镜像
     */
    fun basicMirrorAndFlipStatusSet(openMirror: Boolean)  {
setimage镜像或翻转 PASS
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

    /**
一次complete的锅盖calibration流程
     * https://alidocs.dingtalk.com/i/p/QqWXwywDMb9xKG31/docs/14lgGw3P8vL0P2qbu7OR39d5V5daZ90D
Setp1：插上module出图并确保当前module达到热稳定state，一般需要预热3-5分钟。
预热complete后，移动module至calibration靶area前，靠近但不接触靶area。靶area的成像覆盖全部视场、 无杂散光进入为最佳)；
Setp2：reset锅盖calibrationdata，确保calibration准确性
Setp3：disabled自动快门
Setp4：打快门
Setp5：进行自动锅盖calibration
Setp6：Restore自动快门
Setp7：如果calibration有误，或者需要Cancel自动calibration结果，可调用指令
     * mIrcmdEngine.advRmcoverCaliCancel();
如果观察calibration没有问题，即可save锅盖calibrationdata，可调用指令
     * mIrcmdEngine.basicSaveData(CommonParams.DeviceDataSaveType.BASIC_SAVE_RMCOVER_DATA);
     */
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

    /**
high/low gainmode下各做一组锅盖calibration，如此module的锅盖calibration才是完整的流程
     */
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

    /**
enabledcore内部环境variable修正
     */
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

    /**
core校正的
反射率：range:1~16384
     */
    fun advEnvCorrectEMSSet(value: Int)  {
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            .advEnvCorrectEMSSet(value)
    }

    /**
core校正的
反射temperature(units:Celsius)：range:233~373
     */
    fun advEnvCorrectTUSet(value: Int)  {
        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
            ?.advEnvCorrectTUSet(value)
    }

    /**
lite项目的temperature correction
     * @param temp Float
     * @param params_array FloatArray
@param tau_data_H ByteArray 高gain修正表
@param tau_data_L ByteArray 低gain修正表
     * @return Float
     */
    fun temperatureCorrection(
        temp: Float,
        params_array: FloatArray,
        tau_data_H: ByteArray,
        tau_data_L: ByteArray,
        basicGainGetValue: Int,
    ): Float {
        var newTemp = temp
getgainstate PASS
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

    /**
set场景mode三
     */
    fun setMode()  {
//        val professionModeSetResult = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .advProfessionModeSet(CommonParams.ProfessionMode.valueOf(0))
//        val ircmdError = DeviceIrcmdControlManager.getInstance().ircmdEngine
//            .basicImageSceneModeSet(3)
//        Log.d(TAG, "setModel=${ircmdError}")
    }
}
