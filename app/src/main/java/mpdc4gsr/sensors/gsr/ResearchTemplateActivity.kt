package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityResearchTemplateBinding
import com.mpdc4gsr.gsr.model.ResearchTemplate
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity

class ResearchTemplateActivity : BaseBindingActivity<ActivityResearchTemplateBinding>() {
    private lateinit var templateAdapter: TemplateAdapter

    private var selectedTemplate: ResearchTemplate? = null
    private val allTemplates = ResearchTemplate.PREDEFINED_TEMPLATES
    private val filteredTemplates = mutableListOf<ResearchTemplate>()

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateActivity::class.java))
        }
    }

    override fun initContentLayoutId() = R.layout.activity_research_template

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViews()
        setupCategoryFilter()
        setupTemplateGrid()
        loadTemplates()
    }

    private fun initializeViews() {
        supportActionBar?.title = "Research Templates"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.startRecordingButton.setOnClickListener {
            selectedTemplate?.let { template ->
                startRecordingWithTemplate(template)
            }
        }
    }

    private fun setupCategoryFilter() {
        val categories =
            listOf("All Templates") +
                    ResearchTemplate.TemplateCategory.values().map {
                        it.name.replace("_", " ").lowercase().replaceFirstChar { char ->
                            char.uppercase()
                        }
                    }

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = spinnerAdapter

        binding.categorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    filterTemplatesByCategory(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun setupTemplateGrid() {
        templateAdapter =
            TemplateAdapter(
                context = this,
                templates = filteredTemplates,
                onTemplateSelected = { template -> selectTemplate(template) },
            )

        binding.templatesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.templatesRecyclerView.adapter = templateAdapter
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
            
            filteredTemplates.addAll(allTemplates)
        } else {
            
            val category = ResearchTemplate.TemplateCategory.values()[categoryIndex - 1]
            filteredTemplates.addAll(ResearchTemplate.getTemplatesByCategory(category))
        }

        templateAdapter.notifyDataSetChanged()
        updateEmptyView()

        
        if (selectedTemplate != null && !filteredTemplates.contains(selectedTemplate)) {
            clearSelection()
        }
    }

    private fun selectTemplate(template: ResearchTemplate) {
        selectedTemplate = template
        updateSelectedTemplateView()

        
        templateAdapter.notifyDataSetChanged()
    }

    private fun updateSelectedTemplateView() {
        selectedTemplate?.let { template ->
            binding.selectedTemplateContainer.visibility = View.VISIBLE

            binding.selectedTemplateTitle.text = "${template.icon ?: "📊"} ${template.name}"
            binding.selectedTemplateDescription.text = template.description

            
            val details =
                buildString {
                    append(
                        "🎯 Category: ${
                            template.category.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        }\n"
                    )
                    append(
                        "🔧 Sensors: ${
                            template.sensors.joinToString(", ") {
                                it.name.replace(
                                    "_",
                                    " "
                                )
                            }
                        }\n"
                    )

                    if (template.duration != null) {
                        val durationMs = template.duration!!
                        val minutes = durationMs / (60 * 1000)
                        append("⏱️ Duration: $minutes minutes\n")
                    } else {
                        append("⏱️ Duration: Unlimited\n")
                    }

                    append("📊 GSR Rate: ${template.gsrSamplingRate}Hz\n")
                    append("📹 Video: ${template.videoResolution.width}x${template.videoResolution.height} @ ${template.videoFrameRate}fps\n")

                    template.instructions?.let { instructions ->
                        append("\n📋 Instructions:\n$instructions")
                    }
                }

            binding.selectedTemplateInstructions.text = details
            binding.startRecordingButton.isEnabled = true
        } ?: run {
            binding.selectedTemplateContainer.visibility = View.GONE
            binding.startRecordingButton.isEnabled = false
        }
    }

    private fun clearSelection() {
        selectedTemplate = null
        updateSelectedTemplateView()
        templateAdapter.notifyDataSetChanged()
    }

    private fun updateEmptyView() {
        binding.emptyView.visibility = if (filteredTemplates.isEmpty()) View.VISIBLE else View.GONE
        binding.templatesRecyclerView.visibility =
            if (filteredTemplates.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun startRecordingWithTemplate(template: ResearchTemplate) {
        
        val intent =
            Intent(this, MultiModalRecordingActivity::class.java).apply {
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

class TemplateAdapter(
    private val context: Context,
    private val templates: List<ResearchTemplate>,
    private val onTemplateSelected: (ResearchTemplate) -> Unit,
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TemplateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TemplateViewHolder,
        position: Int,
    ) {
        val template = templates[position]
        val isSelected = template == selectedTemplate

        
        holder.iconText.text = template.icon ?: "📊"
        holder.nameText.text = template.name
        holder.categoryText.text =
            template.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        
        val sensorIcons =
            template.sensors.map { sensor ->
                when (sensor) {
                    ResearchTemplate.SensorType.GSR -> "📊"
                    ResearchTemplate.SensorType.THERMAL_CAMERA -> "🌡️"
                    ResearchTemplate.SensorType.RGB_CAMERA -> "📸"
                }
            }.joinToString(" ")
        holder.sensorsText.text = sensorIcons

        
        holder.durationText.text =
            if (template.duration != null) {
                val durationMs = template.duration!!
                "${durationMs / (60 * 1000)}min"
            } else {
                "∞"
            }

        
        holder.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.cardView.alpha = if (isSelected) 1.0f else 0.8f

        
        holder.cardView.setOnClickListener {
            selectedTemplate = if (isSelected) null else template
            onTemplateSelected(template)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = templates.size
}
