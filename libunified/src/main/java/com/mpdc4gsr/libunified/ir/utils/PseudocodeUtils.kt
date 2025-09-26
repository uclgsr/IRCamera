package com.mpdc4gsr.libunified.ir.utils

import com.energy.iruvc.utils.CommonParams

/**
 * PseudocodeUtils - Utility for pseudocolor mode conversions
 * Used by ImageThreadTC for IR camera processing
 */
object PseudocodeUtils {
    
    /**
     * Change pseudocode mode by old value
     */
    @JvmStatic
    fun changePseudocodeModeByOld(oldMode: Int): CommonParams.PseudoColorType {
        // For now, just return PSEUDO_1 as it's the only available option
        // TODO: Add more pseudo color types when they become available
        return CommonParams.PseudoColorType.PSEUDO_1
    }
}