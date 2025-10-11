package com.mpdc4gsr.component.shared.ir.inf

interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float

    fun compensateTemp(temp: Float): Float
}


