package com.mpdc4gsr.component.shared.ir.inf

import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.DualCameraParams

@Deprecated("[ph][ph][ph]，[ph][ph][ph][ph][ph][ph]")
interface IDualListener {
    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera)

    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType)

    fun setUseIRISP(useIRISP: Boolean)
}


