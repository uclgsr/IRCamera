package com.mpdc4gsr.libunified.ir.inf
interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float
    fun compensateTemp(temp: Float): Float
}
