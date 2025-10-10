package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class ExportFormat(
    val displayName: String,
) {
    CSV("CSV (Comma Separated Values)"),
    JSON("JSON (JavaScript Object Notation)"),
    XML("XML (eXtensible Markup Language)"),
    EXCEL("Excel Spreadsheet"),
}

enum class ExportDestination(
    val displayName: String,
) {
    DOWNLOADS("Downloads Folder"),
    EXTERNAL_STORAGE("External Storage"),
    SHARE("Share with Other Apps"),
    EMAIL("Email Export"),
}

data class GSRSession(
    val sessionId: String,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val deviceId: String,
    val participantId: String?,
    val readingCount: Int,
    val avgConductance: Float,
    val status: String = "COMPLETED",
    val duration: String = "0min",
    val dataPointCount: Int = 0,
    val filePath: String = "",
    val lastModified: Long = 0L,
)

@HiltViewModel
class SessionExportViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AppBaseViewModel() {
        private val application: Context = context.applicationContext

        data class SessionExportState(
            val isLoading: Boolean = false,
            val error: String? = null,
            val sessions: List<GSRSession> = emptyList(),
            val selectedSessions: Set<GSRSession> = emptySet(),
            val exportFormat: ExportFormat = ExportFormat.CSV,
            val exportDestination: ExportDestination = ExportDestination.DOWNLOADS,
            val isExporting: Boolean = false,
            val exportProgress: Float = 0f,
            val currentExportFile: String? = null,
        )

        private val _exportState = MutableStateFlow(SessionExportState())
        val exportState: StateFlow<SessionExportState> = _exportState.asStateFlow()

        init {
            loadSessions()
        }

        fun loadSessions() {
            viewModelScope.launch {
                _exportState.value = _exportState.value.copy(isLoading = true, error = null)
                try {
                    val sessions = getAvailableSessions()
                    _exportState.value =
                        _exportState.value.copy(
                            isLoading = false,
                            sessions = sessions,
                        )
                } catch (e: Exception) {
                    _exportState.value =
                        _exportState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load sessions",
                        )
                }
            }
        }

        fun toggleSessionSelection(session: GSRSession) {
            val currentSelection = _exportState.value.selectedSessions
            val newSelection =
                if (session in currentSelection) {
                    currentSelection - session
                } else {
                    currentSelection + session
                }
            _exportState.value = _exportState.value.copy(selectedSessions = newSelection)
        }

        fun setExportFormat(format: ExportFormat) {
            _exportState.value = _exportState.value.copy(exportFormat = format)
        }

        fun setExportDestination(destination: ExportDestination) {
            _exportState.value = _exportState.value.copy(exportDestination = destination)
        }

        fun startExport() {
            viewModelScope.launch {
                val selectedSessions = _exportState.value.selectedSessions
                if (selectedSessions.isEmpty()) {
                    _exportState.value = _exportState.value.copy(error = "No sessions selected for export")
                    return@launch
                }
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = true,
                        exportProgress = 0f,
                        error = null,
                    )
                try {
                    val exportFiles = mutableListOf<File>()
                    val totalSessions = selectedSessions.size
                    selectedSessions.forEachIndexed { index, session ->
                        _exportState.value =
                            _exportState.value.copy(
                                currentExportFile = session.name,
                                exportProgress = (index.toFloat() / totalSessions),
                            )
                        val exportedFile = exportSession(session)
                        exportFiles.add(exportedFile)
                    }
                    _exportState.value =
                        _exportState.value.copy(
                            exportProgress = 1f,
                            currentExportFile = null,
                        )
                    // Handle export destination
                    handleExportDestination(exportFiles)
                } catch (e: Exception) {
                    _exportState.value =
                        _exportState.value.copy(
                            isExporting = false,
                            exportProgress = 0f,
                            currentExportFile = null,
                            error = "Export failed: ${e.message}",
                        )
                }
            }
        }

        private fun getAvailableSessions(): List<GSRSession> {
            val sessions = mutableListOf<GSRSession>()
            // Check multiple possible session directories
            val possibleDirectories =
                listOf(
                    File(Environment.getExternalStorageDirectory(), "GSR/Sessions"),
                    File(Environment.getExternalStorageDirectory(), "IRCamera/GSR/Sessions"),
                    File(application.getExternalFilesDir(null), "gsr_sessions"),
                    File(application.filesDir, "gsr_sessions"),
                )
            for (directory in possibleDirectories) {
                if (directory.exists() && directory.isDirectory) {
                    directory
                        .listFiles { file ->
                            file.isFile && (file.name.endsWith(".csv") || file.name.endsWith(".txt") || file.name.endsWith(".json"))
                        }?.forEach { file ->
                            sessions.add(
                                GSRSession(
                                    sessionId = file.nameWithoutExtension,
                                    name = file.nameWithoutExtension,
                                    startTime = file.lastModified(),
                                    endTime = null,
                                    deviceId = "unknown",
                                    participantId = null,
                                    readingCount = countDataPoints(file),
                                    avgConductance = 0f,
                                    status = "COMPLETED",
                                    duration = calculateSessionDuration(file),
                                    dataPointCount = countDataPoints(file),
                                    filePath = file.absolutePath,
                                    lastModified = file.lastModified(),
                                ),
                            )
                        }
                }
            }
            // Sort by modification date (newest first)
            return sessions.sortedByDescending { it.lastModified }
        }

        private suspend fun exportSession(session: GSRSession): File {
            val outputDir = getExportDirectory()
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${session.name}_export_$timestamp.${_exportState.value.exportFormat.fileExtension}"
            val outputFile = File(outputDir, fileName)
            when (_exportState.value.exportFormat) {
                ExportFormat.CSV -> exportToCSV(session, outputFile)
                ExportFormat.JSON -> exportToJSON(session, outputFile)
                ExportFormat.XML -> exportToXML(session, outputFile)
                ExportFormat.EXCEL -> exportToExcel(session, outputFile)
            }
            return outputFile
        }

        private fun exportToCSV(
            session: GSRSession,
            outputFile: File,
        ) {
            val sessionFile = File(session.filePath)
            val writer = FileWriter(outputFile)
            writer.use { w ->
                // Write CSV header
                w.write("Timestamp,GSR_Value,Resistance,Conductance,Status\n")
                // Read and convert session data
                sessionFile.readLines().forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        val convertedLine = convertDataLineToCSV(line)
                        w.write("$convertedLine\n")
                    }
                }
            }
        }

        private fun exportToJSON(
            session: GSRSession,
            outputFile: File,
        ) {
            val sessionFile = File(session.filePath)
            val writer = FileWriter(outputFile)
            writer.use { w ->
                w.write("{\n")
                w.write("  \"session\": {\n")
                w.write("    \"name\": \"${session.name}\",\n")
                w.write("    \"duration\": \"${session.duration}\",\n")
                w.write("    \"dataPointCount\": ${session.dataPointCount},\n")
                w.write(
                    "    \"exportedAt\": \"${
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(
                            Date(),
                        )
                    }\",\n",
                )
                w.write("    \"data\": [\n")
                val lines = sessionFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
                lines.forEachIndexed { index, line ->
                    val jsonLine = convertDataLineToJSON(line)
                    w.write("      $jsonLine")
                    if (index < lines.size - 1) w.write(",")
                    w.write("\n")
                }
                w.write("    ]\n")
                w.write("  }\n")
                w.write("}\n")
            }
        }

        private fun exportToXML(
            session: GSRSession,
            outputFile: File,
        ) {
            val sessionFile = File(session.filePath)
            val writer = FileWriter(outputFile)
            writer.use { w ->
                w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                w.write("<gsrSession>\n")
                w.write("  <metadata>\n")
                w.write("    <name>${session.name}</name>\n")
                w.write("    <duration>${session.duration}</duration>\n")
                w.write("    <dataPointCount>${session.dataPointCount}</dataPointCount>\n")
                w.write(
                    "    <exportedAt>${
                        SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.getDefault(),
                        ).format(Date())
                    }</exportedAt>\n",
                )
                w.write("  </metadata>\n")
                w.write("  <data>\n")
                sessionFile.readLines().forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        val xmlLine = convertDataLineToXML(line)
                        w.write("    $xmlLine\n")
                    }
                }
                w.write("  </data>\n")
                w.write("</gsrSession>\n")
            }
        }

        private fun exportToExcel(
            session: GSRSession,
            outputFile: File,
        ) {
            // For now, export as CSV with Excel-compatible format
            // In a full implementation, you'd use Apache POI or similar library
            exportToCSV(session, outputFile)
        }

        private suspend fun handleExportDestination(exportFiles: List<File>) {
            try {
                when (_exportState.value.exportDestination) {
                    ExportDestination.DOWNLOADS -> {
                        // Files are already in downloads, just notify completion
                        _exportState.value =
                            _exportState.value.copy(
                                isExporting = false,
                                error = "Export completed! Files saved to Downloads folder.",
                            )
                    }

                    ExportDestination.EXTERNAL_STORAGE -> {
                        // Files are already in external storage
                        _exportState.value =
                            _exportState.value.copy(
                                isExporting = false,
                                error = "Export completed! Files saved to external storage.",
                            )
                    }

                    ExportDestination.SHARE -> {
                        shareFiles(exportFiles)
                    }

                    ExportDestination.EMAIL -> {
                        emailFiles(exportFiles)
                    }
                }
            } catch (e: Exception) {
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed but failed to handle destination: ${e.message}",
                    )
            }
        }

        private fun shareFiles(files: List<File>) {
            try {
                val context = application.applicationContext
                val uris =
                    files.map { file ->
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                val intent =
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(Intent.createChooser(intent, "Share GSR Export"))
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Opening share dialog...",
                    )
            } catch (e: Exception) {
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed but failed to share: ${e.message}",
                    )
            }
        }

        private fun emailFiles(files: List<File>) {
            try {
                val context = application.applicationContext
                val uris =
                    files.map { file ->
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                val intent =
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_SUBJECT, "GSR Session Export")
                        putExtra(Intent.EXTRA_TEXT, "Attached are the exported GSR session files.")
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(Intent.createChooser(intent, "Email GSR Export"))
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Opening email client...",
                    )
            } catch (e: Exception) {
                _exportState.value =
                    _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed but failed to email: ${e.message}",
                    )
            }
        }

        private fun getExportDirectory(): File =
            when (_exportState.value.exportDestination) {
                ExportDestination.DOWNLOADS ->
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "GSR_Exports",
                    )

                ExportDestination.EXTERNAL_STORAGE ->
                    File(
                        Environment.getExternalStorageDirectory(),
                        "IRCamera/GSR_Exports",
                    )

                else -> File(application.getExternalFilesDir(null), "exports")
            }

        private fun calculateSessionDuration(file: File): String {
            // Simple duration calculation based on file timestamps
            // In a real implementation, you'd parse the actual session data
            val durationMinutes = (file.length() / 1000).coerceAtMost(999)
            return "${durationMinutes}min"
        }

        private fun countDataPoints(file: File): Int =
            try {
                file.readLines().count { line ->
                    line.isNotBlank() && !line.startsWith("#")
                }
            } catch (e: Exception) {
                0
            }

        private fun convertDataLineToCSV(line: String): String {
            // Convert data line to CSV format
            // This is a simplified conversion - adjust based on actual data format
            return line.replace("\t", ",")
        }

        private fun convertDataLineToJSON(line: String): String {
            // Convert data line to JSON format
            val parts = line.split("\t", ",")
            return if (parts.size >= 2) {
                "{ \"timestamp\": \"${parts[0]}\", \"value\": ${parts[1]} }"
            } else {
                "{ \"data\": \"$line\" }"
            }
        }

        private fun convertDataLineToXML(line: String): String {
            // Convert data line to XML format
            val parts = line.split("\t", ",")
            return if (parts.size >= 2) {
                "<dataPoint timestamp=\"${parts[0]}\" value=\"${parts[1]}\" />"
            } else {
                "<dataPoint data=\"$line\" />"
            }
        }
    }

// Extension property for file extensions
private val ExportFormat.fileExtension: String
    get() =
        when (this) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.XML -> "xml"
            ExportFormat.EXCEL -> "xlsx"
        }
