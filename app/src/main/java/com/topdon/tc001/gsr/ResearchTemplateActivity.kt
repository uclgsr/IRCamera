package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.gsr.model.ResearchTemplate
import com.csl.irCamera.R

/**
 * Research Template Selection Activity
 * Allows users to choose from predefined research templates or create custom configurations
 */
class ResearchTemplateActivity : AppCompatActivity() {
    
    private lateinit var categorySpinner: Spinner
    private lateinit var templatesRecyclerView: RecyclerView
    private lateinit var templateAdapter: TemplateAdapter
    private lateinit var emptyView: View
    private lateinit var selectedTemplateContainer: LinearLayout
    private lateinit var selectedTemplateTitle: TextView
    private lateinit var selectedTemplateDescription: TextView
    private lateinit var selectedTemplateInstructions: TextView
    private lateinit var startRecordingButton: Button
    
    private var selectedTemplate: ResearchTemplate? = null
    private val allTemplates = ResearchTemplate.PREDEFINED_TEMPLATES
    private val filteredTemplates = mutableListOf<ResearchTemplate>()
    
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateActivity::class.java))
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_research_template)
        
        initializeViews()
        setupCategoryFilter()
        setupTemplateGrid()
        loadTemplates()
    }
    
    private fun initializeViews() {
        categorySpinner = findViewById(R.id.category_spinner)
        templatesRecyclerView = findViewById(R.id.templates_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        selectedTemplateContainer = findViewById(R.id.selected_template_container)
        selectedTemplateTitle = findViewById(R.id.selected_template_title)
        selectedTemplateDescription = findViewById(R.id.selected_template_description)
        selectedTemplateInstructions = findViewById(R.id.selected_template_instructions)
        startRecordingButton = findViewById(R.id.start_recording_button)
        
        supportActionBar?.title = "Research Templates"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        startRecordingButton.setOnClickListener {
            selectedTemplate?.let { template ->
                startRecordingWithTemplate(template)
            }
        }
    }
    
    private fun setupCategoryFilter() {
        val categories = listOf("All Templates") + ResearchTemplate.TemplateCategory.values().map { it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() } }
        
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterTemplatesByCategory(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupTemplateGrid() {
        templateAdapter = TemplateAdapter(
            context = this,
            templates = filteredTemplates,
            onTemplateSelected = { template -> selectTemplate(template) }
        )
        
        templatesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        templatesRecyclerView.adapter = templateAdapter
    }
    
    private fun loadTemplates() {
        filteredTemplates.clear()
        filteredTemplates.addAll(allTemplates)
        templateAdapter.notifyDataSetChanged()
        updateEmptyView()
    }
    
    private fun filterTemplatesByCategory(categoryIndex: Int) {
        filteredTemplates.clear()
        
        if (categoryIndex == 0) {
            // All templates
            filteredTemplates.addAll(allTemplates)
        } else {
            // Filter by specific category
            val category = ResearchTemplate.TemplateCategory.values()[categoryIndex - 1]
            filteredTemplates.addAll(ResearchTemplate.getTemplatesByCategory(category))
        }
        
        templateAdapter.notifyDataSetChanged()
        updateEmptyView()
        
        // Clear selection when changing categories
        if (selectedTemplate != null && !filteredTemplates.contains(selectedTemplate)) {
            clearSelection()
        }
    }
    
    private fun selectTemplate(template: ResearchTemplate) {
        selectedTemplate = template
        updateSelectedTemplateView()
        
        // Update adapter to show selection
        templateAdapter.notifyDataSetChanged()
    }
    
    private fun updateSelectedTemplateView() {
        selectedTemplate?.let { template ->
            selectedTemplateContainer.visibility = View.VISIBLE
            
            selectedTemplateTitle.text = "${template.icon ?: "📊"} ${template.name}"
            selectedTemplateDescription.text = template.description
            
            // Format template details
            val details = buildString {
                append("🎯 Category: ${template.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}\n")
                append("🔧 Sensors: ${template.sensors.joinToString(", ") { it.name.replace("_", " ") }}\n")
                
                if (template.duration != null) {
                    val durationMs = template.duration!!
                    val minutes = durationMs / (60 * 1000)
                    append("⏱️ Duration: ${minutes} minutes\n")
                } else {
                    append("⏱️ Duration: Unlimited\n")
                }
                
                append("📊 GSR Rate: ${template.gsrSamplingRate}Hz\n")
                append("📹 Video: ${template.videoResolution.width}x${template.videoResolution.height} @ ${template.videoFrameRate}fps\n")
                
                template.instructions?.let { instructions ->
                    append("\n📋 Instructions:\n$instructions")
                }
            }
            
            selectedTemplateInstructions.text = details
            startRecordingButton.isEnabled = true
            
        } ?: run {
            selectedTemplateContainer.visibility = View.GONE
            startRecordingButton.isEnabled = false
        }
    }
    
    private fun clearSelection() {
        selectedTemplate = null
        updateSelectedTemplateView()
        templateAdapter.notifyDataSetChanged()
    }
    
    private fun updateEmptyView() {
        emptyView.visibility = if (filteredTemplates.isEmpty()) View.VISIBLE else View.GONE
        templatesRecyclerView.visibility = if (filteredTemplates.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun startRecordingWithTemplate(template: ResearchTemplate) {
        // Create session with template configuration
        val intent = Intent(this, MultiModalRecordingActivity::class.java).apply {
            putExtra("template_id", template.id)
            putExtra("template_name", template.name)
            putExtra("duration", template.duration ?: -1L)
            putExtra("gsr_sampling_rate", template.gsrSamplingRate)
            putExtra("video_width", template.videoResolution.width)
            putExtra("video_height", template.videoResolution.height)
            putExtra("video_frame_rate", template.videoFrameRate)
            putStringArrayListExtra("sensors", ArrayList(template.sensors.map { it.name }))
            putStringArrayListExtra("metadata_keys", ArrayList(template.metadata.keys))
            putStringArrayListExtra("metadata_values", ArrayList(template.metadata.values))
        }
        
        startActivity(intent)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

/**
 * RecyclerView Adapter for Research Templates
 */
class TemplateAdapter(
    private val context: Context,
    private val templates: List<ResearchTemplate>,
    private val onTemplateSelected: (ResearchTemplate) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {
    
    private var selectedTemplate: ResearchTemplate? = null
    
    class TemplateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: View = view
        val iconText: TextView = view.findViewById(R.id.template_icon)
        val nameText: TextView = view.findViewById(R.id.template_name)
        val categoryText: TextView = view.findViewById(R.id.template_category)
        val sensorsText: TextView = view.findViewById(R.id.template_sensors)
        val durationText: TextView = view.findViewById(R.id.template_duration)
        val selectionIndicator: View = view.findViewById(R.id.selection_indicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = templates[position]
        val isSelected = template == selectedTemplate
        
        // Template info
        holder.iconText.text = template.icon ?: "📊"
        holder.nameText.text = template.name
        holder.categoryText.text = template.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        
        // Sensors
        val sensorIcons = template.sensors.map { sensor ->
            when (sensor) {
                ResearchTemplate.SensorType.GSR -> "📊"
                ResearchTemplate.SensorType.THERMAL_CAMERA -> "🌡️"
                ResearchTemplate.SensorType.RGB_CAMERA -> "📸"
            }
        }.joinToString(" ")
        holder.sensorsText.text = sensorIcons
        
        // Duration
        holder.durationText.text = if (template.duration != null) {
            val durationMs = template.duration!!
            "${durationMs / (60 * 1000)}min"
        } else {
            "∞"
        }
        
        // Selection state
        holder.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.cardView.alpha = if (isSelected) 1.0f else 0.8f
        
        // Click handler
        holder.cardView.setOnClickListener {
            selectedTemplate = if (isSelected) null else template
            onTemplateSelected(template)
            notifyDataSetChanged()
        }
    }
    
    override fun getItemCount(): Int = templates.size
}