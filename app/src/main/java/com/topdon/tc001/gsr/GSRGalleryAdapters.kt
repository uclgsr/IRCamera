package com.topdon.tc001.gsr

import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import java.io.File

/**
 * Adapter for GSR Data Files RecyclerView
 */
class GSRDataAdapter(
    private val dataFiles: List<GSRDataFragment.GSRDataFile>,
    private val onItemClick: (GSRDataFragment.GSRDataFile) -> Unit,
) : RecyclerView.Adapter<GSRDataAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileIcon: ImageView = view.findViewById(R.id.file_icon)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val sessionInfo: TextView = view.findViewById(R.id.session_info)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val sampleCount: TextView = view.findViewById(R.id.sample_count)
        val duration: TextView = view.findViewById(R.id.duration)
        val createdDate: TextView = view.findViewById(R.id.created_date)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gsr_data_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val dataFile = dataFiles[position]

        holder.fileName.text = dataFile.file.name
        holder.sessionInfo.text = "Session: ${dataFile.sessionId} | Participant: ${dataFile.participantId}"
        holder.fileSize.text = formatFileSize(dataFile.file.length())
        holder.sampleCount.text = "${dataFile.sampleCount} samples"
        holder.duration.text = formatDuration(dataFile.duration)
        holder.createdDate.text = dataFile.createdDate

        holder.itemView.setOnClickListener {
            onItemClick(dataFile)
        }
    }

    override fun getItemCount() = dataFiles.size

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }
}

/**
 * Adapter for GSR Video Files RecyclerView
 */
class GSRVideoAdapter(
    private val videoFiles: List<File>,
    private val onItemClick: (File) -> Unit,
) : RecyclerView.Adapter<GSRVideoAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoThumbnail: ImageView = view.findViewById(R.id.video_thumbnail)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val duration: TextView = view.findViewById(R.id.duration)
        val resolution: TextView = view.findViewById(R.id.resolution)
        val createdDate: TextView = view.findViewById(R.id.created_date)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gsr_video_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val videoFile = videoFiles[position]

        holder.fileName.text = videoFile.name
        holder.fileSize.text = formatFileSize(videoFile.length())

        // Parse video metadata from filename if available
        val filename = videoFile.nameWithoutExtension
        when {
            filename.contains("4K") -> holder.resolution.text = "4K UHD (3840×2160)"
            filename.contains("1080") -> holder.resolution.text = "Full HD (1920×1080)"
            filename.contains("720") -> holder.resolution.text = "HD (1280×720)"
            else -> holder.resolution.text = "Unknown resolution"
        }

        holder.createdDate.text =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(videoFile.lastModified()))

        // Extract actual video duration using MediaMetadataRetriever
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(item.filePath)
            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationString?.toLongOrNull() ?: 0L
            
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = (durationMs / (1000 * 60 * 60))
            
            val formattedDuration = when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                else -> String.format("%02d:%02d", minutes, seconds)
            }
            
            holder.binding.videoDuration.text = formattedDuration
            retriever.release()
            
        } catch (e: Exception) {
            Log.e("GSRGalleryAdapter", "Error extracting video duration", e)
            holder.binding.videoDuration.text = "00:00"
        }
        holder.duration.text = "Duration: Unknown"

        holder.itemView.setOnClickListener {
            onItemClick(videoFile)
        }
    }

    override fun getItemCount() = videoFiles.size

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

/**
 * Adapter for GSR RAW Image Files RecyclerView
 */
class GSRRawImageAdapter(
    private val rawImageFiles: List<File>,
    private val onItemClick: (File) -> Unit,
) : RecyclerView.Adapter<GSRRawImageAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageThumbnail: ImageView = view.findViewById(R.id.image_thumbnail)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val resolution: TextView = view.findViewById(R.id.resolution)
        val captureInfo: TextView = view.findViewById(R.id.capture_info)
        val createdDate: TextView = view.findViewById(R.id.created_date)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gsr_raw_image_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val rawImageFile = rawImageFiles[position]

        holder.fileName.text = rawImageFile.name
        holder.fileSize.text = formatFileSize(rawImageFile.length())

        // Parse RAW image metadata from filename
        val filename = rawImageFile.nameWithoutExtension
        holder.resolution.text = "4032×3024 (12MP)" // Samsung S22 sensor size
        holder.captureInfo.text = "DNG RAW • Level 3"

        holder.createdDate.text =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(rawImageFile.lastModified()))

        holder.itemView.setOnClickListener {
            onItemClick(rawImageFile)
        }
    }

    override fun getItemCount() = rawImageFiles.size

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

/**
 * Adapter for GSR Sessions RecyclerView
 */
class GSRSessionAdapter(
    private val sessions: List<GSRSessionFragment.GSRSessionInfo>,
    private val onItemClick: (GSRSessionFragment.GSRSessionInfo) -> Unit,
) : RecyclerView.Adapter<GSRSessionAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sessionIcon: ImageView = view.findViewById(R.id.session_icon)
        val sessionId: TextView = view.findViewById(R.id.session_id)
        val participantInfo: TextView = view.findViewById(R.id.participant_info)
        val studyInfo: TextView = view.findViewById(R.id.study_info)
        val fileCount: TextView = view.findViewById(R.id.file_count)
        val duration: TextView = view.findViewById(R.id.duration)
        val startTime: TextView = view.findViewById(R.id.start_time)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gsr_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val session = sessions[position]

        holder.sessionId.text = session.sessionId
        holder.participantInfo.text = "Participant: ${session.participantId}"
        holder.studyInfo.text = "Study: ${session.studyName}"
        holder.startTime.text = session.startTime
        holder.duration.text = formatDuration(session.duration)

        // Count available files
        val fileCount = mutableListOf<String>()
        if (session.gsrDataFile != null) fileCount.add("GSR Data")
        if (session.videoFile != null) fileCount.add("Video")
        if (session.rawImageCount > 0) fileCount.add("${session.rawImageCount} RAW Images")

        holder.fileCount.text =
            if (fileCount.isEmpty()) {
                "No files"
            } else {
                fileCount.joinToString(" • ")
            }

        holder.itemView.setOnClickListener {
            onItemClick(session)
        }
    }

    override fun getItemCount() = sessions.size

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }
}
