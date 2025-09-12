package com.infisense.usbir.inf

/**
 * 热稳定接口
 * @author: CaiSongL
 * @date: 2024/1/10 11:40
 */
interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float

    fun compensateTemp(temp: Float): Float
}
