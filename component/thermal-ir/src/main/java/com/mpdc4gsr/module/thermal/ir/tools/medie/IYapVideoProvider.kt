package com.mpdc4gsr.module.thermal.ir.tools.medie


interface IYapVideoProvider<Bitmap> {

    fun size(): Int

    operator fun next(): Bitmap

    fun progress(progress: Float)
}
