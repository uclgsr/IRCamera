package com.infisense.usbir.inf

import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.DualCameraParams

/**
 * 统一管理dual light的特殊interface，区别于单光
 * @author: CaiSongL
 * @date: 2024/1/10 11:40
 */
@Deprecated("未使用，好像没什么用")
/**
 * IDualListener manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
interface IDualListener {
    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera)

    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType)

    fun setUseIRISP(useIRISP: Boolean)
}
