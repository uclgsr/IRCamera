package com.mpdc4gsr.module.thermalunified.utils
import com.energy.iruvc.utils.DualCameraParams
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
object DualParamsUtils {
    fun wifiFusionTypeToParams(fusionType: Int): Int {
        return when (fusionType) {
            SaveSettingUtils.FusionTypeVLOnly -> 3
            SaveSettingUtils.FusionTypeIROnlyNoFusion -> 1
            SaveSettingUtils.FusionTypeMeanFusion -> 4
            SaveSettingUtils.FusionTypeIROnly -> 0
            else -> 3
        }
    }
    fun fusionTypeToParams(fusionType: Int): DualCameraParams.FusionType {
        return when (fusionType) {
            SaveSettingUtils.FusionTypeVLOnly -> DualCameraParams.FusionType.VLOnly
            SaveSettingUtils.FusionTypeIROnlyNoFusion -> DualCameraParams.FusionType.IROnlyNoFusion
            SaveSettingUtils.FusionTypeScreenFusion -> DualCameraParams.FusionType.ScreenFusion
            SaveSettingUtils.FusionTypeHSLFusion -> DualCameraParams.FusionType.HSLFusion
            SaveSettingUtils.FusionTypeMeanFusion -> DualCameraParams.FusionType.MeanFusion
            SaveSettingUtils.FusionTypeLPYFusion -> DualCameraParams.FusionType.LPYFusion
            SaveSettingUtils.FusionTypeIROnly -> DualCameraParams.FusionType.IROnly
            else -> DualCameraParams.FusionType.LPYFusion
        }
    }
    fun paramsToFusionType(fusionTypeP: DualCameraParams.FusionType): Int {
        return when (fusionTypeP) {
            DualCameraParams.FusionType.VLOnly -> SaveSettingUtils.FusionTypeVLOnly
            DualCameraParams.FusionType.IROnlyNoFusion -> SaveSettingUtils.FusionTypeIROnlyNoFusion
            DualCameraParams.FusionType.ScreenFusion -> SaveSettingUtils.FusionTypeScreenFusion
            DualCameraParams.FusionType.HSLFusion -> SaveSettingUtils.FusionTypeHSLFusion
            DualCameraParams.FusionType.MeanFusion -> SaveSettingUtils.FusionTypeMeanFusion
            DualCameraParams.FusionType.LPYFusion -> SaveSettingUtils.FusionTypeLPYFusion
            DualCameraParams.FusionType.IROnly -> SaveSettingUtils.FusionTypeIROnly
        }
    }
}
