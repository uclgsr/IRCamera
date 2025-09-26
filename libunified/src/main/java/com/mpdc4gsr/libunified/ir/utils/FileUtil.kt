package com.mpdc4gsr.libunified.ir.utils

import com.energy.iruvc.utils.CommonParams

/**
 * FileUtil - Adapter for file operations related to IR camera
 * Maps to existing utilities where appropriate
 */
object FileUtil {
    
    /**
     * Get Y16 source type by data flow mode
     */
    @JvmStatic
    fun getY16SrcTypeByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): CommonParams.Y16ModePreviewSrcType {
        return when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
            CommonParams.DataFlowMode.TNR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TNR
            else -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
        }
    }
}