package com.mpdc4gsr.module.thermalunified.tools.medie

interface IYapVideoProvider<Bitmap> {
    fun size(): Int
    operator fun next(): Bitmap
    fun progress(progress: Float)
}
