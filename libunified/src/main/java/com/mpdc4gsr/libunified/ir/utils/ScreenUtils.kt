package com.mpdc4gsr.libunified.ir.utils

import com.energy.iruvc.utils.CommonParams

/**
 * ScreenUtils - Adapter for screen operations related to IR camera
 * Maps to UnifiedScreenUtils where appropriate
 */
object ScreenUtils {
    
    /**
     * Get preview FPS by data flow mode
     */
    @JvmStatic
    fun getPreviewFPSByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): Int {
        return when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> 30
            CommonParams.DataFlowMode.TNR_OUTPUT -> 15
            else -> 25
        }
    }
}