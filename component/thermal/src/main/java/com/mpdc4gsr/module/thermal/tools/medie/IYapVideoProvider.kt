package com.mpdc4gsr.module.thermal.tools.medie


interface IYapVideoProvider<Bitmap> {

    fun size(): Int

    operator fun next(): Bitmap

    fun progress(progress: Float)
}
