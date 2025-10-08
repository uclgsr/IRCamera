// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\IDualListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.DualCameraParams

@Deprecated("[ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph]")
interface IDualListener {
    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera)
    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType)
    fun setUseIRISP(useIRISP: Boolean)
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\ILiteListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float
    fun compensateTemp(temp: Float): Float
}