package mpdc4gsr.sensors.gsr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ItemGsrDataFileBinding
import com.csl.irCamera.databinding.ItemGsrRawImageFileBinding
import com.csl.irCamera.databinding.ItemGsrVideoFileBinding
import java.io.File

class GSRDataAdapter(
    private val dataFiles: List<GSRDataFragment.GSRDataFile>,
    private val onItemClick: (GSRDataFragment.GSRDataFile) -> Unit,
) : RecyclerView.Adapter<GSRDataAdapter.ViewHolder>() {
    class ViewHolder(private val binding: ItemGsrDataFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val fileIcon = binding.fileIcon
        val fileName = binding.fileName
        val sessionInfo = binding.sessionInfo
        val fileSize = binding.fileSize
        val sampleCount = binding.sampleCount
        val duration = binding.duration
        val createdDate = binding.createdDate
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemGsrDataFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val dataFile = dataFiles[position]

        holder.fileName.text = dataFile.file.name
        holder.sessionInfo.text =
            "Session: ${dataFile.sessionId} | Participant: ${dataFile.participantId}"
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

class GSRVideoAdapter(
    private val videoFiles: List<File>,
    private val onItemClick: (File) -> Unit,
) : RecyclerView.Adapter<GSRVideoAdapter.ViewHolder>() {
    class ViewHolder(private val binding: ItemGsrVideoFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val videoThumbnail = binding.videoThumbnail
        val fileName = binding.fileName
        val fileSize = binding.fileSize
        val duration = binding.duration
        val resolution = binding.resolution
        val createdDate = binding.createdDate
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemGsrVideoFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val videoFile = videoFiles[position]

        holder.fileName.text = videoFile.name
        holder.fileSize.text = formatFileSize(videoFile.length())

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

class GSRRawImageAdapter(
    private val rawImageFiles: List<File>,
    private val onItemClick: (File) -> Unit,
) : RecyclerView.Adapter<GSRRawImageAdapter.ViewHolder>() {
    class ViewHolder(private val binding: ItemGsrRawImageFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imageThumbnail = binding.imageThumbnail
        val fileName = binding.fileName
        val fileSize = binding.fileSize
        val resolution = binding.resolution
        val captureInfo = binding.captureInfo
        val createdDate = binding.createdDate
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemGsrRawImageFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val rawImageFile = rawImageFiles[position]

        holder.fileName.text = rawImageFile.name
        holder.fileSize.text = formatFileSize(rawImageFile.length())

        val filename = rawImageFile.nameWithoutExtension
        holder.resolution.text = "4032×3024 (12MP)"
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
                .inflate(R.layout.item_sensor_data_consolidated, parent, false)
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
