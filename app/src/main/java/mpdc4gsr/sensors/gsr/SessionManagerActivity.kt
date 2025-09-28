package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivitySessionManagerWithNavBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import com.mpdc4gsr.libunified.app.navigation.BottomNavigationHelper
import mpdc4gsr.activities.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionManagerActivity : BaseViewModelActivity<SessionManagerViewModel>(), BottomNavigationHelper.NavigationHandler {
    private lateinit var binding: ActivitySessionManagerWithNavBinding
    private lateinit var adapter: SessionAdapter

    companion object {
        private const val TAG = "SessionManagerActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SessionManagerActivity::class.java))
        }
    }

    override fun providerVMClass(): Class<SessionManagerViewModel> =
        SessionManagerViewModel::class.java

    override fun initContentView() = R.layout.activity_session_manager_with_nav

    override fun initView() {
        binding = ActivitySessionManagerWithNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupRecyclerView()
        setupSearchAndFilter()
        setupObservers()

        // Load sessions
        viewModel.loadSessions(this)
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    private fun initializeViews() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session Manager"
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Setup navigation click listeners for the included bottom navigation
        binding.bottomNavigation.clNavGallery.setOnClickListener {
            handleNavigation(BottomNavigationHelper.NavigationPage.GALLERY)
        }
        
        binding.bottomNavigation.clNavMain.setOnClickListener {
            handleNavigation(BottomNavigationHelper.NavigationPage.MAIN)
        }
        
        binding.bottomNavigation.clNavMine.setOnClickListener {
            handleNavigation(BottomNavigationHelper.NavigationPage.MINE)
        }
        
        // Set current page as MAIN (session management is part of main functionality)
        updateNavigationSelection(BottomNavigationHelper.NavigationPage.MAIN)
    }
    
    override fun navigateToPage(page: BottomNavigationHelper.NavigationPage) {
        val pageIndex = when (page) {
            BottomNavigationHelper.NavigationPage.GALLERY -> 0
            BottomNavigationHelper.NavigationPage.MAIN -> 1
            BottomNavigationHelper.NavigationPage.MINE -> 2
        }
        
        try {
            val intent = Intent().apply {
                setClassName(this@SessionManagerActivity, MainActivity::class.java.name)
                putExtra("page", pageIndex)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Navigation failed", e)
        }
    }
    
    private fun handleNavigation(page: BottomNavigationHelper.NavigationPage) {
        try {
            when (page) {
                BottomNavigationHelper.NavigationPage.GALLERY -> {
                    val intent = Intent()
                    intent.setClassName(this, "mpdc4gsr.activities.MainActivity")
                    intent.putExtra("page", 0)
                    startActivity(intent)
                    finish()
                }
                BottomNavigationHelper.NavigationPage.MAIN -> {
                    val intent = Intent()
                    intent.setClassName(this, "mpdc4gsr.activities.MainActivity")
                    intent.putExtra("page", 1)
                    startActivity(intent)
                    finish()
                }
                BottomNavigationHelper.NavigationPage.MINE -> {
                    val intent = Intent()
                    intent.setClassName(this, "mpdc4gsr.activities.MainActivity")
                    intent.putExtra("page", 2)
                    startActivity(intent)
                    finish()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Navigation failed", e)
        }
    }
    
    private fun updateNavigationSelection(currentPage: BottomNavigationHelper.NavigationPage) {
        // Reset all selections
        binding.bottomNavigation.ivNavGallery.isSelected = false
        binding.bottomNavigation.tvNavGallery.isSelected = false
        binding.bottomNavigation.ivNavMain.isSelected = false
        binding.bottomNavigation.tvNavMain.isSelected = false
        binding.bottomNavigation.ivNavMine.isSelected = false
        binding.bottomNavigation.tvNavMine.isSelected = false
        
        // Set current selection
        when (currentPage) {
            BottomNavigationHelper.NavigationPage.GALLERY -> {
                binding.bottomNavigation.ivNavGallery.isSelected = true
                binding.bottomNavigation.tvNavGallery.isSelected = true
                binding.bottomNavigation.ivBottomBg.setImageResource(R.drawable.ic_main_bg_not_select)
            }
            BottomNavigationHelper.NavigationPage.MAIN -> {
                binding.bottomNavigation.ivNavMain.isSelected = true
                binding.bottomNavigation.tvNavMain.isSelected = true
                binding.bottomNavigation.ivBottomBg.setImageResource(R.drawable.ic_main_bg_select)
            }
            BottomNavigationHelper.NavigationPage.MINE -> {
                binding.bottomNavigation.ivNavMine.isSelected = true
                binding.bottomNavigation.tvNavMine.isSelected = true
                binding.bottomNavigation.ivBottomBg.setImageResource(R.drawable.ic_main_bg_not_select)
            }
        }
    }

    private fun setupObservers() {
        viewModel.filteredSessions.observe(this) { sessions ->
            adapter.updateSessions(sessions)
        }

        viewModel.loading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                showError(error)
                viewModel.clearError()
            }
        }

        viewModel.storageInfo.observe(this) { storageInfo ->
            val storageText =
                "Storage: ${storageInfo.formattedAvailable} available (${100 - storageInfo.usagePercentage}% free)"
            supportActionBar?.subtitle = storageText

            if (storageInfo.isLowStorage) {
                Toast.makeText(
                    this,
                    "Warning: Low storage space! Only ${storageInfo.formattedAvailable} available.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.sessionAction.observe(this) { action ->
            action?.let {
                handleSessionAction(it)
                viewModel.clearSessionAction()
            }
        }
    }

    private fun handleSessionAction(action: SessionManagerViewModel.SessionAction) {
        when (action.type) {
            SessionManagerViewModel.ActionType.OPEN_DETAILS -> {
                openSessionDetails(action.session)
            }

            SessionManagerViewModel.ActionType.DELETE_CONFIRM -> {
                confirmDeleteSession(action.session)
            }

            SessionManagerViewModel.ActionType.EXPORT -> {
                viewModel.exportSession(action.session)
            }

            SessionManagerViewModel.ActionType.DELETED_SUCCESS -> {
                action.message?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }

            SessionManagerViewModel.ActionType.EXPORT_SUCCESS -> {
                action.message?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }

            SessionManagerViewModel.ActionType.EXPORT_FAILED -> {
                action.message?.let { message ->
                    showError(message)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            context = this,
            sessions = mutableListOf(),
            onSessionClick = { session -> viewModel.onSessionClick(session) },
            onSessionDelete = { session -> viewModel.onSessionDelete(session) },
            onSessionExport = { session -> viewModel.onSessionExport(session) },
        )

        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sessionsRecyclerView.adapter = adapter
    }

    private fun setupSearchAndFilter() {
        binding.searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.filterSessions(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.filterSessions(newText)
                    return true
                }
            },
        )

        val filterOptions = arrayOf("All Sessions", "Recent", "Completed", "With Data")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filterOptions,
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = spinnerAdapter

        binding.sortSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    viewModel.filterSessionsByType(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
    }

    private fun showLoading(show: Boolean) {
        // binding.loadingView.isVisible = show
        // Loading indication can be handled by showing a progress dialog or similar
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
            .setMessage("Are you sure you want to delete session ${session.sessionId}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSession(session)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // SessionAdapter remains the same but needs updateSessions method
    class SessionAdapter(
        private val context: Context,
        private var sessions: MutableList<SessionInfo>,
        private val onSessionClick: (SessionInfo) -> Unit,
        private val onSessionDelete: (SessionInfo) -> Unit,
        private val onSessionExport: (SessionInfo) -> Unit,
    ) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

        fun updateSessions(newSessions: List<SessionInfo>) {
            sessions.clear()
            sessions.addAll(newSessions)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            bind(holder, session)
        }

        override fun getItemCount(): Int = sessions.size

        private fun bind(holder: ViewHolder, session: SessionInfo) {
            holder.sessionIdText.text = session.sessionId
            holder.participantText.text = "Participant: ${session.participantId ?: "Unknown"}"
            holder.studyText.text = "Study: ${session.studyName ?: "N/A"}"

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            holder.startTimeText.text = "Started: ${dateFormat.format(Date(session.startTime))}"

            if (session.endTime != null) {
                holder.endTimeText.text = "Ended: ${dateFormat.format(Date(session.endTime!!))}"
                holder.endTimeText.isVisible = true
            } else {
                holder.endTimeText.isVisible = false
            }

            holder.sampleCountText.text = "Samples: ${session.sampleCount}"

            val dataSizeText = if (session.totalDataSize > 0) {
                val sizeInMB = session.totalDataSize / (1024 * 1024)
                "Data: ${sizeInMB}MB"
            } else {
                "Data: No files"
            }
            holder.dataSizeText.text = dataSizeText

            val gsrCount = session.metadata["gsrFileCount"]?.toIntOrNull() ?: 0
            val thermalCount = session.metadata["thermalFileCount"]?.toIntOrNull() ?: 0
            val rgbCount = session.metadata["rgbFileCount"]?.toIntOrNull() ?: 0

            val dataTypes = mutableListOf<String>()
            if (gsrCount > 0) dataTypes.add("GSR ($gsrCount)")
            if (thermalCount > 0) dataTypes.add("Thermal ($thermalCount)")
            if (rgbCount > 0) dataTypes.add("RGB ($rgbCount)")

            holder.dataTypesText.text = if (dataTypes.isNotEmpty()) {
                "📊 ${dataTypes.joinToString(", ")}"
            } else {
                "📊 No data files found"
            }

            holder.cardView.setOnClickListener { onSessionClick(session) }
            holder.deleteButton.setOnClickListener { onSessionDelete(session) }
            holder.exportButton.setOnClickListener { onSessionExport(session) }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardView: View = view.findViewById(R.id.session_card)
            val sessionIdText: TextView = view.findViewById(R.id.session_id_text)
            val participantText: TextView = view.findViewById(R.id.participant_text)
            val studyText: TextView = view.findViewById(R.id.study_text)
            val startTimeText: TextView = view.findViewById(R.id.start_time_text)
            val endTimeText: TextView = view.findViewById(R.id.end_time_text)
            val sampleCountText: TextView = view.findViewById(R.id.sample_count_text)
            val dataSizeText: TextView = view.findViewById(R.id.data_size_text)
            val dataTypesText: TextView = view.findViewById(R.id.data_types_text)
            val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
            val exportButton: ImageButton = view.findViewById(R.id.export_button)
        }
    }
}
