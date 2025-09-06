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
    // TODO: Implement actual mirror functionality when full SDK is available
    // For now, this is a stub to allow compilation
}

/**
 * Set auto shutter mode
 */
fun IRCMD.setAutoShutter(enabled: Boolean) {
    // TODO: Implement actual auto shutter functionality when full SDK is available  
    // For now, this is a stub to allow compilation
}

/**
 * Set DDE (Digital Detail Enhancement) level
 */
fun IRCMD.setPropDdeLevel(level: Int) {
    // TODO: Implement actual DDE level setting when full SDK is available
    // For now, this is a stub to allow compilation
}

/**
 * Set contrast level
 */
fun IRCMD.setContrast(level: Int) {
    // TODO: Implement actual contrast setting when full SDK is available
    // For now, this is a stub to allow compilation
}