package com.mpdc4gsr.module.thermalunified.tools

import android.content.Context
import android.view.View
import android.widget.ImageView
import coil.load
import com.maning.imagebrowserlibrary.ImageEngine

class CoilImageEngine : ImageEngine {
    override fun loadImage(
        context: Context,
        url: String,
        imageView: ImageView,
        _progressView: View,
        _customImageView: View,
    ) {
        imageView.load(url) {
            crossfade(true)
        }
    }
}
