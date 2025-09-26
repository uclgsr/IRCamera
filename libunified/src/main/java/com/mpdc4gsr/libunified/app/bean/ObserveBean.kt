package com.mpdc4gsr.libunified.app.bean

/**
 * Bean class for observation/target settings used across the IR camera functionality
 */
data class ObserveBean(
    var type: Int = TYPE_NONE,
    var measureMode: Int = TYPE_MEASURE_PERSON,
    var targetMode: Int = TYPE_TARGET_HORIZONTAL,
    var targetColor: Int = TYPE_TARGET_COLOR_GREEN,
    var isEnabled: Boolean = false
) {
    companion object {
        // Basic types
        const val TYPE_NONE = 0
        
        // Measure modes for different subjects
        const val TYPE_MEASURE_PERSON = 1
        const val TYPE_MEASURE_SHEEP = 2
        const val TYPE_MEASURE_DOG = 3
        const val TYPE_MEASURE_BIRD = 4
        
        // Target orientation modes
        const val TYPE_TARGET_HORIZONTAL = 10
        const val TYPE_TARGET_VERTICAL = 11
        
        // Target color types
        const val TYPE_TARGET_COLOR_GREEN = 20
        const val TYPE_TARGET_COLOR_RED = 21
        const val TYPE_TARGET_COLOR_BLUE = 22
        const val TYPE_TARGET_COLOR_BLACK = 23
        const val TYPE_TARGET_COLOR_WHITE = 24
    }
}