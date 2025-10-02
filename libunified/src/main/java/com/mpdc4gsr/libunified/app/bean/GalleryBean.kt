package com.mpdc4gsr.libunified.app.bean

import android.os.Parcel
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.repository.TS004FileBean
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.libunified.app.tools.VideoTools
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.*

@Parcelize
open class GalleryBean(
    val id: Int,
    val path: String,
    val thumb: String,
    val name: String,
    val duration: Long,
    val timeMillis: Long,
    var hasDownload: Boolean,
) : Parcelable {
    constructor(file: File) : this(
        id = 0,
        path = file.absolutePath,
        thumb = file.absolutePath,
        name = file.name,
        duration = VideoTools.getLocalVideoDuration(file.absolutePath),
        timeMillis = TimeTools.updateDateTime(file),
        hasDownload = true,
    )

    constructor(isVideo: Boolean, fileBean: TS004FileBean) : this(
        id = fileBean.id,
        path = "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        thumb = if (isVideo) "http://192.168.40.1:8080/DCIM/${fileBean.thumb}" else "http://192.168.40.1:8080/DCIM/${fileBean.name}",
        name = fileBean.name,
        duration = fileBean.duration * 1000L,
        timeMillis = fileBean.time * 1000 - TimeZone.getDefault().getOffset(fileBean.time * 1000),
        hasDownload = File(FileConfig.ts004GalleryDir, fileBean.name).exists(),
    )
}

class GalleryTitle(timeMillis: Long) : GalleryBean(
    id = 0,
    path = "",
    thumb = "",
    name = "",
    duration = 0L,
    timeMillis = timeMillis,
    hasDownload = true,
) {
    companion object CREATOR : Parcelable.Creator<GalleryTitle> {
        override fun createFromParcel(parcel: Parcel): GalleryTitle {
            return GalleryTitle(parcel.readLong())
        }

        override fun newArray(size: Int): Array<GalleryTitle?> {
            return arrayOfNulls(size)
        }
    }
}
