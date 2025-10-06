package com.mpdc4gsr.libunified.common
enum class RotateDegree(private val value: Int) {
    DEGREE_0(0),
    DEGREE_90(90),
    DEGREE_180(180),
    DEGREE_270(270);
    fun getValue(): Int {
        return value
    }
}