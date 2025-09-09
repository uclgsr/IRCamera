package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import kotlinx.coroutines.*
import java.io.File

/**
 * GSR Data View Activity
 * Detailed view of GSR CSV data files with statistics and export options
 */
class GSRDataViewActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"

        fun startActivity(
            context: Context,
            filePath: String,
        ) {
            val intent =
                Intent(context, GSRDataViewActivity::class.java).apply {
                    putExtra(EXTRA_FILE_PATH, filePath)
                }
            context.startActivity(intent)
        }
    }

    private lateinit var filePath: String
    private lateinit var file: File
    private lateinit var fileInfoText: TextView
    private lateinit var statisticsText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GSRDataRowAdapter
    private val dataRows = mutableListOf<GSRDataRow>()

    data class GSRDataRow(
        val timestamp: String,
        val gsrValue: Double,
        val resistance: Double,
        val conductance: Double,
        val rowNumber: Int,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gsr_data_view)

        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        file = File(filePath)

        if (!file.exists()) {
            finish()
            return
        }

        setupUI()
        loadGSRData()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = file.name

        fileInfoText = findViewById(R.id.file_info_text)
        statisticsText = findViewById(R.id.statistics_text)
        recyclerView = findViewById(R.id.data_recycler_view)

        adapter = GSRDataRowAdapter(dataRows)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Display basic file info
        val fileSize =
            if (file.length() >= 1024 * 1024) {
                "%.1f MB".format(file.length() / (1024.0 * 1024.0))
            } else {
                "%.1f KB".format(file.length() / 1024.0)
            }

        fileInfoText.text =
            """
            File: ${file.name}
            Size: $fileSize
            Path: ${file.absolutePath}
            Modified: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))}
            """.trimIndent()
    }

    private fun loadGSRData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lines = file.readLines()
                val headerLine = lines.firstOrNull() ?: ""
                val dataLines = lines.drop(1)

                val rows =
                    dataLines.mapIndexed { index, line ->
                        parseGSRDataRow(line, index + 2) // +2 because we skip header and 0-based index
                    }.filterNotNull()

                val statistics = calculateStatistics(rows)

                withContext(Dispatchers.Main) {
                    dataRows.clear()
                    dataRows.addAll(rows)
                    adapter.notifyDataSetChanged()

                    statisticsText.text =
                        """
                        Total Samples: ${rows.size}
                        Duration: ${formatDuration((rows.size / 128).toLong())} (@ 128 Hz)
                        
                        GSR Statistics:
                        • Min: %.3f μS
                        • Max: %.3f μS  
                        • Mean: %.3f μS
                        • Std Dev: %.3f μS
                        
                        Resistance Statistics:
                        • Min: %.1f kΩ
                        • Max: %.1f kΩ
                        • Mean: %.1f kΩ
                        """.trimIndent().format(
                            statistics.gsrMin, statistics.gsrMax, statistics.gsrMean, statistics.gsrStdDev,
                            statistics.resistanceMin / 1000, statistics.resistanceMax / 1000, statistics.resistanceMean / 1000,
                        )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statisticsText.text = "Error loading GSR data: ${e.message}"
                }
            }
        }
    }

    private fun parseGSRDataRow(
        line: String,
        rowNumber: Int,
    ): GSRDataRow? {
        return try {
            val parts = line.split(",")
            if (parts.size >= 4) {
                GSRDataRow(
                    timestamp = parts[0].trim(),
                    gsrValue = parts[1].trim().toDouble(),
                    resistance = parts[2].trim().toDouble(),
                    conductance = parts[3].trim().toDouble(),
                    rowNumber = rowNumber,
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateStatistics(rows: List<GSRDataRow>): GSRStatistics {
        if (rows.isEmpty()) {
            return GSRStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val gsrValues = rows.map { it.gsrValue }
        val resistanceValues = rows.map { it.resistance }

        val gsrMean = gsrValues.average()
        val resistanceMean = resistanceValues.average()

        val gsrVariance = gsrValues.map { (it - gsrMean) * (it - gsrMean) }.average()
        val gsrStdDev = kotlin.math.sqrt(gsrVariance)

        return GSRStatistics(
            gsrMin = gsrValues.minOrNull() ?: 0.0,
            gsrMax = gsrValues.maxOrNull() ?: 0.0,
            gsrMean = gsrMean,
            gsrStdDev = gsrStdDev,
            resistanceMin = resistanceValues.minOrNull() ?: 0.0,
            resistanceMax = resistanceValues.maxOrNull() ?: 0.0,
            resistanceMean = resistanceMean,
        )
    }

    data class GSRStatistics(
        val gsrMin: Double,
        val gsrMax: Double,
        val gsrMean: Double,
        val gsrStdDev: Double,
        val resistanceMin: Double,
        val resistanceMax: Double,
        val resistanceMean: Double,
    )

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gsr_data_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_export -> {
                exportData()
                true
            }
            R.id.action_share -> {
                shareData()
                true
            }
            R.id.action_plot -> {
                plotData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportData() {
        // Note: Data export functionality to be implemented in future release
    }

    private fun shareData() {
        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "GSR Data from ${file.name}")
                putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                type = "text/csv"
            }
        startActivity(Intent.createChooser(shareIntent, "Share GSR Data"))
    }

    private fun plotData() {
        // Note: Data plotting functionality to be implemented in future release
        // Could launch a chart activity showing GSR trends over time
    }
}
