package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.service.SessionManager
import com.csl.irCamera.R
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Production-Ready Session Management UI
 * Browse, manage, and delete recording sessions with comprehensive file cleanup
 */
class SessionManagerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionAdapter
    private lateinit var emptyView: View
    private lateinit var loadingView: View
    private lateinit var sessionManager: SessionManager
    private lateinit var searchView: SearchView
    private lateinit var filterSpinner: Spinner
    
    private val sessions = mutableListOf<SessionInfo>()
    private val filteredSessions = mutableListOf<SessionInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    companion object {
        private const val TAG = "SessionManagerActivity"
        
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SessionManagerActivity::class.java))
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_manager)
        
        initializeViews()
        setupSessionManager()
        setupRecyclerView()
        setupSearchAndFilter()
        loadSessions()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.sessions_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        loadingView = findViewById(R.id.loading_view)
        searchView = findViewById(R.id.search_view)
        filterSpinner = findViewById(R.id.filter_spinner)
        
        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session Manager"
    }
    
    private fun setupSessionManager() {
        sessionManager = SessionManager.getInstance(this)
    }
    
    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            context = this,
            sessions = filteredSessions,
            onSessionClick = { session -> openSessionDetails(session) },
            onSessionDelete = { session -> confirmDeleteSession(session) },
            onSessionExport = { session -> exportSession(session) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupSearchAndFilter() {
        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterSessions(query)
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                filterSessions(newText)
                return true
            }
        })
        
        // Filter spinner setup
        val filterOptions = arrayOf("All Sessions", "Active Sessions", "Completed Sessions", "With GSR", "With RGB", "With Thermal")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter
        
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilter(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadSessions() {
        showLoading(true)
        
        scope.launch {
            try {
                val loadedSessions = withContext(Dispatchers.IO) {
                    // Load from SessionManager and also scan for historical sessions
                    val activeSessions = sessionManager.getActiveSessions()
                    val historicalSessions = loadHistoricalSessions()
                    
                    (activeSessions + historicalSessions).distinctBy { it.sessionId }
                }
                
                sessions.clear()
                sessions.addAll(loadedSessions.sortedByDescending { it.startTime })
                filterSessions(searchView.query?.toString())
                
                Log.i(TAG, "Loaded ${sessions.size} sessions")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sessions", e)
                showError("Failed to load sessions: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private suspend fun loadHistoricalSessions(): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            val historicalSessions = mutableListOf<SessionInfo>()
            
            try {
                // Scan for session directories in external storage
                val baseDir = File(getExternalFilesDir(null), "recordings")
                if (baseDir.exists() && baseDir.isDirectory) {
                    baseDir.listFiles()?.forEach { sessionDir ->
                        if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
                            try {
                                val sessionInfo = parseSessionFromDirectory(sessionDir)
                                historicalSessions.add(sessionInfo)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse session from ${sessionDir.name}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load historical sessions", e)
            }
            
            historicalSessions
        }
    }
    
    private fun parseSessionFromDirectory(sessionDir: File): SessionInfo {
        val sessionId = sessionDir.name
        val metadataFile = File(sessionDir, "session_metadata.txt")
        
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            startTime = sessionDir.lastModified()
        )
        
        // Parse metadata if available
        if (metadataFile.exists()) {
            try {
                metadataFile.readLines().forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val value = parts[1]
                        when (key.trim()) {
                            "participantId" -> sessionInfo.participantId = value.trim()
                            "studyName" -> sessionInfo.studyName = value.trim()
                            "endTime" -> sessionInfo.endTime = value.trim().toLongOrNull()
                            "sampleCount" -> sessionInfo.sampleCount = value.trim().toLongOrNull() ?: 0
                            else -> sessionInfo.metadata[key.trim()] = value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse metadata for session $sessionId", e)
            }
        }
        
        // Check for available data files
        sessionInfo.hasGSRData = File(sessionDir, "gsr_data.csv").exists()
        sessionInfo.hasRGBData = File(sessionDir, "rgb_video.mp4").exists()
        sessionInfo.hasThermalData = File(sessionDir, "thermal_video.mp4").exists()
        
        return sessionInfo
    }
    
    private fun filterSessions(query: String?) {
        filteredSessions.clear()
        
        val filtered = if (query.isNullOrEmpty()) {
            sessions
        } else {
            sessions.filter { session ->
                session.sessionId.contains(query, ignoreCase = true) ||
                session.participantId?.contains(query, ignoreCase = true) == true ||
                session.studyName?.contains(query, ignoreCase = true) == true
            }
        }
        
        filteredSessions.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }
    
    private fun applyFilter(filterIndex: Int) {
        val baseList = if (searchView.query.isNullOrEmpty()) sessions else filteredSessions.toList()
        
        filteredSessions.clear()
        
        val filtered = when (filterIndex) {
            0 -> baseList // All Sessions
            1 -> baseList.filter { it.isActive() } // Active Sessions
            2 -> baseList.filter { !it.isActive() } // Completed Sessions
            3 -> baseList.filter { it.hasGSRData } // With GSR
            4 -> baseList.filter { it.hasRGBData } // With RGB
            5 -> baseList.filter { it.hasThermalData } // With Thermal
            else -> baseList
        }
        
        filteredSessions.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }
    
    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun updateEmptyView() {
        emptyView.visibility = if (filteredSessions.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun openSessionDetails(session: SessionInfo) {
        SessionDetailActivity.startActivity(this, session.sessionId)
    }
    
    private fun confirmDeleteSession(session: SessionInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Session")
            .setMessage("Are you sure you want to delete session '${session.sessionId}'?\n\nThis will permanently remove all associated data files including GSR recordings, videos, and metadata.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSession(session)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteSession(session: SessionInfo) {
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    deleteSessionFiles(session)
                }
                
                if (success) {
                    // Remove from active sessions if present
                    if (sessionManager.isSessionActive(session.sessionId)) {
                        sessionManager.completeSession(session.sessionId)
                    }
                    
                    // Remove from local lists
                    sessions.remove(session)
                    filteredSessions.remove(session)
                    adapter.notifyDataSetChanged()
                    updateEmptyView()
                    
                    Toast.makeText(this@SessionManagerActivity, "Session deleted successfully", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Session deleted: ${session.sessionId}")
                    
                } else {
                    showError("Failed to delete session files")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session", e)
                showError("Failed to delete session: ${e.message}")
            }
        }
    }
    
    private suspend fun deleteSessionFiles(session: SessionInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Delete session directory and all contents
                val sessionDir = File(getExternalFilesDir(null), "recordings/${session.sessionId}")
                if (sessionDir.exists()) {
                    sessionDir.deleteRecursively()
                }
                
                // Also check alternative directory structures
                val altSessionDir = File(getExternalFilesDir(null), session.sessionId)
                if (altSessionDir.exists()) {
                    altSessionDir.deleteRecursively()
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session files for ${session.sessionId}", e)
                false
            }
        }
    }
    
    private fun exportSession(session: SessionInfo) {
        // Launch session export functionality
        SessionExportActivity.startActivity(this, session.sessionId)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

/**
 * RecyclerView Adapter for Session List
 */
class SessionAdapter(
    private val context: Context,
    private val sessions: List<SessionInfo>,
    private val onSessionClick: (SessionInfo) -> Unit,
    private val onSessionDelete: (SessionInfo) -> Unit,
    private val onSessionExport: (SessionInfo) -> Unit
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {
    
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.session_title)
        val subtitleText: TextView = view.findViewById(R.id.session_subtitle)
        val statusText: TextView = view.findViewById(R.id.session_status)
        val dataTypesText: TextView = view.findViewById(R.id.session_data_types)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        val exportButton: ImageButton = view.findViewById(R.id.export_button)
        val cardView: View = view
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        
        // Title: Session ID or participant name
        holder.titleText.text = session.participantId ?: session.sessionId
        
        // Subtitle: Study name and date
        val studyText = session.studyName ?: "Unnamed Study"
        val dateText = dateFormatter.format(Date(session.startTime))
        holder.subtitleText.text = "$studyText • $dateText"
        
        // Status
        val statusText = if (session.isActive()) {
            "🟢 Active"
        } else {
            val duration = if (session.endTime != null) {
                val durationMs = session.endTime!! - session.startTime
                val minutes = durationMs / (1000 * 60)
                "${minutes}min"
            } else {
                "Unknown duration"
            }
            "⚪ Completed • $duration"
        }
        holder.statusText.text = statusText
        
        // Data types available
        val dataTypes = mutableListOf<String>()
        if (session.hasGSRData) dataTypes.add("GSR")
        if (session.hasRGBData) dataTypes.add("RGB")
        if (session.hasThermalData) dataTypes.add("Thermal")
        
        holder.dataTypesText.text = if (dataTypes.isNotEmpty()) {
            "📊 ${dataTypes.joinToString(", ")}"
        } else {
            "📊 No data files found"
        }
        
        // Click handlers
        holder.cardView.setOnClickListener { onSessionClick(session) }
        holder.deleteButton.setOnClickListener { onSessionDelete(session) }
        holder.exportButton.setOnClickListener { onSessionExport(session) }
    }
    
    override fun getItemCount(): Int = sessions.size
}