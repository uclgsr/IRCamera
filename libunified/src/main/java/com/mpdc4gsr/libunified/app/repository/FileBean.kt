package com.mpdc4gsr.libunified.app.repository
import android.os.Parcel
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.libunified.app.tools.VideoTools
import java.io.File
import java.util.*
open class FileBean(
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
    override fun describeContents(): Int {
        return 0
    }
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(path)
        dest.writeString(thumb)
        dest.writeString(name)
        dest.writeLong(duration)
        dest.writeLong(timeMillis)
        dest.writeByte(if (hasDownload) 1 else 0)
    }
    companion object CREATOR : Parcelable.Creator<FileBean> {
        override fun createFromParcel(parcel: Parcel): FileBean {
            return FileBean(
                id = parcel.readInt(),
                path = parcel.readString() ?: "",
                thumb = parcel.readString() ?: "",
                name = parcel.readString() ?: "",
                duration = parcel.readLong(),
                timeMillis = parcel.readLong(),
                hasDownload = parcel.readByte() != 0.toByte()
            )
        }
        override fun newArray(size: Int): Array<FileBean?> {
            return arrayOfNulls(size)
        }
    }
}
