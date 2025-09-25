package com.mpdc4gsr.libunified.ui.bean

/**
 * Data class for color bean items used in adapters
 * @param res Resource ID for drawable
 * @param name Display name
 * @param code Type code for identification
 */
data class ColorBean(
    val res: Int,
    val name: String,
    val code: Int
)