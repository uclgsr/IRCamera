package com.mpdc4gsr.module.thermalunified.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.comm.ExcelUtil
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.tools.FileTools
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.mpdc4gsr.libunified.R as LibR

/**
 * Compose implementation of IRLogMPChartActivity
 * Modern thermal data chart logging and export interface
 */
class IRLogMPChartComposeActivity : BaseComposeActivity<IRMonitorViewModel>() {

    private var startTime: Long = 0L

    override fun createViewModel(): IRMonitorViewModel {
        return viewModels<IRMonitorViewModel>().value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTime = intent.getLongExtra("startTime", 0L)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorViewModel) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        
        // Collect thermal data from ViewModel
        val thermalData by viewModel.thermalEntityList.collectAsStateWithLifecycle()
        var isLoading by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }
        
        // Load data when activity starts
        LaunchedEffect(startTime) {
            if (startTime > 0) {
                viewModel.queryDetail(startTime)
            }
        }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_record),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                        .verticalScroll(scrollState)
                ) {
                    // Chart display area
                    ChartDisplaySection(
                        thermalData = thermalData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(16.dp)
                    )
                    
                    // Chart information panel
                    if (thermalData.isNotEmpty()) {
                        ChartInfoPanel(
                            currentValue = getCurrentTemperature(thermalData),
                            maxValue = getMaxTemperature(thermalData),
                            minValue = getMinTemperature(thermalData),
                            averageValue = getAverageTemperature(thermalData),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Export path information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Export Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${stringResource(LibR.string.temp_export_path)}: ${FileConfig.excelDir}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Data Points: ${thermalData.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        
        // Export confirmation dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Data") },
                text = { Text("Export thermal data to Excel file?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            exportThermalData(thermalData)
                        }
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    @Composable
    private fun ChartDisplaySection(
        thermalData: List<ThermalEntity>,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (thermalData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "No data",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chart data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Use the ChartLogCompose component we created earlier
                val chartData = thermalData.map { entity ->
                    ThermalDataEntry(
                        timestamp = entity.createTime,
                        temperature = entity.thermal,
                        temperatureMax = entity.thermalMax,
                        temperatureMin = entity.thermalMin,
                        temperatureCenter = (entity.thermalMax + entity.thermalMin) / 2f,
                        type = entity.type
                    )
                }
                
                val chartType = when (thermalData.firstOrNull()?.type) {
                    "point" -> ThermalChartType.POINT
                    "line" -> ThermalChartType.LINE
                    else -> ThermalChartType.AREA
                }
                
                ChartLogCompose(
                    thermalData = chartData,
                    chartType = chartType,
                    timeFormat = TimeFormat.MINUTES,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun exportThermalData(thermalData: List<ThermalEntity>) {
        if (thermalData.isEmpty()) {
            ToastTools.showShort(LibR.string.liveData_save_error)
            return
        }

        val permissionList = if (applicationInfo.targetSdkVersion >= 34) {
            listOf(Permission.WRITE_EXTERNAL_STORAGE)
        } else if (applicationInfo.targetSdkVersion == 33) {
            listOf(Permission.WRITE_EXTERNAL_STORAGE)
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }

        XXPermissions.with(this)
            .permission(permissionList)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        lifecycleScope.launch {
                            try {
                                val filePath = withContext(Dispatchers.IO) {
                                    ExcelUtil.exportExcel(
                                        ArrayList(thermalData),
                                        "point" == thermalData[0].type
                                    )
                                }
                                
                                if (filePath.isNullOrEmpty()) {
                                    ToastTools.showShort(LibR.string.liveData_save_error)
                                } else {
                                    val uri = FileTools.getUri(File(filePath))
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/xlsx"
                                    }
                                    startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            getString(LibR.string.battery_share)
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                ToastTools.showShort(LibR.string.liveData_save_error)
                            }
                        }
                    } else {
                        ToastTools.showShort(LibR.string.scan_ble_tip_authorize)
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        if (BaseApplication.instance.isDomestic()) {
                            ToastTools.showShort(getString(LibR.string.app_storage_content))
                            return
                        }
                        
                        TipDialog.Builder(this@IRLogMPChartComposeActivity)
                            .setTitleMessage(getString(LibR.string.app_tip))
                            .setMessage(getString(LibR.string.app_storage_content))
                            .setPositiveListener(LibR.string.app_open) {
                                // Open app settings
                            }
                            .setCancelListener(LibR.string.app_cancel) { }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
    }
    
    private fun getCurrentTemperature(data: List<ThermalEntity>): String {
        return data.lastOrNull()?.thermal?.let { "${String.format("%.1f", it)}°C" } ?: "--"
    }
    
    private fun getMaxTemperature(data: List<ThermalEntity>): String {
        val max = data.maxOfOrNull { it.thermal } ?: return "--"
        return "${String.format("%.1f", max)}°C"
    }
    
    private fun getMinTemperature(data: List<ThermalEntity>): String {
        val min = data.minOfOrNull { it.thermal } ?: return "--"
        return "${String.format("%.1f", min)}°C"
    }
    
    private fun getAverageTemperature(data: List<ThermalEntity>): String {
        if (data.isEmpty()) return "--"
        val avg = data.map { it.thermal }.average()
        return "${String.format("%.1f", avg)}°C"
    }
}