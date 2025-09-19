package com.topdon.tc001.sensors.gsr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GSRDataFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: GSRDataAdapter
    private val dataFiles = mutableListOf<GSRDataFile>()

    data class GSRDataFile(
        val file: File,
        val sessionId: String,
        val participantId: String,
        val sampleCount: Long,
        val duration: Long,
        val createdDate: String,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_gsr_data, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.gsr_data_recycler)
        emptyView = view.findViewById(R.id.empty_view)

        setupRecyclerView()
        loadGSRDataFiles()
    }

    private fun setupRecyclerView() {
        adapter =
            GSRDataAdapter(dataFiles) { dataFile ->
                openDataFile(dataFile)
            }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadGSRDataFiles() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val files = findGSRDataFiles()
            withContext(Dispatchers.Main) {
                dataFiles.clear()
                dataFiles.addAll(files)
                adapter.notifyDataSetChanged()

                if (files.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun findGSRDataFiles(): List<GSRDataFile> {
        val files = mutableListOf<GSRDataFile>()

        val recordingDir = File(context?.getExternalFilesDir(null), "GSR_Recordings")
        if (recordingDir.exists()) {
            recordingDir.listFiles { file ->
                file.isFile && file.extension == "csv" && file.name.contains("gsr_data")
            }?.forEach { file ->
                try {
                    val metadata = parseGSRFileMetadata(file)
                    files.add(metadata)
                } catch (e: Exception) {

                }
            }
        }

        return files.sortedByDescending { it.file.lastModified() }
    }

    private fun parseGSRFileMetadata(file: File): GSRDataFile {

        val filename = file.nameWithoutExtension
        val parts = filename.split("_")

        val sessionId = parts.getOrNull(2) ?: "Unknown"
        val participantId = parts.getOrNull(3) ?: "Unknown"

        val sampleCount =
            try {
                file.readLines().size - 1L // Subtract header
            } catch (e: Exception) {
                0L
            }

        val duration = sampleCount / 128 // seconds

        val createdDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(file.lastModified()))

        return GSRDataFile(
            file = file,
            sessionId = sessionId,
            participantId = participantId,
            sampleCount = sampleCount,
            duration = duration,
            createdDate = createdDate,
        )
    }

    private fun openDataFile(dataFile: GSRDataFile) {

        GSRDataViewActivity.startActivity(requireContext(), dataFile.file.absolutePath)
    }
}

class GSRVideoFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: GSRVideoAdapter
    private val videoFiles = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_gsr_video, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.gsr_video_recycler)
        emptyView = view.findViewById(R.id.empty_view)

        setupRecyclerView()
        loadVideoFiles()
    }

    private fun setupRecyclerView() {
        adapter =
            GSRVideoAdapter(videoFiles) { videoFile ->
                openVideoFile(videoFile)
            }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadVideoFiles() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val files = findVideoFiles()
            withContext(Dispatchers.Main) {
                videoFiles.clear()
                videoFiles.addAll(files)
                adapter.notifyDataSetChanged()

                if (files.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun findVideoFiles(): List<File> {
        val files = mutableListOf<File>()

        val recordingDir = File(context?.getExternalFilesDir(null), "GSR_Recordings")
        if (recordingDir.exists()) {
            recordingDir.listFiles { file ->
                file.isFile && (file.extension == "mp4" || file.extension == "mov")
            }?.let { files.addAll(it) }
        }

        return files.sortedByDescending { it.lastModified() }
    }

    private fun openVideoFile(videoFile: File) {

        GSRVideoPlayerActivity.startActivity(requireContext(), videoFile.absolutePath)
    }
}

class GSRRawImageFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: GSRRawImageAdapter
    private val rawImageFiles = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_gsr_raw_image, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.gsr_raw_image_recycler)
        emptyView = view.findViewById(R.id.empty_view)

        setupRecyclerView()
        loadRawImageFiles()
    }

    private fun setupRecyclerView() {
        adapter =
            GSRRawImageAdapter(rawImageFiles) { rawImageFile ->
                openRawImageFile(rawImageFile)
            }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadRawImageFiles() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val files = findRawImageFiles()
            withContext(Dispatchers.Main) {
                rawImageFiles.clear()
                rawImageFiles.addAll(files)
                adapter.notifyDataSetChanged()

                if (files.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun findRawImageFiles(): List<File> {
        val files = mutableListOf<File>()

        val recordingDir = File(context?.getExternalFilesDir(null), "GSR_Recordings")
        if (recordingDir.exists()) {
            recordingDir.listFiles { file ->
                file.isFile && file.extension == "dng"
            }?.let { files.addAll(it) }
        }

        return files.sortedByDescending { it.lastModified() }
    }

    private fun openRawImageFile(rawImageFile: File) {

        GSRRawImageViewActivity.startActivity(requireContext(), rawImageFile.absolutePath)
    }
}

class GSRSessionFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: GSRSessionAdapter
    private val sessions = mutableListOf<GSRSessionInfo>()

    data class GSRSessionInfo(
        val sessionId: String,
        val participantId: String,
        val studyName: String,
        val startTime: String,
        val duration: Long,
        val gsrDataFile: File?,
        val videoFile: File?,
        val rawImageCount: Int,
        val sessionDirectory: File,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_gsr_session, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.gsr_session_recycler)
        emptyView = view.findViewById(R.id.empty_view)

        setupRecyclerView()
        loadSessions()
    }

    private fun setupRecyclerView() {
        adapter =
            GSRSessionAdapter(sessions) { session ->
                openSessionDetails(session)
            }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadSessions() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val sessionList = findCompleteSessions()
            withContext(Dispatchers.Main) {
                sessions.clear()
                sessions.addAll(sessionList)
                adapter.notifyDataSetChanged()

                if (sessionList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun findCompleteSessions(): List<GSRSessionInfo> {
        val sessions = mutableListOf<GSRSessionInfo>()

        val recordingDir = File(context?.getExternalFilesDir(null), "GSR_Recordings")
        if (recordingDir.exists()) {
            recordingDir.listFiles { file -> file.isDirectory }?.forEach { sessionDir ->
                try {
                    val sessionInfo = parseSessionDirectory(sessionDir)
                    sessions.add(sessionInfo)
                } catch (e: Exception) {

                }
            }
        }

        return sessions.sortedByDescending { it.sessionDirectory.lastModified() }
    }

    private fun parseSessionDirectory(sessionDir: File): GSRSessionInfo {
        val sessionId = sessionDir.name

        val gsrDataFile =
            sessionDir.listFiles { file -> file.extension == "csv" && file.name.contains("gsr_data") }
                ?.firstOrNull()
        val videoFile =
            sessionDir.listFiles { file -> file.extension == "mp4" || file.extension == "mov" }
                ?.firstOrNull()
        val rawImageCount = sessionDir.listFiles { file -> file.extension == "dng" }?.size ?: 0

        val parts = sessionId.split("_")
        val participantId = parts.getOrNull(1) ?: "Unknown"
        val studyName = parts.getOrNull(2) ?: "MultiModal Study"

        val startTime =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(sessionDir.lastModified()))

        val duration =
            gsrDataFile?.let { file: File ->
                try {
                    val sampleCount = file.readLines().size - 1L
                    sampleCount / 128 // seconds at 128 Hz
                } catch (e: Exception) {
                    0L
                }
            } ?: 0L

        return GSRSessionInfo(
            sessionId = sessionId,
            participantId = participantId,
            studyName = studyName,
            startTime = startTime,
            duration = duration,
            gsrDataFile = gsrDataFile,
            videoFile = videoFile,
            rawImageCount = rawImageCount,
            sessionDirectory = sessionDir,
        )
    }

    private fun openSessionDetails(session: GSRSessionInfo) {
        SessionDetailActivity.startActivity(requireContext(), session.sessionId)
    }
}
