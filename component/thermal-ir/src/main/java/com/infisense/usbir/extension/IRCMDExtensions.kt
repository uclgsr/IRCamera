package com.infisense.usbir.extension

import com.energy.iruvc.ircmd.IRCMD

/**
 * Extension functions for IRCMD to provide missing hardware SDK methods
 * These are minimal implementations to ensure compilation compatibility
 */

/**
 * Set mirror mode for the IR camera
 */
fun IRCMD.setMirror(enabled: Boolean) {
    // Note: Mirror functionality requires full SDK integration
    // For now, this is a stub to allow compilation
}

/**
 * Set auto shutter mode
 */
fun IRCMD.setAutoShutter(enabled: Boolean) {
    // Note: Auto shutter functionality requires full SDK integration
    // For now, this is a stub to allow compilation
}

/**
 * Set DDE (Digital Detail Enhancement) level
 */
fun IRCMD.setPropDdeLevel(level: Int) {
    // Note: DDE level setting requires full SDK integration
    // For now, this is a stub to allow compilation
}

/**
 * Set contrast level
 */
fun IRCMD.setContrast(level: Int) {
    // Note: Contrast setting requires full SDK integration
    // For now, this is a stub to allow compilation
}