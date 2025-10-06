package com.mpdc4gsr.libunified.app.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class ReportRepository : BaseRepository() {
    private val reportCache = ConcurrentHashMap<String, CachedReportData>()

    data class ReportData(
        val id: String,
        val title: String,
        val content: String,
        val timestamp: Long,
        val type: ReportType,
        val status: ReportStatus
    )

    enum class ReportType { GSR, THERMAL, COMBINED, ANALYSIS }
    enum class ReportStatus { DRAFT, PROCESSING, COMPLETED, ERROR }
    data class CachedReportData(
        val data: List<ReportData>,
        val cachedAt: Long,
        val page: Int
    )

    fun getReports(
        isTC007: Boolean,
        page: Int,
        pageSize: Int = 20
    ): Flow<BaseRepository.Result<List<ReportData>>> = safeFlow {
        val cacheKey = "reports_${if (isTC007) "tc007" else "ts004"}_$page"
        val cached = reportCache[cacheKey]
        // Return cached data if valid
        if (cached != null && System.currentTimeMillis() - cached.cachedAt < 60000) {
            return@safeFlow cached.data
        }
        // Simulate network call
        delay(1000)
        val reports = generateSampleReports(isTC007, page, pageSize)
        // Cache the results
        reportCache[cacheKey] = CachedReportData(
            data = reports,
            cachedAt = System.currentTimeMillis(),
            page = page
        )
        reports
    }

    private fun generateSampleReports(
        isTC007: Boolean,
        page: Int,
        pageSize: Int
    ): List<ReportData> {
        val deviceType = if (isTC007) "TC007" else "TS004"
        return (1..pageSize).map { index ->
            val id = "${page * pageSize + index}"
            ReportData(
                id = id,
                title = "$deviceType Report #$id",
                content = "Sample report content for $deviceType device",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                type = if (isTC007) ReportType.THERMAL else ReportType.GSR,
                status = ReportStatus.COMPLETED
            )
        }
    }

    fun clearCache() {
        reportCache.clear()
    }
}