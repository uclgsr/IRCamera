package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoilLoader {

    private fun getPhotoOptions(context: Context): RoundedCornersTransformation {
        return RoundedCornersTransformation(6f.dpToPx(context))
    }

    fun loadCircle(
        img: ImageView,
        resourceId: Int,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(resourceId)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        url: String,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        drawable: Drawable,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(drawable)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        uri: Uri,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(uri)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        url: String,
        resourceId: Int,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .error(resourceId)
            .placeholder(resourceId)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        resourceId: Int,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(resourceId)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        url: String,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        drawable: Drawable,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(drawable)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        uri: Uri,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(uri)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun load(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.mipmap.bg_default_img)
            .error(R.mipmap.bg_default_img)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadGallery(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_gallery_default_shape)
            .error(R.drawable.ic_gallery_default_shape)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadFit(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_default_search_svg)
            .error(R.drawable.ic_default_search_svg)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun load(
        img: ImageView,
        resourceId: Int,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(resourceId)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadP(
        img: ImageView,
        url: String?,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(url)
            .placeholder(R.drawable.ic_default_search_svg)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    suspend fun getDrawable(
        context: Context,
        url: String?,
    ): Drawable? {
        if (url == null) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                val result = context.imageLoader.execute(request)
                result.drawable
            } catch (e: Exception) {
                null
            }
        }
    }

    private val Context.imageLoader: ImageLoader
        get() = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
}
