package com.mpdc4gsr.libunified.app.comm

import android.os.Environment
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Excel utility for exporting thermal data to CSV format
 * Based on thermal data export requirements
 */
object ExcelUtil {
    
    private const val TAG = "ExcelUtil"
    
    /**
     * Export thermal data to CSV file with progress callback
     * @param fileName Base filename without extension
     * @param width Thermal image width
     * @param height Thermal image height
     * @param temperatureData Array of temperature values
     * @param progressCallback Callback for progress updates (current, total)
     * @return File path of exported CSV file or null if failed
     */
    suspend fun exportExcel(
        fileName: String,
        width: Int,
        height: Int,
        temperatureData: FloatArray,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFileName = "${fileName}_${timestamp}.csv"
            val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "IRCamera/Excel")
            
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val csvFile = File(exportDir, csvFileName)
            val writer = FileWriter(csvFile)
            
            // Write CSV header
            writer.write("Row,Column,Temperature(°C)\n")
            
            val total = temperatureData.size
            var current = 0
            
            // Write temperature data
            for (row in 0 until height) {
                for (col in 0 until width) {
                    val index = row * width + col
                    if (index < temperatureData.size) {
                        val temperature = temperatureData[index]
                        writer.write("$row,$col,$temperature\n")
                        current++
                        
                        // Report progress every 100 items
                        if (current % 100 == 0) {
                            progressCallback?.invoke(current, total)
                        }
                    }
                }
            }
            
            writer.close()
            progressCallback?.invoke(total, total)
            
            XLog.d(TAG, "Successfully exported thermal data to: ${csvFile.absolutePath}")
            csvFile.absolutePath
        } catch (e: IOException) {
            XLog.e(TAG, "Failed to export thermal data", e)
            null
        }
    }
    
    /**
     * Export thermal entity data to CSV file
     * @param thermalData List of thermal entities from database
     * @param isPointData Whether this is point temperature data or area data
     * @return File path of exported CSV file or null if failed
     */
    suspend fun exportExcel(
        thermalData: ArrayList<ThermalEntity>?,
        isPointData: Boolean
    ): String? = withContext(Dispatchers.IO) {
        if (thermalData.isNullOrEmpty()) {
            XLog.w(TAG, "No thermal data to export")
            return@withContext null
        }
        
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFileName = "thermal_log_${timestamp}.csv"
            val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "IRCamera/Excel")
            
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val csvFile = File(exportDir, csvFileName)
            val writer = FileWriter(csvFile)
            
            // Write CSV header based on data type
            if (isPointData) {
                writer.write("Timestamp,Point Temperature(°C),Notes\n")
            } else {
                writer.write("Timestamp,Max Temperature(°C),Min Temperature(°C),Avg Temperature(°C),Notes\n")
            }
            
            // Write data rows
            for (entity in thermalData) {
                val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(entity.timestamp))
                
                if (isPointData) {
                    writer.write("$timestampStr,${entity.temperature},${entity.notes ?: ""}\n")
                } else {
                    writer.write("$timestampStr,${entity.maxTemp},${entity.minTemp},${entity.avgTemp},${entity.notes ?: ""}\n")
                }
            }
            
            writer.close()
            
            XLog.d(TAG, "Successfully exported thermal log to: ${csvFile.absolutePath}")
            csvFile.absolutePath
        } catch (e: IOException) {
            XLog.e(TAG, "Failed to export thermal log", e)
            null
        }
    }
}