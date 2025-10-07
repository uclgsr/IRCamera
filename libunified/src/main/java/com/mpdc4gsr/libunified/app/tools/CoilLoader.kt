package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoilLoader {
    private const val TAG = "CoilLoader"
    private const val CORNER_RADIUS_DP = 6f
    private fun getPhotoOptions(context: Context): RoundedCornersTransformation {
        return RoundedCornersTransformation(CORNER_RADIUS_DP.dpToPx(context))
    }

    private fun loadCircleWithData(
        img: ImageView,
        data: Any,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(data)
            .target(img)
            .apply { options?.invoke(this) }
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadCircle(
        img: ImageView,
        resourceId: Int,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, resourceId, options)

    fun loadCircle(
        img: ImageView,
        url: String,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, url, options)

    fun loadCircle(
        img: ImageView,
        drawable: Drawable,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, drawable, options)

    fun loadCircle(
        img: ImageView,
        uri: Uri,
        options: ((ImageRequest.Builder) -> Unit)? = null,
    ) = loadCircleWithData(img, uri, options)

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

    private fun loadRoundedWithData(
        img: ImageView,
        data: Any,
    ) {
        val request = ImageRequest.Builder(img.context)
            .data(data)
            .transformations(getPhotoOptions(img.context))
            .error(R.mipmap.ic_default_head)
            .target(img)
            .build()
        img.context.imageLoader.enqueue(request)
    }

    fun loadRounded(
        img: ImageView,
        resourceId: Int,
    ) = loadRoundedWithData(img, resourceId)

    fun loadRounded(
        img: ImageView,
        url: String,
    ) = loadRoundedWithData(img, url)

    fun loadRounded(
        img: ImageView,
        drawable: Drawable,
    ) = loadRoundedWithData(img, drawable)

    fun loadRounded(
        img: ImageView,
        uri: Uri,
    ) = loadRoundedWithData(img, uri)

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
                Log.e(TAG, "Failed to load drawable from URL: $url", e)
                null
            }
        }
    }
}
