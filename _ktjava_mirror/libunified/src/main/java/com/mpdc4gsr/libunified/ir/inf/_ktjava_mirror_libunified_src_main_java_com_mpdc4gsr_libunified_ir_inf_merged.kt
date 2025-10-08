// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\libunified_src_main_java_com_mpdc4gsr_libunified_ir_inf_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf' subtree
// Files: 2; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\IDualListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.DualCameraParams

@Deprecated("[ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph]")
interface IDualListener {
    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera)
    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType)
    fun setUseIRISP(useIRISP: Boolean)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\ILiteListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float
    fun compensateTemp(temp: Float): Float
}