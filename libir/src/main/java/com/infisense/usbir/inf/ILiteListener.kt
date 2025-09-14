package com.infisense.usbir.inf



interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float


    fun compensateTemp(temp: Float): Float
}
